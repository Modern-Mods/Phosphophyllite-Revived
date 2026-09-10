package modernmods.phosphophylliterevived.transfer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import modernmods.phosphophylliterevived.util.NonnullDefault;

@NonnullDefault
public final class ItemResourceHandler extends SnapshotJournal<Integer> implements ResourceHandler<ItemResource> {

    private record Operation(int index, ItemResource resource, int amount, boolean insert) {
    }

    private final IItemHandler handler;
    private final ObjectArrayList<Operation> pending = new ObjectArrayList<>();

    public static ResourceHandler<ItemResource> of(IItemHandler handler) {
        return new ItemResourceHandler(handler);
    }

    private ItemResourceHandler(IItemHandler handler) {
        this.handler = handler;
    }

    @Override
    protected Integer createSnapshot() {
        return pending.size();
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
        pending.size(snapshot);
    }

    @Override
    protected void onRootCommit(Integer originalState) {
        for (int i = 0; i < pending.size(); i++) {
            final var operation = pending.get(i);
            if (operation.insert()) {
                handler.insertItem(operation.index(), operation.resource().toStack(operation.amount()), false);
            } else {
                handler.extractItem(operation.index(), operation.amount(), false);
            }
        }
        pending.clear();
    }

    private int pendingAmount(int index, ItemResource resource, boolean insert) {
        int total = 0;
        for (int i = 0; i < pending.size(); i++) {
            final var operation = pending.get(i);
            if (operation.index() == index && operation.insert() == insert && operation.resource().equals(resource)) {
                total += operation.amount();
            }
        }
        return total;
    }

    @Override
    public int size() {
        return handler.getSlots();
    }

    @Override
    public ItemResource getResource(int index) {
        final var stack = handler.getStackInSlot(index);
        if (!stack.isEmpty()) {
            return ItemResource.of(stack);
        }
        for (int i = 0; i < pending.size(); i++) {
            final var operation = pending.get(i);
            if (operation.index() == index && operation.insert()) {
                return operation.resource();
            }
        }
        return ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        long amount = handler.getStackInSlot(index).getCount();
        for (int i = 0; i < pending.size(); i++) {
            final var operation = pending.get(i);
            if (operation.index() != index) {
                continue;
            }
            amount += operation.insert() ? operation.amount() : -operation.amount();
        }
        return Math.max(amount, 0);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return handler.getSlotLimit(index);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return handler.isItemValid(index, resource.toStack(1));
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        final int alreadyPending = pendingAmount(index, resource, true);
        final ItemStack probe = resource.toStack(alreadyPending + amount);
        final ItemStack remainder = handler.insertItem(index, probe, true);
        final int accepted = probe.getCount() - remainder.getCount() - alreadyPending;
        if (accepted <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        pending.add(new Operation(index, resource, accepted, true));
        return accepted;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        int inserted = 0;
        for (int slot = 0; slot < handler.getSlots() && inserted < amount; slot++) {
            inserted += insert(slot, resource, amount - inserted, transaction);
        }
        return inserted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        final int alreadyPending = pendingAmount(index, resource, false);
        final ItemStack extracted = handler.extractItem(index, alreadyPending + amount, true);
        if (extracted.isEmpty() || !resource.matches(extracted)) {
            return 0;
        }
        final int available = extracted.getCount() - alreadyPending;
        if (available <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        pending.add(new Operation(index, resource, available, false));
        return available;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        int extracted = 0;
        for (int slot = 0; slot < handler.getSlots() && extracted < amount; slot++) {
            extracted += extract(slot, resource, amount - extracted, transaction);
        }
        return extracted;
    }
}
