package modernmods.phosphophylliterevived.datagen.providers;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import modernmods.phosphophylliterevived.Phosphophyllite;

public class PhosphophylliteModelProvider extends ModelProvider {

    static final String[] CUBE_ALL_BLOCKS = {
            "fluid_black_hole",
            "fluid_white_hole",
            "item_black_hole",
            "item_white_hole",
            "phosphophyllite_ore",
            "power_black_hole",
            "power_white_hole",
    };

    public PhosphophylliteModelProvider(PackOutput output) {
        super(output, Phosphophyllite.modid);
    }

    private static Block block(String name) {
        final var location = Identifier.fromNamespaceAndPath(Phosphophyllite.modid, name);
        final var block = BuiltInRegistries.BLOCK.getValue(location);
        if (block == Blocks.AIR) {
            throw new IllegalStateException("Unknown block " + location);
        }
        return block;
    }

    private static Item item(String name) {
        final var location = Identifier.fromNamespaceAndPath(Phosphophyllite.modid, name);
        final var item = BuiltInRegistries.ITEM.getValue(location);
        if (item == Items.AIR) {
            throw new IllegalStateException("Unknown item " + location);
        }
        return item;
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        for (final var name : CUBE_ALL_BLOCKS) {
            blockModels.createTrivialCube(block(name));
        }
        itemModels.generateFlatItem(item("debug_tool"), ModelTemplates.FLAT_HANDHELD_ITEM);
    }
}
