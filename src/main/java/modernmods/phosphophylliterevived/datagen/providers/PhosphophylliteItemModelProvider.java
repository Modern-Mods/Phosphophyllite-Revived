package modernmods.phosphophylliterevived.datagen.providers;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import modernmods.phosphophylliterevived.Phosphophyllite;

public class PhosphophylliteItemModelProvider extends ItemModelProvider {

    public PhosphophylliteItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Phosphophyllite.modid, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        withExistingParent("debug_tool", "item/handheld")
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(Phosphophyllite.modid, "item/debug_tool"));

        for (final var name : PhosphophylliteBlockStateProvider.CUBE_ALL_BLOCKS) {
            withExistingParent(name, ResourceLocation.fromNamespaceAndPath(Phosphophyllite.modid, "block/" + name));
        }
    }
}
