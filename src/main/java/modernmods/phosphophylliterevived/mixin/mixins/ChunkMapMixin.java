package modernmods.phosphophylliterevived.mixin.mixins;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @Inject(method = "saveChunksEagerly", at = @At("HEAD"), cancellable = true)
    private void saveChunksEagerly(BooleanSupplier haveTime, CallbackInfo ci) {
        ci.cancel();
    }
}
