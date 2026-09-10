package modernmods.phosphophylliterevived.transfer;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import modernmods.phosphophylliterevived.util.NonnullDefault;

@NonnullDefault
public final class TransferUtil {

    public static Transaction openTransaction() {
        final var current = Transaction.getCurrentOpenedTransaction();
        return current == null ? Transaction.openRoot() : Transaction.open(current);
    }
}
