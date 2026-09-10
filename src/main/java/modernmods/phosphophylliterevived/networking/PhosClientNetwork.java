package modernmods.phosphophylliterevived.networking;

import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import modernmods.phosphophylliterevived.util.NonnullDefault;

@NonnullDefault
public final class PhosClientNetwork {

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(PhosNetwork.Payload.TYPE, PhosNetwork::handle);
    }
}
