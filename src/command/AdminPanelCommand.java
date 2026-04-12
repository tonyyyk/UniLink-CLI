package command;

import manager.UserManager;
import model.Student;
import ui.CLIHelper;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * COMMAND PATTERN — Admin control panel.
 *
 * STATE PATTERN integration: This command triggers state transitions.
 * When an admin suspends a user, the STATE changes from NormalState →
 * SuspendedState, and all behaviour gates (canSendMessage, canAppearInSearch)
 * automatically return the correct values without any if-checks in business code.
 *
 * Only accessible to accounts with role = "ADMIN".
 * The default admin credentials are: admin / admin123
 */
public class AdminPanelCommand implements Command {

    private final Student     currentAdmin;
    private final UserManager userManager;
    private final Scanner     scanner;

    public AdminPanelCommand(Student currentAdmin, UserManager userManager, Scanner scanner) {
        this.currentAdmin = currentAdmin;
        this.userManager  = userManager;
        this.scanner      = scanner;
    }

    @Override
    public String getLabel() { return "[ADMIN] Admin Panel"; }

    @Override
    public void execute() {
        if (!currentAdmin.isAdmin()) {
            System.out.println("  [!] Access denied. Admin only.");
            return;
        }

        boolean running = true;
        while (running) {
            CLIHelper.printBanner("Admin Panel");
            System.out.println("  [1] View all users");
            System.out.println("  [2] Suspend a user");
            System.out.println("  [3] Reinstate a user");
            System.out.println("  [4] Back");
            CLIHelper.printSeparator();

            int choice = CLIHelper.readInt(scanner, 1, 4);
            switch (choice) {
                case 1 -> listAllUsers();
                case 2 -> changeUserStatus(false);
                case 3 -> changeUserStatus(true);
                case 4 -> running = false;
            }
        }
    }

    private void listAllUsers() {
        CLIHelper.printBanner("All Users");
        try {
            List<Student> users = userManager.getAllUsers();
            System.out.printf("  %-20s %-15s %-10s %-10s%n",
                    "Username", "Major", "Role", "Status");
            CLIHelper.printSeparator();
            for (Student s : users) {
                System.out.printf("  %-20s %-15s %-10s %-10s%n",
                        s.getUsername(),
                        s.getMajor().isBlank() || s.getMajor().equals("-") ? "-" : s.getMajor(),
                        s.getRole(),
                        s.getStateName());
            }
            System.out.println("\n  Total: " + users.size() + " user(s).");
        } catch (IOException e) {
            System.out.println("  [ERROR] " + e.getMessage());
        }
        CLIHelper.pause(scanner);
    }

    /**
     * Suspend or reinstate a user account.
     * STATE PATTERN: calls UserManager.suspendUser() / reinstateUser()
     * which rewrites the status field in users.csv. On next login the
     * correct StudentState is reconstructed from that persisted value.
     *
     * @param reinstate true to reinstate, false to suspend
     */
    private void changeUserStatus(boolean reinstate) {
        String action = reinstate ? "Reinstate" : "Suspend";
        CLIHelper.printBanner(action + " User");

        try {
            List<Student> users = userManager.getAllUsers();
            users.removeIf(s -> s.isAdmin()); // admins cannot be suspended

            if (users.isEmpty()) {
                System.out.println("  No non-admin users found.");
                CLIHelper.pause(scanner);
                return;
            }

            // Show eligible users
            for (int i = 0; i < users.size(); i++) {
                System.out.printf("  [%d] %-20s [%s]%n",
                        i + 1, users.get(i).getUsername(), users.get(i).getStateName());
            }
            System.out.printf("  [%d] Cancel%n", users.size() + 1);
            CLIHelper.printSeparator();

            int choice = CLIHelper.readInt(scanner, 1, users.size() + 1);
            if (choice == users.size() + 1) return;

            Student target = users.get(choice - 1);

            // Guard: can't re-suspend an already-suspended user, etc.
            if (!reinstate && target.getStateName().equals("SUSPENDED")) {
                System.out.println("  [!] " + target.getUsername() + " is already suspended.");
                CLIHelper.pause(scanner);
                return;
            }
            if (reinstate && target.getStateName().equals("NORMAL")) {
                System.out.println("  [!] " + target.getUsername() + " is not suspended.");
                CLIHelper.pause(scanner);
                return;
            }

            // STATE PATTERN: trigger the transition in the CSV
            if (reinstate) {
                userManager.reinstateUser(target.getUsername());
                System.out.println("\n  [OK] " + target.getUsername() + " has been REINSTATED (NormalState).");
            } else {
                userManager.suspendUser(target.getUsername());
                System.out.println("\n  [OK] " + target.getUsername() + " has been SUSPENDED (SuspendedState).");
                System.out.println("       They can no longer send messages or appear in searches.");
            }

        } catch (IOException e) {
            System.out.println("  [ERROR] " + e.getMessage());
        }
        CLIHelper.pause(scanner);
    }
}
