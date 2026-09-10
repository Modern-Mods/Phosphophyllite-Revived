package modernmods.phosphophylliterevived.blocks.blackholes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import modernmods.phosphophylliterevived.modular.tile.PhosphophylliteTile;
import modernmods.phosphophylliterevived.registry.RegisterTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ItemBlackHoleTile extends PhosphophylliteTile implements IItemHandler {
    
    @RegisterTile("item_black_hole")
    public static final BlockEntityType.BlockEntitySupplier<ItemBlackHoleTile> SUPPLIER = new RegisterTile.Producer<>(ItemBlackHoleTile::new);
    
    public ItemBlackHoleTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Nullable
    @Override
    public <T> T capability(BlockCapability<T, Direction> cap, final @Nullable Direction side) {
        if (cap == Capabilities.Item.BLOCK) {
            //noinspection unchecked
            return (T) modernmods.phosphophylliterevived.transfer.ItemResourceHandler.of(this);
        }
        return super.capability(cap, side);
    }
    
    @Override
    public int getSlots() {
        return 1;
    }
    
    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }
    
    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return ItemStack.EMPTY;
    }
    
    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }
    
    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return true;
    }
}
