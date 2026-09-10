package modernmods.phosphophylliterevived.mixin.mixins;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootTable;
import modernmods.phosphophylliterevived.registry.RegistrationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {

    @Shadow
    @Final
    protected Optional<ResourceKey<LootTable>> drops;

    @Shadow
    @Final
    protected String descriptionId;

    @Unique
    private static boolean phos$unresolved(String value) {
        return value.contains(RegistrationContext.UNRESOLVED.getPath());
    }

    @Inject(method = "getLootTable", at = @At("HEAD"), cancellable = true)
    private void phos$getLootTable(CallbackInfoReturnable<Optional<ResourceKey<LootTable>>> cir) {
        final var current = this.drops.orElse(null);
        if (current == null || !phos$unresolved(current.identifier().getPath())) {
            return;
        }
        if (!(((Object) this) instanceof Block block)) {
            return;
        }
        final var key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null) {
            return;
        }
        cir.setReturnValue(Optional.of(ResourceKey.create(Registries.LOOT_TABLE, key.withPrefix("blocks/"))));
    }

    @Inject(method = "getDescriptionId", at = @At("HEAD"), cancellable = true)
    private void phos$getDescriptionId(CallbackInfoReturnable<String> cir) {
        if (!phos$unresolved(this.descriptionId)) {
            return;
        }
        if (!(((Object) this) instanceof Block block)) {
            return;
        }
        final var key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null) {
            return;
        }
        cir.setReturnValue(Util.makeDescriptionId("block", key));
    }
}
