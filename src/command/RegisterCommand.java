package command;

import manager.UserManager;
import ui.CLIHelper;

import java.io.IOException;
import java.util.Scanner;

/**
 * COMMAND PATTERN — Register a new user account.
 * Collects username, password, and major, then delegates to UserManager.
 */
public class RegisterCommand implements Command {

    private final UserManager userManager;
    private final Scanner scanner;

    public RegisterCommand(UserManager userManager, Scanner scanner) {
        this.userManager = userManager;
        this.scanner     = scanner;
    }

    @Override
    public String getLabel() { return "Register"; }

    @Override
    public void execute() {
        CLIHelper.printBanner("Register New Account");

        String username = CLIHelper.readNonEmpty(scanner, "Choose a username: ");
        try {
            if (userManager.usernameExists(username)) {
                System.out.println("  [!] Username '" + username + "' is already taken. Please try another.");
                CLIHelper.pause(scanner);
                return;
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] Could not check username: " + e.getMessage());
            return;
        }

        String password = CLIHelper.readNonEmpty(scanner, "Choose a password: ");
        String major    = CLIHelper.readNonEmpty(scanner, "Enter your major (e.g. CS, Maths, Physics): ");

        try {
            boolean success = userManager.register(username, password, major);
            if (success) {
                System.out.println("\n  [OK] Account created successfully! You can now log in.");
            } else {
                System.out.println("  [!] Registration failed. Please try again.");
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] Could not write to file: " + e.getMessage());
        }
        CLIHelper.pause(scanner);
    }
}
