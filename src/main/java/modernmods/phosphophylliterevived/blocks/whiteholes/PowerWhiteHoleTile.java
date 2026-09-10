package modernmods.phosphophylliterevived.blocks.whiteholes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import modernmods.phosphophylliterevived.Phosphophyllite;
import modernmods.phosphophylliterevived.debug.DebugInfo;
import modernmods.phosphophylliterevived.energy.IEnergyTile;
import modernmods.phosphophylliterevived.energy.IPhosphophylliteEnergyHandler;
import modernmods.phosphophylliterevived.modular.tile.PhosphophylliteTile;
import modernmods.phosphophylliterevived.registry.RegisterTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PowerWhiteHoleTile extends PhosphophylliteTile implements IEnergyTile {

    @RegisterTile("power_white_hole")
    public static final BlockEntityType.BlockEntitySupplier<PowerWhiteHoleTile> SUPPLIER = new RegisterTile.Producer<>(PowerWhiteHoleTile::new);

    public PowerWhiteHoleTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        rotateCapability(null);
    }

    private static final Direction[] directions = Direction.values();
    private final IPhosphophylliteEnergyHandler[] handlers = new IPhosphophylliteEnergyHandler[6];

    @Nullable
    private IPhosphophylliteEnergyHandler energyHandler;

    private long sentLastTick = 0;
    private boolean doPush = true;
    private boolean allowPull;

    @Nullable
    @Override
    public IPhosphophylliteEnergyHandler energyHandler() {
        return energyHandler;
    }

    public void tick() {
        assert level != null;
        sentLastTick = 0;
        if (doPush) {
            for (final var handler : handlers) {
                if (handler != null) {
                    sendEnergy(handler);
                }
            }
        }
    }

    private void sendEnergy(IPhosphophylliteEnergyHandler handler) {
        final var sent = handler.insertEnergy(Long.MAX_VALUE, false);
        if (sentLastTick + sent < sentLastTick) {
            sentLastTick = Long.MAX_VALUE;
            return;
        }
        sentLastTick += sent;
    }

    public void rotateCapability(@Nullable Player player) {
        energyHandler = new IPhosphophylliteEnergyHandler() {

            private void ensureValid() {
                if (!Phosphophyllite.CONFIG.debugMode) {
                    return;
                }
                if (energyHandler != this) {
                    throw new IllegalStateException("Attempt to use capability after invalidate");
                }
            }

            @Override
            public long insertEnergy(long maxInsert, boolean simulate) {
                ensureValid();
                if (maxInsert < 0) {
                    if (Phosphophyllite.CONFIG.debugMode) {
                        throw new IllegalStateException("Something tried to insert negative power");
                    }
                    return 0;
                }
                return 0;
            }

            @Override
            public long extractEnergy(long maxExtract, boolean simulate) {
                ensureValid();
                if (maxExtract < 0) {
                    if (Phosphophyllite.CONFIG.debugMode) {
                        throw new IllegalStateException("Something tried to extract negative power");
                    }
                    return 0;
                }
                if (!allowPull) {
                    return 0;
                }
                if (!simulate) {
                    if (sentLastTick + maxExtract < sentLastTick) {
                        sentLastTick = Long.MAX_VALUE;
                        return maxExtract;
                    }
                    sentLastTick += maxExtract;
                }
                return maxExtract;
            }

            @Override
            public long energyStored() {
                ensureValid();
                return Long.MAX_VALUE;
            }

            @Override
            public long maxEnergyStored() {
                ensureValid();
                return Long.MAX_VALUE;
            }
        };
        if (level != null) {
            level.invalidateCapabilities(getBlockPos());
        }
        if (player != null) {
            player.sendSystemMessage(Component.literal("Capability invalidated"));
        }
    }

    public void nextOption(Player player) {
        if (doPush) {
            if (allowPull) {
                allowPull = false;
            } else {
                allowPull = true;
                doPush = false;
            }
        } else {
            doPush = true;
            allowPull = true;
        }
        if (player.isLocalPlayer()) {
            player.sendSystemMessage(Component.literal("doPush: " + doPush + ", allowPull: " + allowPull));
        }
    }

    @Override
    public void onAdded() {
        for (Direction direction : directions) {
            updateCapability(direction, null, getBlockPos());
        }
    }

    @Override
    public void onRemoved(boolean chunkUnload) {
        energyHandler = null;
        if (level != null) {
            level.invalidateCapabilities(getBlockPos());
        }
    }

    @Override
    protected void readNBT(CompoundTag compound) {
        doPush = compound.getBooleanOr("doPush", false);
        allowPull = compound.getBooleanOr("allowPull", false);
    }

    @Override
    protected CompoundTag writeNBT() {
        final var tag = new CompoundTag();
        tag.putBoolean("doPush", doPush);
        tag.putBoolean("allowPull", allowPull);
        return tag;
    }

    @Nonnull
    @Override
    public DebugInfo getDebugInfo() {
        return super.getDebugInfo()
                .add("SentLastTick: " + sentLastTick)
                .add("DoPush " + doPush)
                .add("AllowPull " + allowPull)
                ;
    }

    public void updateCapability(Direction updateDirection, @Nullable Block oldBlock, BlockPos updatePos) {
        final var index = updateDirection.ordinal();
        if (Phosphophyllite.CONFIG.debugMode && handlers[index] != null && oldBlock != null) {
            assert level != null;
            var currentBlockState = level.getBlockState(updatePos);
            if (currentBlockState.getBlock() != oldBlock) {
                Phosphophyllite.LOGGER.warn("Block updated from " + BuiltInRegistries.BLOCK.getKey(oldBlock) + " without invalidating capability");
                handlers[index] = null;
            }
        }
        handlers[index] = findEnergyCapability(updateDirection);
    }
}
