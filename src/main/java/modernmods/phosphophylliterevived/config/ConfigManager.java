package modernmods.phosphophylliterevived.config;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import modernmods.phosphophylliterevived.networking.PhosNetwork;
import modernmods.phosphophylliterevived.parsers.ROBN;
import modernmods.phosphophylliterevived.registry.OnModLoad;
import modernmods.phosphophylliterevived.registry.RegisterConfig;
import modernmods.phosphophylliterevived.util.NonnullDefault;
import modernmods.phosphophylliterevived.util.ReflectionUtil;
import modernmods.phosphophylliterevived.util.TriConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static modernmods.phosphophylliterevived.Phosphophyllite.modid;

@NonnullDefault
public class ConfigManager {
    static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/Config");
    public static final Identifier NETWORK_CHANNEL = Identifier.fromNamespaceAndPath(modid, "phosphophyllite/configsync");
    
    private static final Object2ObjectOpenHashMap<String, ConfigRegistration> clientConfigs = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<String, ConfigRegistration> commonConfigs = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<String, ConfigRegistration> serverConfigs = new Object2ObjectOpenHashMap<>();
    
    private static boolean connectedToServer = false;
    @Nullable
    private static MinecraftServer server;
    private static final ObjectArrayList<ServerPlayer> players = new ObjectArrayList<>();
    
    public static void registerConfig(Object rootConfigObject, RegisterConfig annotation) {
        registerConfig(rootConfigObject, ModLoadingContext.get().getActiveNamespace(), annotation);
    }
    
    public static void registerConfig(Object rootConfigObject, String modName, RegisterConfig annotation) {
        TriConsumer<Map<ConfigType, List<Runnable>>, Method, ConfigType[]> createCallback = (callbacks, method, applicableTypes) -> {
            if (applicableTypes.length == 0) {
                applicableTypes = ConfigType.values();
            }
            final var runnable = ReflectionUtil.createRunnableForFunction(method);
            for (final var type : applicableTypes) {
                var list = callbacks.get(type);
                if (list == null) {
                    list = new ObjectArrayList<>();
                    callbacks.put(type, list);
                }
                list.add(runnable);
            }
        };
        Map<ConfigType, List<Runnable>> registrationCallbacks = new Object2ObjectOpenHashMap<>();
        Map<ConfigType, List<Runnable>> preLoadCallbacks = new Object2ObjectOpenHashMap<>();
        Map<ConfigType, List<Runnable>> postLoadCallbacks = new Object2ObjectOpenHashMap<>();
        for (final var method : rootConfigObject.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(RegisterConfig.Registration.class)) {
                final var callbackAnnotation = method.getAnnotation(RegisterConfig.Registration.class);
                createCallback.accept(registrationCallbacks, method, callbackAnnotation.type());
            }
            if (method.isAnnotationPresent(RegisterConfig.PreLoad.class)) {
                final var callbackAnnotation = method.getAnnotation(RegisterConfig.PreLoad.class);
                createCallback.accept(preLoadCallbacks, method, callbackAnnotation.type());
            }
            if (method.isAnnotationPresent(RegisterConfig.PostLoad.class)) {
                final var callbackAnnotation = method.getAnnotation(RegisterConfig.PostLoad.class);
                createCallback.accept(postLoadCallbacks, method, callbackAnnotation.type());
            }
        }
        registerConfig(
                rootConfigObject, modName, annotation.name(), annotation.folder(),
                annotation.comment(), annotation.format(), annotation.type(), annotation.rootLevelType(), annotation.rootLevelReloadable(),
                registrationCallbacks, preLoadCallbacks, postLoadCallbacks
        );
    }
    
    public static void registerConfig(
            Object rootConfigObject, String modName, String name, String folder,
            String comment, ConfigFormat format, ConfigType[] configTypes, ConfigType rootLevelDefaultType, boolean rootLevelReloadableDefault,
            Map<ConfigType, List<Runnable>> registrationCallbacks, Map<ConfigType, List<Runnable>> preLoadCallbacks, Map<ConfigType, List<Runnable>> postLoadCallbacks) {
        if (configTypes.length == 1) {
            rootLevelDefaultType = rootLevelDefaultType.from(configTypes[0]);
        } else {
            if (rootLevelDefaultType == ConfigType.NULL) {
                throw new IllegalArgumentException("Must specify root level default type when registering multiple config types");
            }
        }
        if (name.isEmpty()) {
            name = modName;
        }
        for (final var configType : Arrays.stream(configTypes).collect(Collectors.toSet())) {
            if (!configType.appliesToPhysicalSide) {
                continue;
            }
            var configs = switch (configType) {
                case NULL -> null;
                case CLIENT -> clientConfigs;
                case COMMON -> commonConfigs;
                case SERVER -> serverConfigs;
            };
            assert configs != null;
            final var registration = new ConfigRegistration(rootConfigObject, modName, name, folder, comment, format, configType, rootLevelDefaultType, rootLevelReloadableDefault, preLoadCallbacks.getOrDefault(configType, new ObjectArrayList<>()), postLoadCallbacks.getOrDefault(configType, new ObjectArrayList<>()));
            if (registration.isEmpty()) {
                continue;
            }
            configs.put(name, registration);
            final var callbacks = registrationCallbacks.get(configType);
            if (callbacks != null) {
                callbacks.forEach(Runnable::run);
            }
            registration.loadLocalConfigFile(false);
        }
    }
    
    // TODO: 8/28/22 command to trigger this
    public static void reloadAllConfigs() {
        for (final var value : clientConfigs.values()) {
            value.loadLocalConfigFile(true);
        }
        if (FMLEnvironment.getDist().isDedicatedServer() || server == null || !server.isDedicatedServer()) {
            // dedicated servers, disconnected clients, and integrated servers reload common and server configs too
            for (final var value : commonConfigs.values()) {
                value.loadLocalConfigFile(true);
            }
            for (final var value : serverConfigs.values()) {
                value.loadLocalConfigFile(true);
            }
            for (ServerPlayer player : players) {
                sendConfigToPlayer(player, false);
            }
        }
    }
    
    public static List<ConfigRegistration> getAllConfigsForMod(String modName) {
        return Stream.concat(clientConfigs.values().stream(), Stream.concat(commonConfigs.values().stream(), serverConfigs.values().stream()))
                .filter(configRegistration -> configRegistration.modName.equals(modName)).collect(Collectors.toList());
    }
    
    
    @OnModLoad
    private static void onModLoad() {
        PhosNetwork.registerChannel(NETWORK_CHANNEL, ConfigManager::packetHandler);
        NeoForge.EVENT_BUS.addListener(ConfigManager::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(ConfigManager::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(ConfigManager::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(ConfigManager::onServerStopped);
        if (FMLEnvironment.getDist().isClient()) {
            NeoForge.EVENT_BUS.addListener(ConfigManager::onLoggingIn);
            NeoForge.EVENT_BUS.addListener(ConfigManager::onLoggingOut);
        }
    }
    
    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        server = event.getServer();
    }
    
    private static void onServerStopped(ServerStoppedEvent event) {
        server = null;
        players.clear();
    }
    
    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        connectedToServer = true;
    }
    
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        connectedToServer = false;
        commonConfigs.values().forEach(ConfigRegistration::unloadRemoteConfig);
    }
    
    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent e) {
        var server = e.getEntity().level().getServer();
        assert server != null;
        if (!server.isDedicatedServer()) {
            var serverUUID = e.getEntity().getUUID();
            var localUUID = Minecraft.getInstance().getUser().getProfileId().toString();
            if (serverUUID.toString().equals(localUUID)) {
                // ignore local player on integrated server
                // do have the configs reload the saved tree though
                for (ConfigRegistration value : commonConfigs.values()) {
                    value.unloadRemoteConfig();
                }
                return;
            }
        }
        var player = (ServerPlayer) e.getEntity();
        players.add(player);
        sendConfigToPlayer(player, true);
    }
    
    private static void sendConfigToPlayer(ServerPlayer player, boolean initialLogin) {
        final var configs = new Object2ObjectOpenHashMap<String, ByteArrayList>();
        for (ConfigRegistration modConfig : commonConfigs.values()) {
            var configTree = modConfig.rootConfigSpecNode.generateSyncElement();
            var configROBN = ROBN.parseElement(configTree);
            if (configROBN != null) {
                configs.put(modConfig.baseFile.toString(), configROBN);
            }
        }
        var robn = modernmods.phosphophylliterevived.robn.ROBN.toROBN(new Pair<>(initialLogin, configs));
        var bytes = new byte[robn.size()];
        for (int i = 0; i < robn.size(); i++) {
            bytes[i] = robn.get(i);
        }
        PhosNetwork.sendToPlayer(player, NETWORK_CHANNEL, bytes);
    }
    
    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        var player = (ServerPlayer) e.getEntity();
        players.remove(player);
    }
    
    private static void packetHandler(byte[] packetBytes, IPayloadContext ctx) {
        if (!ctx.flow().isClientbound()) {
            return;
        }
        ctx.enqueueWork(() -> {
            try {
                ArrayList<Byte> buf = new ArrayList<>();
                for (byte aByte : packetBytes) {
                    buf.add(aByte);
                }
                //noinspection unchecked
                final var pair = (Pair<Boolean, Map<String, List<Byte>>>) modernmods.phosphophylliterevived.robn.ROBN.fromROBN(buf);
                boolean initialLogin = pair.getFirst();
                final var configs = pair.getSecond();
                for (final var entry : configs.entrySet()) {
                    final var configName = entry.getKey();
                    final var config = commonConfigs.get(configName);
                    if (config == null) {
                        return;
                    }
                    try {
                        final var elementTree = ROBN.parseROBN(entry.getValue());
                        config.loadRemoteConfig(elementTree, !initialLogin);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } catch (ClassCastException ignored) {
            }
        });
    }
    
}
