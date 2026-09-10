package modernmods.phosphophylliterevived.client.gui;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import modernmods.phosphophylliterevived.networking.PhosNetwork;
import modernmods.phosphophylliterevived.Phosphophyllite;
import modernmods.phosphophylliterevived.registry.OnModLoad;
import modernmods.phosphophylliterevived.robn.ROBN;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static modernmods.phosphophylliterevived.Phosphophyllite.modid;

public class GuiSync {
    
    public interface IGUIPacketProvider {
        
        @Nullable
        IGUIPacket getGuiPacket();
        
        /**
         * DO NOT OVERRIDE THIS METHOD!
         *
         * @param requestName The request to make.
         * @param requestData The payload to send.
         */
        default void runRequest(@Nonnull String requestName, @Nullable Object requestData) {
            HashMap<String, Object> map = new HashMap<>();
            
            map.put("request", requestName);
            if (requestData != null) {
                map.put("data", requestData);
            }
            
            final var buf = ROBN.toROBN(map);
            
            final var bytes = new byte[buf.size()];
            for (int i = 0; i < buf.size(); i++) {
                bytes[i] = buf.get(i);
            }
            
            PhosNetwork.sendToServer(CHANNEL, bytes);
        }
        
        default void executeRequest(String requestName, Object requestData) {
        }
    }
    
    public interface IGUIPacket {
        void read(@Nonnull Map<?, ?> data);
        
        @Nullable
        Map<?, ?> write();
    }
    
    private static final HashMap<Player, IGUIPacketProvider> playerGUIs = new HashMap<>();
    
    public static synchronized void onContainerOpen(@Nonnull PlayerContainerEvent.Open e) {
        AbstractContainerMenu container = e.getContainer();
        if (container instanceof IGUIPacketProvider) {
            playerGUIs.put(e.getEntity(), (IGUIPacketProvider) container);
        }
    }
    
    public static synchronized void onContainerClose(@Nonnull PlayerContainerEvent.Close e) {
        playerGUIs.remove(e.getEntity());
    }
    
    private static IGUIPacketProvider currentGUI;
    
    @OnlyIn(Dist.CLIENT)
    public static synchronized void GuiOpenEvent(@Nonnull ScreenEvent.Opening e) {
        
        Screen gui = e.getScreen();
        if (gui instanceof AbstractContainerScreen) {
            AbstractContainerMenu container = ((AbstractContainerScreen<?>) gui).getMenu();
            if (container instanceof IGUIPacketProvider) {
                currentGUI = (IGUIPacketProvider) container;
            }
        } else {
            currentGUI = null;
        }
    }
    
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(modid, "multiblock/guisync");
    
    @OnModLoad
    public static void onModLoad() {
        PhosNetwork.registerChannel(CHANNEL, GuiSync::handler);
        NeoForge.EVENT_BUS.addListener(GuiSync::onContainerClose);
        NeoForge.EVENT_BUS.addListener(GuiSync::onContainerOpen);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(GuiSync::GuiOpenEvent);
        }
        Thread updateThread = new Thread(() -> {
            while (true) {
                synchronized (GuiSync.class) {
                    playerGUIs.forEach((player, gui) -> {
                        try {
                            assert player instanceof ServerPlayer;
                            IGUIPacket packet = gui.getGuiPacket();
                            if (packet == null) {
                                return;
                            }
                            Map<?, ?> packetMap = packet.write();
                            if (packetMap == null) {
                                return;
                            }
                            ByteArrayList buf;
                            try {
                                buf = ROBN.toROBN(packetMap);
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                                return;
                            }
                            final var bytes = new byte[buf.size()];
                            for (int i = 0; i < buf.size(); i++) {
                                bytes[i] = buf.get(i);
                            }
                            PhosNetwork.sendToPlayer((ServerPlayer) player, CHANNEL, bytes);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(Phosphophyllite.CONFIG.gui.UpdateIntervalMS);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updateThread.setName("Phosphophyllite-GuiSync");
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    private static void handler(byte[] packetBytes, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            IGUIPacketProvider currentGUI;
            ArrayList<Byte> buf = new ArrayList<>();
            for (byte aByte : packetBytes) {
                buf.add(aByte);
            }
            Map<?, ?> map = (Map<?, ?>) ROBN.fromROBN(buf);
            
            if (ctx.flow().isClientbound()) {
                currentGUI = GuiSync.currentGUI;
                if (currentGUI != null) {
                    IGUIPacket guiPacket = currentGUI.getGuiPacket();
                    if (guiPacket != null) {
                        guiPacket.read(map);
                    }
                }
            } else {
                currentGUI = playerGUIs.get(ctx.player());
                if (currentGUI != null) {
                    currentGUI.executeRequest((String) map.get("request"), map.get("data"));
                }
            }
        });
    }
}
