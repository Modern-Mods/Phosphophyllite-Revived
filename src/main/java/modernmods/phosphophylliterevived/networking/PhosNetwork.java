package modernmods.phosphophylliterevived.networking;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import modernmods.phosphophylliterevived.Phosphophyllite;
import modernmods.phosphophylliterevived.util.NonnullDefault;

@NonnullDefault
public final class PhosNetwork {

    public interface PacketHandler {
        void handle(byte[] data, IPayloadContext context);
    }

    public record Payload(Identifier channel, byte[] data) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<Payload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Phosphophyllite.modid, "compound"));

        public static final StreamCodec<FriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Payload::channel,
                ByteBufCodecs.BYTE_ARRAY, Payload::data,
                Payload::new
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static final Object2ObjectMap<Identifier, PacketHandler> handlers = new Object2ObjectOpenHashMap<>();

    public static synchronized void registerChannel(Identifier channel, PacketHandler handler) {
        handlers.put(channel, handler);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playBidirectional(Payload.TYPE, Payload.STREAM_CODEC, PhosNetwork::handle);
    }

    static void handle(Payload payload, IPayloadContext context) {
        final PacketHandler handler;
        synchronized (PhosNetwork.class) {
            handler = handlers.get(payload.channel());
        }
        if (handler == null) {
            return;
        }
        handler.handle(payload.data(), context);
    }

    public static void sendToServer(Identifier channel, byte[] data) {
        ClientPacketDistributor.sendToServer(new Payload(channel, data));
    }

    public static void sendToPlayer(ServerPlayer player, Identifier channel, byte[] data) {
        PacketDistributor.sendToPlayer(player, new Payload(channel, data));
    }
}
