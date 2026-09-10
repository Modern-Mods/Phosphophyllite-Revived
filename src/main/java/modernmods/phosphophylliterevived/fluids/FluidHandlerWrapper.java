package modernmods.phosphophylliterevived.fluids;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class FluidHandlerWrapper implements IPhosphophylliteFluidHandler {
    
    public static IPhosphophylliteFluidHandler wrap(IFluidHandler handler) {
        if (handler instanceof IPhosphophylliteFluidHandler) {
            return (IPhosphophylliteFluidHandler) handler;
        }
        return new FluidHandlerWrapper(handler);
    }
    
    final IFluidHandler handler;
    
    private FluidHandlerWrapper(IFluidHandler handler) {
        this.handler = handler;
    }
    
    private static FluidStack stack(Fluid fluid, DataComponentPatch components, long amount) {
        return new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluid), (int) Math.min(amount, Integer.MAX_VALUE), components);
    }
    
    @Override
    public int tankCount() {
        return handler.getTanks();
    }
    
    @Override
    public long tankCapacity(int tank) {
        return handler.getTankCapacity(tank);
    }
    
    @Override
    public Fluid fluidTypeInTank(int tank) {
        return handler.getFluidInTank(tank).getFluid();
    }
    
    @Override
    public DataComponentPatch fluidComponentsInTank(int tank) {
        return handler.getFluidInTank(tank).getComponentsPatch();
    }
    
    @Override
    public long fluidAmountInTank(int tank) {
        return handler.getFluidInTank(tank).getAmount();
    }
    
    @Override
    public boolean fluidValidForTank(int tank, Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return false;
        }
        return handler.isFluidValid(tank, stack(fluid, DataComponentPatch.EMPTY, 1));
    }
    
    @Override
    public long fill(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return 0;
        }
        return handler.fill(stack(fluid, components, amount), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
    }
    
    @Override
    public long drain(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return 0;
        }
        return handler.drain(stack(fluid, components, amount), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE).getAmount();
    }
}
