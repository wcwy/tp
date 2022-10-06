package seedu.duke.command;

import seedu.duke.Storage;
import seedu.duke.Ui;
import seedu.duke.common.InfoMessages;
import seedu.duke.data.TransactionList;

public class PurgeCommand extends Command {
    @Override
    public void execute(TransactionList transactions, Ui ui, Storage storage) {
        // Shows confirmation prompt before deleting all transactions
        Ui.showInfoMessage(InfoMessages.INFO_WARNING.toString());
        String input = ui.readCommand();
        if (input.equals("Y")) {
            TransactionList.purgeEntries(transactions);
        } else {
            System.out.println(InfoMessages.INFO_DIVIDER);
            System.out.println("MOOOOOO.... Aborting Command, returning to Home.");
            System.out.println(InfoMessages.INFO_DIVIDER);
        }
    }

    @Override
    public boolean isExit() {
        return false;
    }
}
