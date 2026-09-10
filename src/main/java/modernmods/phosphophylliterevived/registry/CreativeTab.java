package modernmods.phosphophylliterevived.registry;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.RegisterEvent;

import javax.annotation.Nonnull;
import java.util.List;

public class CreativeTab {

    private final ResourceLocation location;
    private final Component title;
    private final List<ResourceLocation> tabsBefore;
    private final List<ResourceLocation> tabsAfter;
    private final ObjectArrayList<Item> items = new ObjectArrayList<>();
    private Item icon = Items.STONE;

    public CreativeTab(@Nonnull String modNamespace, @Nonnull List<ResourceLocation> tabsBefore, @Nonnull List<ResourceLocation> tabsAfter) {
        location = ResourceLocation.fromNamespaceAndPath(modNamespace, "creative_tab");
        title = Component.translatable("item_group." + modNamespace);
        tabsBefore.add(CreativeModeTabs.SPAWN_EGGS.location());
        this.tabsBefore = tabsBefore;
        this.tabsAfter = tabsAfter;
    }

    public void add(Item item) {
        items.add(item);
    }

    public void icon(Item item) {
        icon = item;
    }

    public ResourceLocation location() {
        return location;
    }

    public void registerEvent(RegisterEvent registerEvent) {
        registerEvent.register(Registries.CREATIVE_MODE_TAB, this::register);
    }

    private void register(RegisterEvent.RegisterHelper<CreativeModeTab> event) {
        if (items.isEmpty()) {
            return;
        }
        final var tabBuilder = CreativeModeTab.builder();
        tabsBefore.forEach(tabBuilder::withTabsBefore);
        tabsAfter.forEach(tabBuilder::withTabsAfter);
        tabBuilder.title(title);
        tabBuilder.icon(() -> new ItemStack(icon));
        tabBuilder.displayItems((CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) -> {
            items.forEach(output::accept);
        });
        event.register(location, tabBuilder.build());
    }
}
