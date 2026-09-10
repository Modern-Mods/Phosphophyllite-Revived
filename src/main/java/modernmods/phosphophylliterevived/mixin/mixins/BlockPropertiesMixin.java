package modernmods.phosphophylliterevived.mixin.mixins;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BlockBehaviour.Properties.class)
public class BlockPropertiesMixin {

    @Shadow
    private @Nullable ResourceKey<Block> id;

    @Shadow
    private DependantName<Block, Optional<ResourceKey<LootTable>>> drops;

    @Shadow
    private DependantName<Block, String> descriptionId;

    @Inject(method = "effectiveDrops", at = @At("HEAD"), cancellable = true)
    private void phos$effectiveDrops(CallbackInfoReturnable<Optional<ResourceKey<LootTable>>> cir) {
        if (this.id == null) {
            cir.setReturnValue(this.drops.get(modernmods.phosphophylliterevived.registry.RegistrationContext.blockKey()));
        }
    }

    @Inject(method = "effectiveDescriptionId", at = @At("HEAD"), cancellable = true)
    private void phos$effectiveDescriptionId(CallbackInfoReturnable<String> cir) {
        if (this.id == null) {
            cir.setReturnValue(this.descriptionId.get(modernmods.phosphophylliterevived.registry.RegistrationContext.blockKey()));
        }
    }
}
