package modernmods.phosphophylliterevived.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;

public final class RegistrationContext {

    public static final Identifier UNRESOLVED = Identifier.fromNamespaceAndPath("phosphophyllite", "unresolved");

    private static final ThreadLocal<Identifier> CURRENT = new ThreadLocal<>();

    public static void push(Identifier location) {
        CURRENT.set(location);
    }

    public static void pop() {
        CURRENT.remove();
    }

    @Nullable
    public static Identifier current() {
        return CURRENT.get();
    }

    public static ResourceKey<Block> blockKey() {
        final var location = CURRENT.get();
        return ResourceKey.create(Registries.BLOCK, location == null ? UNRESOLVED : location);
    }

    public static ResourceKey<Item> itemKey() {
        final var location = CURRENT.get();
        return ResourceKey.create(Registries.ITEM, location == null ? UNRESOLVED : location);
    }
}
