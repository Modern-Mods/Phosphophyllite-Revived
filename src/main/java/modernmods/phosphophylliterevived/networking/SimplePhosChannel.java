package modernmods.phosphophylliterevived.networking;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import modernmods.phosphophylliterevived.serialization.PhosphophylliteCompound;
import modernmods.phosphophylliterevived.util.NonnullDefault;

import java.util.function.Consumer;

@NonnullDefault
public class SimplePhosChannel {

    private final ResourceLocation id;
    private final Consumer<PhosphophylliteCompound> callbackFunction;

    public SimplePhosChannel(ResourceLocation id, String version, Consumer<PhosphophylliteCompound> callbackFunction) {
        this.id = id;
        this.callbackFunction = callbackFunction;
        PhosNetwork.registerChannel(id, this::handler);
    }

    private void handler(byte[] data, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> callbackFunction.accept(new PhosphophylliteCompound(data)));
    }

    private static byte[] encode(PhosphophylliteCompound compound) {
        final var robn = compound.toROBN();
        final var bytes = new byte[robn.size()];
        for (int i = 0; i < robn.size(); i++) {
            bytes[i] = robn.getByte(i);
        }
        return bytes;
    }

    public void sendToServer(PhosphophylliteCompound compound) {
        PhosNetwork.sendToServer(id, encode(compound));
    }

    public void sendToPlayer(ServerPlayer serverPlayer, PhosphophylliteCompound compound) {
        PhosNetwork.sendToPlayer(serverPlayer, id, encode(compound));
    }
}
