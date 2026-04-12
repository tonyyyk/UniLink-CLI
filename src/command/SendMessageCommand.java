package command;

import manager.MessageManager;
import manager.UserManager;
import model.Student;
import ui.CLIHelper;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * COMMAND PATTERN — Send a message to another student.
 *
 * Integrates TWO design patterns:
 *
 * 1. STATE PATTERN: Checks canSendMessage() before proceeding.
 *    A SuspendedState returns false, blocking the action with a clear
 *    error message rather than null checks or try/catch guards.
 *
 * 2. OBSERVER PATTERN: MessageManager.sendMessage() appends to the CSV
 *    and immediately calls notifyObservers() — if the recipient is online,
 *    their Student.update() is called and the alert will appear on their
 *    next menu render. This is the asynchronous delivery chain.
 */
public class SendMessageCommand implements Command {

    private final Student        currentUser;
    private final MessageManager messageManager;
    private final UserManager    userManager;
    private final Scanner        scanner;

    public SendMessageCommand(Student currentUser, MessageManager messageManager,
                              UserManager userManager, Scanner scanner) {
        this.currentUser    = currentUser;
        this.messageManager = messageManager;
        this.userManager    = userManager;
        this.scanner        = scanner;
    }

    @Override
    public String getLabel() { return "Send Message"; }

    @Override
    public void execute() {
        CLIHelper.printBanner("Send Message");

        // STATE PATTERN: gateway check — suspended students cannot send messages
        if (!currentUser.canSendMessage()) {
            System.out.println("  [!] Your account is SUSPENDED. You cannot send messages.");
            System.out.println("      Please contact an administrator to reinstate your account.");
            CLIHelper.pause(scanner);
            return;
        }

        // Show active users the current user can message
        try {
            List<Student> activeUsers = userManager.getAllActiveStudents();
            activeUsers.removeIf(s -> s.getUsername().equalsIgnoreCase(currentUser.getUsername()));

            if (activeUsers.isEmpty()) {
                System.out.println("  No other active students to message.");
                CLIHelper.pause(scanner);
                return;
            }

            System.out.println("  Active students:");
            for (int i = 0; i < activeUsers.size(); i++) {
                System.out.printf("  [%d] %s (%s)%n",
                        i + 1,
                        activeUsers.get(i).getUsername(),
                        activeUsers.get(i).getMajor().isBlank() ? "no major" : activeUsers.get(i).getMajor());
            }
            System.out.printf("  [%d] (Enter a username manually)%n", activeUsers.size() + 1);
            CLIHelper.printSeparator();

            int choice = CLIHelper.readInt(scanner, 1, activeUsers.size() + 1);
            String recipient;
            if (choice <= activeUsers.size()) {
                recipient = activeUsers.get(choice - 1).getUsername();
            } else {
                recipient = CLIHelper.readNonEmpty(scanner, "Enter recipient username: ");
            }

            // Validate recipient exists
            if (!userManager.usernameExists(recipient)) {
                System.out.println("  [!] User '" + recipient + "' does not exist.");
                CLIHelper.pause(scanner);
                return;
            }

            if (recipient.equalsIgnoreCase(currentUser.getUsername())) {
                System.out.println("  [!] You cannot send a message to yourself.");
                CLIHelper.pause(scanner);
                return;
            }

            System.out.println("\n  Composing message to: " + recipient);
            System.out.println("  (Note: Avoid commas in your message)");
            String content = CLIHelper.readNonEmpty(scanner, "Message: ");

            // Replace commas to protect CSV integrity
            content = content.replace(",", " ");

            // OBSERVER PATTERN: sendMessage() persists to CSV and notifies recipient
            messageManager.sendMessage(currentUser.getUsername(), recipient, content);
            System.out.println("\n  [OK] Message sent to " + recipient + ".");

        } catch (IOException e) {
            System.out.println("  [ERROR] Could not send message: " + e.getMessage());
        }

        CLIHelper.pause(scanner);
    }
}
