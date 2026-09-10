package modernmods.phosphophylliterevived.transfer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import modernmods.phosphophylliterevived.util.NonnullDefault;

@NonnullDefault
public final class FluidResourceHandler extends SnapshotJournal<Integer> implements ResourceHandler<FluidResource> {

    private record Operation(FluidResource resource, int amount, boolean insert) {
    }

    private final IFluidHandler handler;
    private final ObjectArrayList<Operation> pending = new ObjectArrayList<>();

    public static ResourceHandler<FluidResource> of(IFluidHandler handler) {
        return new FluidResourceHandler(handler);
    }

    private FluidResourceHandler(IFluidHandler handler) {
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
                handler.fill(operation.resource().toStack(operation.amount()), IFluidHandler.FluidAction.EXECUTE);
            } else {
                handler.drain(operation.resource().toStack(operation.amount()), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        pending.clear();
    }

    private int pendingAmount(FluidResource resource, boolean insert) {
        int total = 0;
        for (int i = 0; i < pending.size(); i++) {
            final var operation = pending.get(i);
            if (operation.insert() == insert && operation.resource().equals(resource)) {
                total += operation.amount();
            }
        }
        return total;
    }

    @Override
    public int size() {
        return handler.getTanks();
    }

    @Override
    public FluidResource getResource(int index) {
        final var stack = handler.getFluidInTank(index);
        if (!stack.isEmpty()) {
            return FluidResource.of(stack);
        }
        for (int i = 0; i < pending.size(); i++) {
            final var operation = pending.get(i);
            if (operation.insert()) {
                return operation.resource();
            }
        }
        return FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        final var stack = handler.getFluidInTank(index);
        long amount = stack.getAmount();
        for (int i = 0; i < pending.size(); i++) {
            final var operation = pending.get(i);
            if (!stack.isEmpty() && !operation.resource().matches(stack)) {
                continue;
            }
            amount += operation.insert() ? operation.amount() : -operation.amount();
        }
        return Math.max(amount, 0);
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return handler.getTankCapacity(index);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return handler.isFluidValid(index, resource.toStack(1));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return insert(resource, amount, transaction);
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        final int alreadyPending = pendingAmount(resource, true);
        final int accepted = handler.fill(resource.toStack(alreadyPending + amount), IFluidHandler.FluidAction.SIMULATE) - alreadyPending;
        if (accepted <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        pending.add(new Operation(resource, accepted, true));
        return accepted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return extract(resource, amount, transaction);
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        final int alreadyPending = pendingAmount(resource, false);
        final var drained = handler.drain(resource.toStack(alreadyPending + amount), IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty() || !resource.matches(drained)) {
            return 0;
        }
        final int available = drained.getAmount() - alreadyPending;
        if (available <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        pending.add(new Operation(resource, available, false));
        return available;
    }
}
