package modernmods.phosphophylliterevived.blocks.whiteholes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import modernmods.phosphophylliterevived.modular.tile.PhosphophylliteTile;
import modernmods.phosphophylliterevived.registry.RegisterTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemWhiteHoleTile extends PhosphophylliteTile implements IItemHandler {
    
    @RegisterTile("item_white_hole")
    public static final BlockEntityType.BlockEntitySupplier<ItemWhiteHoleTile> SUPPLIER = new RegisterTile.Producer<>(ItemWhiteHoleTile::new);
    
    public ItemWhiteHoleTile(BlockEntityType<?> TYPE, BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
    
    @Nullable
    @Override
    public <T> T capability(BlockCapability<T, Direction> cap, final @Nullable Direction side) {
        if (cap == Capabilities.ItemHandler.BLOCK) {
            //noinspection unchecked
            return (T) this;
        }
        return super.capability(cap, side);
    }
    
    Item item = null;
    
    public void setItem(Item item) {
        this.item = item;
    }
    
    @Nonnull
    @Override
    public CompoundTag writeNBT() {
        var compound = super.writeNBT();
        if (item != null) {
            compound.putString("item", Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)).toString());
        }
        return compound;
    }
    
    @Override
    public void readNBT(@Nonnull CompoundTag compound) {
        super.readNBT(compound);
        if (compound.contains("item")) {
            item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(compound.getString("item")));
        }
    }
    
    public void tick() {
        if (item != null) {
            assert level != null;
            for (Direction direction : Direction.values()) {
                BlockEntity te = level.getBlockEntity(worldPosition.relative(direction));
                if (te != null) {
                    final var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, te.getBlockPos(), direction.getOpposite());
                    if (handler != null) {
                        for (int i = 0; i < handler.getSlots(); i++) {
                            handler.insertItem(i, new ItemStack(item, item.getDefaultMaxStackSize()), false);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public int getSlots() {
        return 128;
    }
    
    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        //noinspection deprecation
        return new ItemStack(item, item.getDefaultMaxStackSize());
    }
    
    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }
    
    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return new ItemStack(item, amount);
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
