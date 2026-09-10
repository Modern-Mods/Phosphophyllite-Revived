package modernmods.phosphophylliterevived.blocks.blackholes;

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
import modernmods.phosphophylliterevived.util.NonnullDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@NonnullDefault
public class PowerBlackHoleTile extends PhosphophylliteTile implements IEnergyTile {

    @RegisterTile("power_black_hole")
    public static final BlockEntityType.BlockEntitySupplier<PowerBlackHoleTile> SUPPLIER = new RegisterTile.Producer<>(PowerBlackHoleTile::new);

    private static final Direction[] directions = Direction.values();
    private final IPhosphophylliteEnergyHandler[] handlers = new IPhosphophylliteEnergyHandler[6];

    @Nullable
    private IPhosphophylliteEnergyHandler energyHandler;

    private long receivedLastTick = 0;
    private boolean doPull = false;
    private boolean allowPush = true;

    public PowerBlackHoleTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        rotateCapability(null);
    }

    @Nullable
    @Override
    public IPhosphophylliteEnergyHandler energyHandler() {
        return energyHandler;
    }

    public void tick() {
        assert level != null;
        receivedLastTick = 0;
        if (doPull) {
            for (final var handler : handlers) {
                if (handler != null) {
                    sendEnergy(handler);
                }
            }
        }
    }

    private void sendEnergy(IPhosphophylliteEnergyHandler handler) {
        final var received = handler.extractEnergy(Long.MAX_VALUE, false);
        if (receivedLastTick + received < receivedLastTick) {
            receivedLastTick = Long.MAX_VALUE;
            return;
        }
        receivedLastTick += received;
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
                if (!allowPush) {
                    return 0;
                }
                if (!simulate) {
                    if (receivedLastTick + maxInsert < receivedLastTick) {
                        receivedLastTick = Long.MAX_VALUE;
                        return maxInsert;
                    }
                    receivedLastTick += maxInsert;
                }
                return maxInsert;
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
                return 0;
            }

            @Override
            public long energyStored() {
                ensureValid();
                return 0;
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
        if (doPull) {
            if (allowPush) {
                allowPush = false;
            } else {
                allowPush = true;
                doPull = false;
            }
        } else {
            doPull = true;
            allowPush = true;
        }
        if (player.isLocalPlayer()) {
            player.sendSystemMessage(Component.literal("doPull: " + doPull + ", allowPush: " + allowPush));
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
        doPull = compound.getBooleanOr("doPull", false);
        allowPush = compound.getBooleanOr("allowPush", false);
    }

    @Override
    protected CompoundTag writeNBT() {
        final var tag = new CompoundTag();
        tag.putBoolean("doPull", doPull);
        tag.putBoolean("allowPush", allowPush);
        return tag;
    }

    @Nonnull
    @Override
    public DebugInfo getDebugInfo() {
        return super.getDebugInfo()
                .add("ReceivedLastTick: " + receivedLastTick)
                .add("DoPull " + doPull)
                .add("AllowPush " + allowPush)
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
