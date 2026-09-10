package modernmods.phosphophylliterevived.datagen;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import modernmods.phosphophylliterevived.Phosphophyllite;
import modernmods.phosphophylliterevived.datagen.providers.PhosphophylliteBlockStateProvider;
import modernmods.phosphophylliterevived.datagen.providers.PhosphophylliteItemModelProvider;
import modernmods.phosphophylliterevived.datagen.providers.PhosphophylliteLanguageProvider;

@EventBusSubscriber(modid = Phosphophyllite.modid, bus = EventBusSubscriber.Bus.MOD)
public final class PhosphophylliteDatagen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        final var generator = event.getGenerator();
        final var output = generator.getPackOutput();
        final var existingFileHelper = event.getExistingFileHelper();

        for (final var locale : PhosphophylliteLanguageProvider.LOCALES) {
            generator.addProvider(event.includeClient(), new PhosphophylliteLanguageProvider(output, locale));
        }
        generator.addProvider(event.includeClient(), new PhosphophylliteBlockStateProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new PhosphophylliteItemModelProvider(output, existingFileHelper));
    }
}
