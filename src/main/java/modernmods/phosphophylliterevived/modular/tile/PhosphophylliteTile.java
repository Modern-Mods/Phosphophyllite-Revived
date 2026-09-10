package modernmods.phosphophylliterevived.modular.tile;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import modernmods.phosphophylliterevived.capabilities.IPhosphophylliteCapabilityProvider;
import modernmods.phosphophylliterevived.debug.DebugInfo;
import modernmods.phosphophylliterevived.debug.IDebuggable;
import modernmods.phosphophylliterevived.modular.api.IModularTile;
import modernmods.phosphophylliterevived.modular.api.ModuleRegistry;
import modernmods.phosphophylliterevived.modular.api.TileModule;
import modernmods.phosphophylliterevived.util.NonnullDefault;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@NonnullDefault
public class PhosphophylliteTile extends BlockEntity implements IModularTile, IDebuggable, IPhosphophylliteCapabilityProvider {
    
    public static final Logger MODULE_LOGGER = LogManager.getLogger("Phosphophyllite/ModularTile");
    @Deprecated(forRemoval = true)
    public static final Logger LOGGER = MODULE_LOGGER;
    
    boolean removed = false;
    private final Reference2ReferenceMap<Class<?>, TileModule<?>> modules = new Reference2ReferenceOpenHashMap<>();
    private final ArrayList<TileModule<?>> moduleList = new ArrayList<>();
    private final List<TileModule<?>> moduleListRO = Collections.unmodifiableList(moduleList);
    
    public PhosphophylliteTile(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
        Class<?> thisClazz = this.getClass();
        ModuleRegistry.forEachTileModule((clazz, constructor) -> {
            if (clazz.isAssignableFrom(thisClazz)) {
                TileModule<?> module = constructor.apply(this);
                modules.put(clazz, module);
                moduleList.ensureCapacity(16);
                moduleList.add(module);
            }
        });
        moduleList.forEach(TileModule::postModuleConstruction);
    }
    
    @Nullable
    public TileModule<?> module(Class<?> interfaceClazz) {
        return modules.get(interfaceClazz);
    }
    
    @Override
    public List<TileModule<?>> modules() {
        return moduleListRO;
    }
    
    @Override
    public final void clearRemoved() {
        super.clearRemoved();
    }
    
    private static final Reference2ReferenceMap<Level, ObjectArrayList<PhosphophylliteTile>> clientWorldUnloadEventTiles = new Reference2ReferenceOpenHashMap<>();
    private static final Reference2ReferenceMap<Level, ObjectArrayList<PhosphophylliteTile>> serverWorldUnloadEventTiles = new Reference2ReferenceOpenHashMap<>();
    private int index = 0;
    
    static {
        NeoForge.EVENT_BUS.addListener(PhosphophylliteTile::serverStopEvent);
        NeoForge.EVENT_BUS.addListener(PhosphophylliteTile::worldUnloadEvent);
    }
    
    private static void worldUnloadEvent(LevelEvent.Unload unload) {
        var removed = (unload.getLevel().isClientSide() ? clientWorldUnloadEventTiles : serverWorldUnloadEventTiles).remove((Level) unload.getLevel());
        if (removed != null) {
            for (int i = 0; i < removed.size(); i++) {
                removed.get(i).remove(true);
            }
        }
    }
    
    private static void serverStopEvent(ServerStoppedEvent stoppedEvent) {
        serverWorldUnloadEventTiles.clear();
    }
    
    @Override
    public final void onLoad() {
        assert level != null;
        super.onLoad();
        moduleList.forEach(TileModule::onAdded);
        onAdded();
        var worldUnloadTiles = (level.isClientSide() ? clientWorldUnloadEventTiles : serverWorldUnloadEventTiles).computeIfAbsent(level, __ -> new ObjectArrayList<>());
        index = worldUnloadTiles.size();
        worldUnloadTiles.add(this);
    }
    
    public void onAdded() {
    }
    
    @Override
    public final void setRemoved() {
        super.setRemoved();
        remove(false);
    }
    
    @Override
    public final void onChunkUnloaded() {
        super.onChunkUnloaded();
        remove(true);
    }
    
    private void remove(boolean chunkUnload) {
        if (removed) {
            return;
        }
        assert level != null;
        onRemoved(chunkUnload);
        moduleList.forEach(module -> module.onRemoved(chunkUnload));
        removed = true;
        if (index == -1) {
            return;
        }
        var worldUnloadTiles = (level.isClientSide() ? clientWorldUnloadEventTiles : serverWorldUnloadEventTiles).get(level);
        if (worldUnloadTiles != null) {
            var arrayTile = worldUnloadTiles.get(index);
            if (arrayTile == this) {
                var removed = worldUnloadTiles.pop();
                if (removed != this) {
                    removed.index = index;
                    worldUnloadTiles.set(index, removed);
                }
                this.index = -1;
            }
        }
    }
    
    public void onRemoved(@SuppressWarnings("unused") boolean chunkUnload) {
    }
    
    @Override
    protected final void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
        super.loadAdditional(compound, registries);
        if (compound.contains("local")) {
            CompoundTag local = compound.getCompound("local");
            readNBT(local);
        }
        CompoundTag subNBTs = compound.getCompound("sub");
        for (var module : moduleList) {
            String key = module.saveKey();
            if (key != null && subNBTs.contains(key)) {
                CompoundTag nbt = subNBTs.getCompound(key);
                module.readNBT(nbt);
            }
        }
    }
    
    @Nullable
    private CompoundTag subNBTs(Function<TileModule<?>, CompoundTag> nbtSupplier) {
        CompoundTag subNBTs = new CompoundTag();
        for (var module : moduleList) {
            CompoundTag nbt = nbtSupplier.apply(module);
            if (nbt != null) {
                String key = module.saveKey();
                if (key != null) {
                    if (subNBTs.contains(key)) {
                        MODULE_LOGGER.warn("Multiple modules with the same save key \"" + key + "\" for tile type \"" + getClass().getSimpleName() + "\" at " + getBlockPos());
                    }
                    subNBTs.put(key, nbt);
                }
            }
        }
        if (subNBTs.isEmpty()) {
            return null;
        }
        return subNBTs;
    }
    
    @Override
    protected final void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        CompoundTag subNBTs = subNBTs(TileModule::writeNBT);
        if (subNBTs != null) {
            nbt.put("sub", subNBTs);
        }
        
        CompoundTag localNBT = writeNBT();
        if (!localNBT.isEmpty()) {
            nbt.put("local", localNBT);
        }
    }
    
    protected void readNBT(CompoundTag compound) {
    }
    
    protected CompoundTag writeNBT() {
        return new CompoundTag();
    }
    
    private static final CompoundTag EMPTY_TAG = new CompoundTag();
    
    @Override
    public final void handleUpdateTag(CompoundTag compound, HolderLookup.Provider registries) {
        super.handleUpdateTag(compound, registries);
        if (compound.contains("local")) {
            CompoundTag local = compound.getCompound("local");
            handleDataNBT(local);
        }
        CompoundTag subNBTs = compound.getCompound("sub");
        for (var module : moduleList) {
            String key = module.saveKey();
            if (key != null) {
                CompoundTag nbt = EMPTY_TAG;
                if (subNBTs.contains(key)) {
                    nbt = subNBTs.getCompound(key);
                }
                module.handleDataNBT(nbt);
                if (nbt == EMPTY_TAG && !nbt.isEmpty()) {
                    MODULE_LOGGER.warn("Module " + key + " wrote to NBT in read!");
                    for (var str : EMPTY_TAG.getAllKeys().toArray(new String[0])) {
                        EMPTY_TAG.remove(str);
                    }
                }
            }
        }
    }
    
    @Override
    public final CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag nbt = super.getUpdateTag(registries);
        CompoundTag subNBTs = subNBTs(TileModule::getDataNBT);
        if (subNBTs != null) {
            nbt.put("sub", subNBTs);
        }
        CompoundTag localNBT = getDataNBT();
        if (!localNBT.isEmpty()) {
            nbt.put("local", localNBT);
        }
        return nbt;
    }
    
    protected void handleDataNBT(CompoundTag nbt) {
        // mimmicks behavior of IForgeTileEntity
        readNBT(nbt);
    }
    
    protected CompoundTag getDataNBT() {
        return writeNBT();
    }
    
    @Override
    public final void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        assert level != null;
        // getters are client only, so, cant grab it on the server even if i want to
        if (!level.isClientSide) {
            return;
        }
        CompoundTag compound = pkt.getTag();
        if (compound == null) {
            return;
        }
        if (compound.contains("local")) {
            CompoundTag local = compound.getCompound("local");
            handleUpdateNBT(local);
        }
        CompoundTag subNBTs = compound.getCompound("sub");
        for (var module : moduleList) {
            String key = module.saveKey();
            if (key != null && subNBTs.contains(key)) {
                CompoundTag nbt = subNBTs.getCompound(key);
                module.handleUpdateNBT(nbt);
            }
        }
    }
    
    @Nullable
    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        boolean sendPacket = false;
        CompoundTag subNBTs = subNBTs(TileModule::getUpdateNBT);
        CompoundTag nbt = new CompoundTag();
        if (subNBTs != null) {
            nbt.put("sub", subNBTs);
        }
        CompoundTag localNBT = getUpdateNBT();
        if (localNBT != null) {
            sendPacket = true;
            nbt.put("local", localNBT);
        }
        if (!sendPacket) {
            return null;
        }
        return ClientboundBlockEntityDataPacket.create(this, (e, registryAccess) -> nbt);
    }
    
    protected void handleUpdateNBT(@SuppressWarnings("unused") CompoundTag nbt) {
    }
    
    @Nullable
    protected CompoundTag getUpdateNBT() {
        return null;
    }
    
    @Nullable
    @Override
    public final <T> T getCapability(final BlockCapability<T, Direction> cap, final @Nullable Direction side) {
        var handler = capability(cap, side);
        for (var module : moduleList) {
            var moduleHandler = module.capability(cap, side);
            if (moduleHandler != null) {
                if (handler != null) {
                    MODULE_LOGGER.warn("Multiple implementations of same capability \"" + cap.name() + "\" on " + side + " side for tile type \"" + getClass().getSimpleName() + "\" at " + getBlockPos());
                    continue;
                }
                handler = moduleHandler;
            }
        }
        return handler;
    }
    
    @Nullable
    protected <T> T capability(final BlockCapability<T, Direction> cap, final @Nullable Direction side) {
        return null;
    }
    
    @Nonnull
    @Override
    public DebugInfo getDebugInfo() {
        final var moduleInfo = new DebugInfo("Module Debug Info");
    
        for (final var moduleEntry : modules.entrySet()) {
            final var moduleDebugInfo = moduleEntry.getValue().getDebugInfo();
            if (moduleDebugInfo == null) {
                final var interfaceClass = moduleEntry.getKey();
                moduleInfo.add(new DebugInfo(interfaceClass.getCanonicalName().substring(interfaceClass.getPackageName().length() + 1)));
                continue;
            }
            moduleInfo.add(moduleDebugInfo);
        }
    
        final var debugInfo = new DebugInfo(this.getClass().getSimpleName());
        debugInfo.add(moduleInfo);
        return debugInfo;
    }
}
