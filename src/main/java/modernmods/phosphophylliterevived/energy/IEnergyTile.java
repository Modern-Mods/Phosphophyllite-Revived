package modernmods.phosphophylliterevived.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import modernmods.phosphophylliterevived.modular.api.IModularTile;
import modernmods.phosphophylliterevived.modular.api.ModuleRegistry;
import modernmods.phosphophylliterevived.modular.api.TileModule;
import modernmods.phosphophylliterevived.registry.OnModLoad;
import modernmods.phosphophylliterevived.util.NonnullDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@NonnullDefault
public interface IEnergyTile extends IModularTile {

    @Nullable
    IPhosphophylliteEnergyHandler energyHandler();

    @Nullable
    default IPhosphophylliteEnergyHandler findEnergyCapability(Direction direction) {
        return Objects.requireNonNull(module(IEnergyTile.class, Module.class)).findEnergyCapability(direction);
    }

    final class Module extends TileModule<IEnergyTile> {

        @OnModLoad
        private static void onModLoad() {
            ModuleRegistry.registerTileModule(IEnergyTile.class, Module::new);
        }

        public Module(IEnergyTile iface) {
            super(iface);
        }

        @Nullable
        @Override
        public <T> T capability(BlockCapability<T, Direction> cap, @Nullable Direction side) {
            final var handler = iface.energyHandler();
            if (handler == null) {
                return null;
            }
            //noinspection unchecked
            return (T) EnergyHandlerWrappers.attemptWrap(cap, handler);
        }

        @Nullable
        IPhosphophylliteEnergyHandler findEnergyCapability(Direction direction) {
            final var thisTile = iface.as(BlockEntity.class);
            final var level = thisTile.getLevel();
            if (level == null) {
                return null;
            }
            final var tile = level.getBlockEntity(thisTile.getBlockPos().offset(direction.getUnitVec3i()));
            if (tile == null) {
                return null;
            }
            return EnergyHandlerWrappers.findCapability(tile, direction.getOpposite());
        }
    }
}
