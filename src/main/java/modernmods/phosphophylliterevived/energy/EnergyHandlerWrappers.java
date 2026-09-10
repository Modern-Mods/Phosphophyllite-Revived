package modernmods.phosphophylliterevived.energy;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mekanism.api.Action;
import mekanism.api.energy.IEnergyConversion;
import mekanism.api.energy.IEnergyConversionHelper;
import mekanism.api.energy.IStrictEnergyHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import modernmods.phosphophylliterevived.transfer.TransferUtil;
import modernmods.phosphophylliterevived.capabilities.MekanismCapabilities;
import modernmods.phosphophylliterevived.capabilities.PhosphophylliteCapabilities;
import modernmods.phosphophylliterevived.registry.OnModLoad;
import modernmods.phosphophylliterevived.util.NonnullDefault;
import modernmods.phosphophylliterevived.util.Util;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.function.Function;

@NonnullDefault
public class EnergyHandlerWrappers {

    private record WrapperRegistration<T>(BlockCapability<T, Direction> capability,
                                          Function<T, IPhosphophylliteEnergyHandler> toWrapperConstructor,
                                          Function<IPhosphophylliteEnergyHandler, T> fromWrapperConstructor,
                                          int priority) {
        IPhosphophylliteEnergyHandler wrapTo(Object handler) {
            //noinspection unchecked
            return toWrapperConstructor.apply((T) handler);
        }

        Object wrapFrom(IPhosphophylliteEnergyHandler handler) {
            return fromWrapperConstructor.apply(handler);
        }
    }

    private static final ObjectArrayList<WrapperRegistration<?>> supportedCapabilitiesList = new ObjectArrayList<>();
    private static final Object2ObjectMap<BlockCapability<?, Direction>, WrapperRegistration<?>> supportedCapabilitiesMap = new Object2ObjectOpenHashMap<>();

    public static synchronized <T> void registerWrapper(BlockCapability<T, Direction> capability, Function<T, IPhosphophylliteEnergyHandler> toWrapperConstructor, Function<IPhosphophylliteEnergyHandler, T> fromWrapperConstructor, int priority) {
        registerWrapper(new WrapperRegistration<>(capability, toWrapperConstructor, fromWrapperConstructor, priority));
    }

    private static synchronized <T> void registerWrapper(WrapperRegistration<T> registration) {
        supportedCapabilitiesList.add(registration);
        final Comparator<WrapperRegistration<?>> comparator = Comparator.comparingInt(WrapperRegistration::priority);
        supportedCapabilitiesList.sort(comparator.reversed());
        supportedCapabilitiesMap.put(registration.capability, registration);
        PhosphophylliteCapabilities.registerBlockCapability(registration.capability);
    }

    static {
        registerWrapper(IPhosphophylliteEnergyHandler.CAPABILITY, Function.identity(), Function.identity(), Integer.MAX_VALUE);
    }

    @Nullable
    public static IPhosphophylliteEnergyHandler findCapability(BlockEntity entity, Direction direction) {
        final var level = entity.getLevel();
        if (level == null) {
            return null;
        }
        final var pos = entity.getBlockPos();
        for (int i = 0; i < supportedCapabilitiesList.size(); i++) {
            final var registration = supportedCapabilitiesList.get(i);
            final var handler = level.getCapability(registration.capability, pos, entity.getBlockState(), entity, direction);
            if (handler == null) {
                continue;
            }
            return registration.wrapTo(handler);
        }
        return null;
    }

    @Nullable
    public static Object attemptWrap(BlockCapability<?, Direction> capability, @Nullable IPhosphophylliteEnergyHandler handler) {
        if (handler == null) {
            return null;
        }
        final var registration = supportedCapabilitiesMap.get(capability);
        if (registration == null) {
            return null;
        }
        return registration.wrapFrom(handler);
    }

    private static class Neo {

        @OnModLoad
        public static void onModLoad() {
            registerWrapper(Capabilities.Energy.BLOCK, ToWrapper::new, FromWrapper::new, Integer.MIN_VALUE);
        }

        private record ToWrapper(EnergyHandler neoStorage) implements IPhosphophylliteEnergyHandler {

            @Override
            public long insertEnergy(long maxInsert, boolean simulate) {
                try (final var transaction = TransferUtil.openTransaction()) {
                    final int inserted = neoStorage.insert(Util.clampToInt(maxInsert), transaction);
                    if (!simulate) {
                        transaction.commit();
                    }
                    return inserted;
                }
            }

            @Override
            public long extractEnergy(long maxExtract, boolean simulate) {
                try (final var transaction = TransferUtil.openTransaction()) {
                    final int extracted = neoStorage.extract(Util.clampToInt(maxExtract), transaction);
                    if (!simulate) {
                        transaction.commit();
                    }
                    return extracted;
                }
            }

            @Override
            public long energyStored() {
                return neoStorage.getAmountAsLong();
            }

            @Override
            public long maxEnergyStored() {
                return neoStorage.getCapacityAsLong();
            }
        }

        private static final class FromWrapper extends SnapshotJournal<Long> implements EnergyHandler {

            private final IPhosphophylliteEnergyHandler phosHandler;
            private long delta = 0;

            private FromWrapper(IPhosphophylliteEnergyHandler phosHandler) {
                this.phosHandler = phosHandler;
            }

            @Override
            protected Long createSnapshot() {
                return delta;
            }

            @Override
            protected void revertToSnapshot(Long snapshot) {
                final long difference = delta - snapshot;
                if (difference > 0) {
                    phosHandler.extractEnergy(difference, false);
                } else if (difference < 0) {
                    phosHandler.insertEnergy(-difference, false);
                }
                delta = snapshot;
            }

            @Override
            public long getAmountAsLong() {
                return phosHandler.energyStored();
            }

            @Override
            public long getCapacityAsLong() {
                return phosHandler.maxEnergyStored();
            }

            @Override
            public int insert(int amount, TransactionContext transaction) {
                updateSnapshots(transaction);
                final long inserted = phosHandler.insertEnergy(amount, false);
                delta += inserted;
                return Util.clampToInt(inserted);
            }

            @Override
            public int extract(int amount, TransactionContext transaction) {
                updateSnapshots(transaction);
                final long extracted = phosHandler.extractEnergy(amount, false);
                delta -= extracted;
                return Util.clampToInt(extracted);
            }
        }
    }

    private static class Mekanism {

        private static final IEnergyConversion JouleFEConversion = IEnergyConversionHelper.INSTANCE.feConversion();

        @OnModLoad(required = false)
        public static void onModLoad() {
            registerWrapper(MekanismCapabilities.STRICT_ENERGY, ToWrapper::new, FromWrapper::new, -1);
        }

        private record ToWrapper(IStrictEnergyHandler mekHandler) implements IPhosphophylliteEnergyHandler {

            @Override
            public long insertEnergy(long maxInsert, boolean simulate) {
                long inserted = 0;
                final int containers = mekHandler.getEnergyContainerCount();
                for (int i = 0; i < containers && inserted < maxInsert; i++) {
                    final var toInsertJoules = JouleFEConversion.convertFrom(maxInsert - inserted);
                    final var remainingJoules = mekHandler.insertEnergy(i, toInsertJoules, simulate ? Action.SIMULATE : Action.EXECUTE);
                    inserted += JouleFEConversion.convertTo(toInsertJoules - remainingJoules);
                }
                return inserted;
            }

            @Override
            public long extractEnergy(long maxExtract, boolean simulate) {
                long extracted = 0;
                final int containers = mekHandler.getEnergyContainerCount();
                for (int i = 0; i < containers && extracted < maxExtract; i++) {
                    final var toExtractJoules = JouleFEConversion.convertFrom(maxExtract - extracted);
                    final var extractedJoules = mekHandler.extractEnergy(i, toExtractJoules, simulate ? Action.SIMULATE : Action.EXECUTE);
                    extracted += JouleFEConversion.convertTo(extractedJoules);
                }
                return extracted;
            }

            @Override
            public long energyStored() {
                long stored = 0;
                final int containers = mekHandler.getEnergyContainerCount();
                for (int i = 0; i < containers; i++) {
                    final var containerStored = JouleFEConversion.convertTo(mekHandler.getEnergy(i));
                    if (stored + containerStored < stored) {
                        return Long.MAX_VALUE;
                    }
                    stored += containerStored;
                }
                return stored;
            }

            @Override
            public long maxEnergyStored() {
                long maxStored = 0;
                final int containers = mekHandler.getEnergyContainerCount();
                for (int i = 0; i < containers; i++) {
                    final var containerMaxStored = JouleFEConversion.convertTo(mekHandler.getMaxEnergy(i));
                    if (maxStored + containerMaxStored < maxStored) {
                        return Long.MAX_VALUE;
                    }
                    maxStored += containerMaxStored;
                }
                return maxStored;
            }
        }

        private record FromWrapper(IPhosphophylliteEnergyHandler phosHandler) implements IStrictEnergyHandler {

            @Override
            public int getEnergyContainerCount() {
                return 1;
            }

            @Override
            public long getEnergy(int container) {
                if (container != 0) {
                    return 0;
                }
                return JouleFEConversion.convertFrom(phosHandler.energyStored());
            }

            @Override
            public void setEnergy(int container, long energy) {
                throw new RuntimeException("setEnergy not implemented");
            }

            @Override
            public long getMaxEnergy(int container) {
                if (container != 0) {
                    return 0;
                }
                return JouleFEConversion.convertFrom(phosHandler.maxEnergyStored());
            }

            @Override
            public long getNeededEnergy(int container) {
                if (container != 0) {
                    return 0;
                }
                return JouleFEConversion.convertFrom(phosHandler.maxEnergyStored() - phosHandler.energyStored());
            }

            @Override
            public long insertEnergy(int container, long amount, Action action) {
                if (container != 0) {
                    return amount;
                }
                final var toInsert = JouleFEConversion.convertTo(amount);
                final var inserted = phosHandler.insertEnergy(toInsert, action.simulate());
                return JouleFEConversion.convertFrom(toInsert - inserted);
            }

            @Override
            public long extractEnergy(int container, long amount, Action action) {
                if (container != 0) {
                    return 0;
                }
                final var toExtract = JouleFEConversion.convertTo(amount);
                final var extracted = phosHandler.extractEnergy(toExtract, action.simulate());
                return JouleFEConversion.convertFrom(extracted);
            }
        }
    }
}
