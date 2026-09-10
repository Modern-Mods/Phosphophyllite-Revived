package modernmods.phosphophylliterevived.mixin.mixins;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import modernmods.phosphophylliterevived.registry.RegistrationContext;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.Properties.class)
public class ItemPropertiesMixin {

    @Shadow
    private @Nullable ResourceKey<Item> id;

    @Inject(method = "itemIdOrThrow", at = @At("HEAD"), cancellable = true)
    private void phos$itemIdOrThrow(CallbackInfoReturnable<ResourceKey<Item>> cir) {
        if (this.id == null) {
            cir.setReturnValue(RegistrationContext.itemKey());
        }
    }
}
