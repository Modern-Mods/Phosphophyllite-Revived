package modernmods.phosphophylliterevived.multiblock.touching;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import modernmods.phosphophylliterevived.modular.api.IModularTile;
import modernmods.phosphophylliterevived.modular.api.ModuleRegistry;
import modernmods.phosphophylliterevived.modular.api.TileModule;
import modernmods.phosphophylliterevived.multiblock.IMultiblockTile;
import modernmods.phosphophylliterevived.multiblock.MultiblockController;
import modernmods.phosphophylliterevived.multiblock.common.IPersistentMultiblock;
import modernmods.phosphophylliterevived.multiblock.common.IPersistentMultiblockTile;
import modernmods.phosphophylliterevived.multiblock.modular.ICoreMultiblockTileModule;
import modernmods.phosphophylliterevived.multiblock.rectangular.IRectangularMultiblock;
import modernmods.phosphophylliterevived.multiblock.rectangular.IRectangularMultiblockBlock;
import modernmods.phosphophylliterevived.multiblock.rectangular.IRectangularMultiblockTile;
import modernmods.phosphophylliterevived.registry.OnModLoad;
import modernmods.phosphophylliterevived.util.NonnullDefault;
import modernmods.phosphophylliterevived.util.VectorUtil;
import org.joml.Vector3i;

@NonnullDefault
public interface ITouchingMultiblockTile<
        TileType extends BlockEntity & ITouchingMultiblockTile<TileType, BlockType, ControllerType> & IRectangularMultiblockTile<TileType, BlockType, ControllerType> & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IRectangularMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & ITouchingMultiblock<TileType, BlockType, ControllerType> & IRectangularMultiblock<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
        > extends IMultiblockTile<TileType, BlockType, ControllerType> {
    
    final class Module<
            TileType extends BlockEntity & ITouchingMultiblockTile<TileType, BlockType, ControllerType> & IRectangularMultiblockTile<TileType, BlockType, ControllerType> & IPersistentMultiblockTile<TileType, BlockType, ControllerType>,
            BlockType extends Block & IRectangularMultiblockBlock,
            ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & ITouchingMultiblock<TileType, BlockType, ControllerType> & IRectangularMultiblock<TileType, BlockType, ControllerType> & IPersistentMultiblock<TileType, BlockType, ControllerType>
            > extends TileModule<TileType> implements ICoreMultiblockTileModule<TileType, BlockType, ControllerType> {
        
        boolean assembled = false;
        final Vector3i min = new Vector3i();
        final Vector3i max = new Vector3i();
        
        @OnModLoad
        public static void register() {
            ModuleRegistry.registerTileModule(ITouchingMultiblockTile.class, Module::new);
        }
        
        public Module(IModularTile iface) {
            super(iface);
        }
        
        @Override
        public boolean shouldConnectTo(TileType tile, Direction direction) {
            if (!assembled) {
                return true;
            }
            return VectorUtil.lequal(min, tile.getBlockPos()) && VectorUtil.grequal(max, tile.getBlockPos());
        }
    
        @Override
        public String saveKey() {
            return "touching_multiblock";
        }
    
        @Override
        public void readNBT(CompoundTag nbt) {
            assembled = nbt.getBoolean("assembled");
            min.set(nbt.getInt("minx"), nbt.getInt("miny"), nbt.getInt("minz"));
            max.set(nbt.getInt("maxx"), nbt.getInt("maxy"), nbt.getInt("maxz"));
        }
        
        @Override
        public CompoundTag writeNBT() {
            final var tag = new CompoundTag();
            tag.putBoolean("assembled", assembled);
            tag.putInt("minx", min.x());
            tag.putInt("miny", min.y());
            tag.putInt("minz", min.z());
            tag.putInt("maxx", max.x());
            tag.putInt("maxy", max.y());
            tag.putInt("maxz", max.z());
            return tag;
        }
    }
}
