package modernmods.phosphophylliterevived.multiblock.rectangular;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import modernmods.phosphophylliterevived.modular.api.BlockModule;
import modernmods.phosphophylliterevived.modular.api.IModularBlock;
import modernmods.phosphophylliterevived.modular.api.ModuleRegistry;
import modernmods.phosphophylliterevived.registry.OnModLoad;
import modernmods.phosphophylliterevived.util.BlockStates;
import modernmods.phosphophylliterevived.util.NonnullDefault;

@NonnullDefault
public interface IFaceDirectionBlock extends IModularBlock {
    class Module extends BlockModule<IFaceDirectionBlock> {
    
        public Module(IFaceDirectionBlock iface) {
            super(iface);
        }
    
        @Override
        public void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BlockStates.FACING);
        }
    
        @Override
        public BlockState buildDefaultState(BlockState state) {
            return state.setValue(BlockStates.FACING, Direction.UP);
        }
    
        @OnModLoad
        static void onModLoad() {
            ModuleRegistry.registerBlockModule(IFaceDirectionBlock.class, Module::new);
        }
    }
}
