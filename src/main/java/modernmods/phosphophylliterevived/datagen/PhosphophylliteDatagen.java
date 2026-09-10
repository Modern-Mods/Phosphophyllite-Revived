package modernmods.phosphophylliterevived.datagen;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import modernmods.phosphophylliterevived.Phosphophyllite;
import modernmods.phosphophylliterevived.datagen.providers.PhosphophylliteLanguageProvider;
import modernmods.phosphophylliterevived.datagen.providers.PhosphophylliteModelProvider;

@EventBusSubscriber(modid = Phosphophyllite.modid)
public final class PhosphophylliteDatagen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        for (final var locale : PhosphophylliteLanguageProvider.LOCALES) {
            event.createProvider(output -> new PhosphophylliteLanguageProvider(output, locale));
        }
        event.createProvider(PhosphophylliteModelProvider::new);
    }
}
