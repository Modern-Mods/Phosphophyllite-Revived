package modernmods.phosphophylliterevived.datagen.providers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import modernmods.phosphophylliterevived.Phosphophyllite;

public class PhosphophylliteBlockStateProvider extends BlockStateProvider {

    static final String[] CUBE_ALL_BLOCKS = {
            "fluid_black_hole",
            "fluid_white_hole",
            "item_black_hole",
            "item_white_hole",
            "phosphophyllite_ore",
            "power_black_hole",
            "power_white_hole",
    };

    public PhosphophylliteBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Phosphophyllite.modid, existingFileHelper);
    }

    private static Block block(String name) {
        final var location = ResourceLocation.fromNamespaceAndPath(Phosphophyllite.modid, name);
        final var block = BuiltInRegistries.BLOCK.get(location);
        if (block == Blocks.AIR) {
            throw new IllegalStateException("Unknown block " + location);
        }
        return block;
    }

    @Override
    protected void registerStatesAndModels() {
        for (final var name : CUBE_ALL_BLOCKS) {
            simpleBlock(block(name), models().cubeAll("block/" + name,
                    ResourceLocation.fromNamespaceAndPath(Phosphophyllite.modid, "block/" + name)));
        }
    }
}
