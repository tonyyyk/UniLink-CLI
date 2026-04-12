package command;

import manager.MessageManager;
import manager.UserManager;
import model.Student;
import ui.CLIHelper;

import java.io.IOException;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * COMMAND PATTERN — Log in to an existing account.
 *
 * On success, registers the student as an OBSERVER with MessageManager
 * (OBSERVER PATTERN) and signals the main loop to enter the logged-in menu
 * via the onLoginSuccess callback.
 */
public class LoginCommand implements Command {

    private final UserManager     userManager;
    private final MessageManager  messageManager;
    private final Scanner         scanner;

    /**
     * Callback invoked with the authenticated Student on successful login.
     * Main.java uses this to hand control to the main-menu loop.
     */
    private final Consumer<Student> onLoginSuccess;

    public LoginCommand(UserManager userManager, MessageManager messageManager,
                        Scanner scanner, Consumer<Student> onLoginSuccess) {
        this.userManager    = userManager;
        this.messageManager = messageManager;
        this.scanner        = scanner;
        this.onLoginSuccess = onLoginSuccess;
    }

    @Override
    public String getLabel() { return "Login"; }

    @Override
    public void execute() {
        CLIHelper.printBanner("Login");

        String username = CLIHelper.readNonEmpty(scanner, "Username: ");
        String password = CLIHelper.readNonEmpty(scanner, "Password: ");

        try {
            Student student = userManager.login(username, password);
            if (student == null) {
                System.out.println("\n  [!] Invalid username or password. Please try again.");
                CLIHelper.pause(scanner);
                return;
            }

            System.out.println("\n  [OK] Welcome back, " + student.getUsername() + "!");

            if (student.getStateName().equals("SUSPENDED")) {
                System.out.println("  [!] Note: Your account is currently SUSPENDED.");
                System.out.println("      You may update your profile but cannot send messages or appear in searches.");
            }

            // OBSERVER PATTERN: register this student to receive live notifications
            messageManager.registerObserver(student);

            // Hand control to the main menu loop
            onLoginSuccess.accept(student);

        } catch (IOException e) {
            System.out.println("  [ERROR] Login failed: " + e.getMessage());
            CLIHelper.pause(scanner);
        }
    }
}
