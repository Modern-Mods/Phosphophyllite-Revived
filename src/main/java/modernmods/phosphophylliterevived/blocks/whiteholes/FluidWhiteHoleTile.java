package modernmods.phosphophylliterevived.blocks.whiteholes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import modernmods.phosphophylliterevived.transfer.TransferUtil;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import modernmods.phosphophylliterevived.modular.tile.PhosphophylliteTile;
import modernmods.phosphophylliterevived.registry.RegisterTile;

import javax.annotation.Nonnull;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;


@ParametersAreNonnullByDefault
public class FluidWhiteHoleTile extends PhosphophylliteTile implements IFluidHandler {
    
    @RegisterTile("fluid_white_hole")
    public static final BlockEntityType.BlockEntitySupplier<FluidWhiteHoleTile> SUPPLIER = new RegisterTile.Producer<>(FluidWhiteHoleTile::new);
    
    public FluidWhiteHoleTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Nullable
    @Override
    public <T> T capability(BlockCapability<T, Direction> cap, final @Nullable Direction side) {
        if (cap == Capabilities.Fluid.BLOCK) {
            //noinspection unchecked
            return (T) modernmods.phosphophylliterevived.transfer.FluidResourceHandler.of(this);
        }
        return super.capability(cap, side);
    }
    
    FluidStack fluidStack = FluidStack.EMPTY;
    
    @Override
    public int getTanks() {
        return 1;
    }
    
    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return fluidStack;
    }
    
    @Override
    public int getTankCapacity(int tank) {
        return Integer.MAX_VALUE;
    }
    
    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return true;
    }
    
    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }
    
    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.getFluid() == fluidStack.getFluid()) {
            return resource.copy();
        }
        return FluidStack.EMPTY;
    }
    
    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return new FluidStack(fluidStack.getFluid(), maxDrain);
    }
    
    public void setFluid(Fluid fluid) {
        fluidStack = new FluidStack(fluid, Integer.MAX_VALUE);
    }
    
    @Override
    public CompoundTag writeNBT() {
        var compound = super.writeNBT();
        compound.put("fluidstack", FluidStack.OPTIONAL_CODEC.encodeStart(Objects.requireNonNull(level).registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), fluidStack).getOrThrow());
        return compound;
    }
    
    @Override
    public void readNBT(CompoundTag compound) {
        fluidStack = compound.getCompound("fluidstack").flatMap(tag -> FluidStack.OPTIONAL_CODEC.parse(Objects.requireNonNull(level).registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag).result()).orElse(FluidStack.EMPTY);
        super.readNBT(compound);
    }
    
    public void tick() {
        assert level != null;
        for (Direction direction : Direction.values()) {
            BlockEntity te = level.getBlockEntity(worldPosition.relative(direction));
            if (te != null) {
                final var handler = level.getCapability(Capabilities.Fluid.BLOCK, te.getBlockPos(), direction.getOpposite());
                if (handler != null) {
                    try (final var transaction = TransferUtil.openTransaction()) {
                        handler.insert(FluidResource.of(fluidStack), Integer.MAX_VALUE, transaction);
                        transaction.commit();
                    }
                }
            }
        }
    }
}
