package command;

import manager.MessageManager;
import manager.UserManager;
import model.Message;
import model.Student;
import ui.CLIHelper;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * COMMAND PATTERN — Read chat history with another student.
 * Retrieves conversation rows from messages.csv via MessageManager (File I/O).
 * Marks messages as read after viewing.
 */
public class ReadMessagesCommand implements Command {

    private final Student        currentUser;
    private final MessageManager messageManager;
    private final UserManager    userManager;
    private final Scanner        scanner;

    public ReadMessagesCommand(Student currentUser, MessageManager messageManager,
                               UserManager userManager, Scanner scanner) {
        this.currentUser    = currentUser;
        this.messageManager = messageManager;
        this.userManager    = userManager;
        this.scanner        = scanner;
    }

    @Override
    public String getLabel() { return "Read Messages"; }

    @Override
    public void execute() {
        CLIHelper.printBanner("Messages");

        try {
            // Show unread count
            List<Message> unread = messageManager.getUnreadForUser(currentUser.getUsername());
            if (!unread.isEmpty()) {
                System.out.println("  [!] You have " + unread.size() + " unread message(s).");
                CLIHelper.printSeparator();
            }

            // List users who have chatted with current user
            List<Student> allUsers = userManager.getAllUsers();
            allUsers.removeIf(s -> s.getUsername().equalsIgnoreCase(currentUser.getUsername()));

            if (allUsers.isEmpty()) {
                System.out.println("  No users to read messages from.");
                CLIHelper.pause(scanner);
                return;
            }

            System.out.println("  Select a user to view conversation:");
            for (int i = 0; i < allUsers.size(); i++) {
                System.out.printf("  [%d] %s%n", i + 1, allUsers.get(i).getUsername());
            }
            System.out.printf("  [%d] Cancel%n", allUsers.size() + 1);
            CLIHelper.printSeparator();

            int choice = CLIHelper.readInt(scanner, 1, allUsers.size() + 1);
            if (choice == allUsers.size() + 1) return;

            String partner = allUsers.get(choice - 1).getUsername();
            List<Message> conversation = messageManager.getConversation(
                    currentUser.getUsername(), partner);

            CLIHelper.printBanner("Conversation with " + partner);

            if (conversation.isEmpty()) {
                System.out.println("  No messages yet. Be the first to say hello!");
            } else {
                for (Message msg : conversation) {
                    String direction = msg.getSender().equalsIgnoreCase(currentUser.getUsername())
                            ? "You" : msg.getSender();
                    System.out.printf("  [%s] %s: %s%n",
                            msg.getFormattedTimestamp(), direction, msg.getContent());
                }
                // Mark all messages from this partner as read
                messageManager.markConversationAsRead(currentUser.getUsername(), partner);
            }

        } catch (IOException e) {
            System.out.println("  [ERROR] Could not read messages: " + e.getMessage());
        }

        CLIHelper.pause(scanner);
    }
}
