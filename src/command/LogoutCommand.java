package command;

import manager.MessageManager;
import model.Student;
import ui.CLIHelper;

import java.util.Scanner;

/**
 * COMMAND PATTERN — Log out of the current session.
 *
 * OBSERVER PATTERN: Unregisters the current student from MessageManager
 * so they stop receiving live notifications after logout.
 * Any unread messages remain in the CSV for the next session.
 */
public class LogoutCommand implements Command {

    private final Student        currentUser;
    private final MessageManager messageManager;
    private final Scanner        scanner;

    /** Runnable called when logout completes — used to exit the main-menu loop. */
    private final Runnable onLogout;

    public LogoutCommand(Student currentUser, MessageManager messageManager,
                         Scanner scanner, Runnable onLogout) {
        this.currentUser    = currentUser;
        this.messageManager = messageManager;
        this.scanner        = scanner;
        this.onLogout       = onLogout;
    }

    @Override
    public String getLabel() { return "Logout"; }

    @Override
    public void execute() {
        System.out.println("\n  Goodbye, " + currentUser.getUsername() + "! See you next time.");

        // OBSERVER PATTERN: deregister from the Subject so no more notifications arrive
        messageManager.removeObserver(currentUser);

        onLogout.run(); // signal the menu loop to stop
    }
}
