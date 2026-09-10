package modernmods.phosphophylliterevived.client;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import modernmods.phosphophylliterevived.registry.Registry;
import modernmods.phosphophylliterevived.util.NonnullDefault;

import javax.annotation.Nullable;

@NonnullDefault
public final class FluidModelRegistrar {

    @Nullable
    private static RegisterFluidModelsEvent handled;

    public static void register(RegisterFluidModelsEvent event) {
        if (handled == event) {
            return;
        }
        handled = event;
        for (Registry.FluidClientInfo info : Registry.FLUID_TEXTURES) {
            final var unbaked = new FluidModel.Unbaked(
                    new Material(info.stillTexture()),
                    new Material(info.flowingTexture()),
                    new Material(info.overlayTexture()),
                    FluidTintSources.constant(info.tintColor()));
            event.register(unbaked, info.still(), info.flowing());
        }
    }
}
