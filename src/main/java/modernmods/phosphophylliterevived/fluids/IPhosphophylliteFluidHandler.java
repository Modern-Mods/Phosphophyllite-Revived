package modernmods.phosphophylliterevived.fluids;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IPhosphophylliteFluidHandler extends IFluidHandler {
    
    @Override
    default int getTanks() {
        return tankCount();
    }
    
    @Nonnull
    @Override
    default FluidStack getFluidInTank(int tank) {
        final var fluid = fluidTypeInTank(tank);
        if (fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }
        final var amount = fluidAmountInTank(tank);
        if (amount <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluid), (int) Math.min(amount, Integer.MAX_VALUE), fluidComponentsInTank(tank));
    }
    
    @Override
    default int getTankCapacity(int tank) {
        return (int) Math.min(Integer.MAX_VALUE, tankCapacity(tank));
    }
    
    @Override
    default boolean isFluidValid(int tank, FluidStack resource) {
        return fluidValidForTank(tank, resource.getFluid());
    }
    
    @Override
    default int fill(FluidStack resource, FluidAction action) {
        return (int) fill(resource.getFluid(), resource.getComponentsPatch(), resource.getAmount(), action.simulate());
    }
    
    @Nonnull
    @Override
    default FluidStack drain(FluidStack resource, FluidAction action) {
        int drainedAmount = (int) drain(resource.getFluid(), resource.getComponentsPatch(), resource.getAmount(), action.simulate());
        if (drainedAmount == 0) {
            return FluidStack.EMPTY;
        }
        return resource.copyWithAmount(drainedAmount);
    }
    
    @Nonnull
    @Override
    default FluidStack drain(int maxDrain, FluidAction action) {
        for (int i = 0; i < tankCount(); i++) {
            final var fluid = fluidTypeInTank(i);
            if (fluid == Fluids.EMPTY) {
                continue;
            }
            final var components = fluidComponentsInTank(i);
            final int drainedAmount = (int) drain(fluid, components, maxDrain, action.simulate());
            if (drainedAmount != 0) {
                return new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluid), drainedAmount, components);
            }
        }
        return FluidStack.EMPTY;
    }
    
    int tankCount();
    
    long tankCapacity(int tank);
    
    Fluid fluidTypeInTank(int tank);
    
    default DataComponentPatch fluidComponentsInTank(int tank) {
        return DataComponentPatch.EMPTY;
    }
    
    long fluidAmountInTank(int tank);
    
    boolean fluidValidForTank(int tank, Fluid fluid);
    
    long fill(Fluid fluid, DataComponentPatch components, long amount, boolean simulate);
    
    long drain(Fluid fluid, DataComponentPatch components, long amount, boolean simulate);
}
