package modernmods.phosphophylliterevived.capabilities;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import modernmods.phosphophylliterevived.registry.OnModLoad;
import modernmods.phosphophylliterevived.util.NonnullDefault;

import java.util.Collections;
import java.util.List;

@NonnullDefault
public final class PhosphophylliteCapabilities {
    
    private static final ObjectArrayList<BlockCapability<?, Direction>> blockCapabilities = new ObjectArrayList<>();
    private static final List<BlockCapability<?, Direction>> blockCapabilitiesRO = Collections.unmodifiableList(blockCapabilities);
    
    public static synchronized void registerBlockCapability(BlockCapability<?, Direction> capability) {
        if (!blockCapabilities.contains(capability)) {
            blockCapabilities.add(capability);
        }
    }
    
    public static List<BlockCapability<?, Direction>> blockCapabilities() {
        return blockCapabilitiesRO;
    }
    
    @OnModLoad
    private static void onModLoad() {
        registerBlockCapability(Capabilities.ItemHandler.BLOCK);
        registerBlockCapability(Capabilities.FluidHandler.BLOCK);
        registerBlockCapability(Capabilities.EnergyStorage.BLOCK);
    }
}
