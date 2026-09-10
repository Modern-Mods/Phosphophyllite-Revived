package modernmods.phosphophylliterevived.fluids;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import modernmods.phosphophylliterevived.transfer.TransferUtil;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ResourceFluidHandlerWrapper implements IPhosphophylliteFluidHandler {

    public static IPhosphophylliteFluidHandler wrap(ResourceHandler<FluidResource> handler) {
        if (handler instanceof IPhosphophylliteFluidHandler phosHandler) {
            return phosHandler;
        }
        return new ResourceFluidHandlerWrapper(handler);
    }

    private final ResourceHandler<FluidResource> handler;

    private ResourceFluidHandlerWrapper(ResourceHandler<FluidResource> handler) {
        this.handler = handler;
    }

    @Override
    public int tankCount() {
        return handler.size();
    }

    @Override
    public long tankCapacity(int tank) {
        return handler.getCapacityAsLong(tank, handler.getResource(tank));
    }

    @Override
    public Fluid fluidTypeInTank(int tank) {
        return handler.getResource(tank).getFluid();
    }

    @Override
    public DataComponentPatch fluidComponentsInTank(int tank) {
        return handler.getResource(tank).getComponentsPatch();
    }

    @Override
    public long fluidAmountInTank(int tank) {
        return handler.getAmountAsLong(tank);
    }

    @Override
    public boolean fluidValidForTank(int tank, Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return false;
        }
        return handler.isValid(tank, FluidResource.of(fluid));
    }

    @Override
    public long fill(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return 0;
        }
        try (final var transaction = TransferUtil.openTransaction()) {
            final int filled = handler.insert(FluidResource.of(fluid, components), clamp(amount), transaction);
            if (!simulate) {
                transaction.commit();
            }
            return filled;
        }
    }

    @Override
    public long drain(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return 0;
        }
        try (final var transaction = TransferUtil.openTransaction()) {
            final int drained = handler.extract(FluidResource.of(fluid, components), clamp(amount), transaction);
            if (!simulate) {
                transaction.commit();
            }
            return drained;
        }
    }

    private static int clamp(long amount) {
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }
}
