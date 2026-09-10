package modernmods.phosphophylliterevived;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import modernmods.phosphophylliterevived.config.ConfigType;
import modernmods.phosphophylliterevived.event.ReloadDataEvent;
import modernmods.phosphophylliterevived.multiblock.MultiblockRegistry;
import modernmods.phosphophylliterevived.networking.PhosNetwork;
import modernmods.phosphophylliterevived.registry.RegisterConfig;
import modernmods.phosphophylliterevived.registry.Registry;
import modernmods.phosphophylliterevived.threading.Queues;
import modernmods.phosphophylliterevived.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
@Mod(Phosphophyllite.modid)
public class Phosphophyllite {
    public static final String modid = "phosphophyllite";

    public static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/Main");
    public static long lastTime = 0;
    // used to ensure i dont tick things twice
    private static long tick = 0;

    @RegisterConfig(folder = modid, name = "general", type = {ConfigType.CLIENT, ConfigType.COMMON, ConfigType.SERVER}, rootLevelType = ConfigType.SERVER)
    public static final PhosphophylliteConfig CONFIG = new PhosphophylliteConfig();

    public Phosphophyllite(IEventBus modBus) {
        new Registry(modid, CreativeTabOrder.before(), CreativeTabOrder.after());
        modBus.addListener(PhosNetwork::register);
        NeoForge.EVENT_BUS.register(this);
        if (FMLEnvironment.getDist().isClient()) {
            modBus.addListener(modernmods.phosphophylliterevived.networking.PhosClientNetwork::register);
            NeoForge.EVENT_BUS.addListener(ClientTicker::advanceTick);
        }

        if (CONFIG.bypassPerformantCheck) {
            LOGGER.warn("Performant check bypassed");
            LOGGER.warn("Performant " + (FMLLoader.getCurrent().getLoadingModList().getModFileById("performant") != null ? "is" : "is not") + " present");
        } else {
            if (FMLLoader.getCurrent().getLoadingModList().getModFileById("performant") != null) {
                throw new IllegalStateException("""
                        Performant is incompatible with Phosphophyllite
                        This is a known issue with performant and it breaking other mods, the author does not care
                        GitHub issue on the matter: https://github.com/someaddons/performant_issues/issues/70
                        To bypass this check add "bypassPerformantCheck = true" to the top of your phosphophyllite config
                        If you bypass this check I (RogueLogix) will not provide any support for any issues related to Phosphophyllite or the mods that use it
                        If you believe your issue is unrelated, disable performant and reproduce it
                        By choosing to bypass this check you understand that here there be dragons""");
            }
        }
    }

    private static MinecraftServer server;
    public static ResourceManager serverResourceManager;

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent serverStartedEvent) {
        server = serverStartedEvent.getServer();
        updateRegistries();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent serverStoppedEvent) {
        serverResourceManager = null;
        server = null;
    }

    @SubscribeEvent()
    public void onTagsUpdated(TagsUpdatedEvent tagsUpdatedEvent) {
        updateRegistries();
    }

    void updateRegistries() {
        if (server == null) {
            return;
        }
        if (FMLEnvironment.getDist().isClient()) {
            // ignore client thread
            // prevents double reloads, and reaching across sides
            if (RenderSystem.isOnRenderThread()) {
                return;
            }
        }
        serverResourceManager = server.getResourceManager();
        NeoForge.EVENT_BUS.post(new ReloadDataEvent());
        MultiblockRegistry.revalidateAll();
    }

    public static long tickNumber() {
        return tick;
    }

    @SubscribeEvent
    public void advanceTick(ServerTickEvent.Post e) {
        tick++;

        // prevents deadlock
        final boolean[] run = {true};
        Queues.serverThread.enqueue(() -> run[0] = false);
        while (run[0]) {
            Queues.serverThread.runOne();
        }

        Util.serverTick();
    }

    @SubscribeEvent
    public void tickWorld(LevelTickEvent.Post e) {
        if (!(e.getLevel() instanceof ServerLevel)) {
            return;
        }
        Util.updateBlockStates(e.getLevel());
        Util.worldTickEndEvent(e.getLevel());
    }

    private static class ClientTicker {
        private static void advanceTick(ClientTickEvent.Pre e) {
            // prevents deadlock
            final boolean[] run = {true};
            Queues.clientThread.enqueue(() -> run[0] = false);
            while (run[0]) {
                Queues.clientThread.runOne();
            }

            Util.clientTick();
        }
    }
}
