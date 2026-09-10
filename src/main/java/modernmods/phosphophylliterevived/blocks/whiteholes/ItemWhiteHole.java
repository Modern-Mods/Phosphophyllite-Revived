package modernmods.phosphophylliterevived.blocks.whiteholes;


import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import modernmods.phosphophylliterevived.registry.RegisterBlock;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class ItemWhiteHole extends Block implements EntityBlock {
    
    @RegisterBlock(name = "item_white_hole", tileEntityClass = ItemWhiteHoleTile.class)
    public static final ItemWhiteHole INSTANCE = new ItemWhiteHole();
    
    public ItemWhiteHole() {
        super(Properties.of());
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ItemWhiteHoleTile.SUPPLIER.create(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153212_, BlockState p_153213_, BlockEntityType<T> p_153214_) {
        return (level, pos, state, entity) -> {
            assert entity instanceof ItemWhiteHoleTile;
            ((ItemWhiteHoleTile) entity).tick();
        };
    }
    
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level worldIn, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity te = worldIn.getBlockEntity(pos);
        if (te instanceof ItemWhiteHoleTile) {
            Item item = player.getMainHandItem().getItem();
            ((ItemWhiteHoleTile) te).setItem(item);
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, worldIn, pos, player, hit);
    }
    

}
