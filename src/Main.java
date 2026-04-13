import command.*;
import manager.GroupManager;
import manager.MessageManager;
import manager.ReportManager;
import manager.UserManager;
import model.Student;
import server.ApiServer;
import ui.CLIHelper;
import ui.MenuBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * UniLink — Study Partner Finding System
 * Entry point and application controller.
 *
 * Responsibilities:
 *  1. Bootstrap: create data/ directory and CSV files on first run.
 *  2. Wire all Singletons together (SINGLETON PATTERN).
 *  3. Run the auth menu loop (Register / Login / Exit).
 *  4. On login: build the main-menu Command list (COMMAND PATTERN)
 *     and register the student as an Observer (OBSERVER PATTERN).
 *  5. Main menu loop: drain notifications at the top of each iteration
 *     so the student sees "[!]" alerts asynchronously (OBSERVER PATTERN).
 *  6. On logout: remove the Observer registration and return to auth menu.
 */
public class Main {

    public static void main(String[] args) {
        // ── Web mode: java -cp out Main --web ─────────────────────────────────
        if (args.length > 0 && args[0].equals("--web")) {
            try {
                new File("data").mkdirs();
                UserManager.getInstance().initialise();
                MessageManager.getInstance().initialise();
                GroupManager.getInstance().initialise();
                ReportManager.getInstance().initialise();

                new ApiServer(8080).start();
                System.out.println("  Press Ctrl+C to stop.");
                Thread.currentThread().join(); // keep JVM alive
            } catch (Exception e) {
                System.err.println("[FATAL] Web server error: " + e.getMessage());
            }
            return;
        }

        Scanner scanner = new Scanner(System.in);

        // ── 1. Bootstrap ──────────────────────────────────────────────────────
        try {
            // Ensure the data/ directory exists
            new File("data").mkdirs();

            // SINGLETON PATTERN: obtain the single shared manager instances
            UserManager    userManager    = UserManager.getInstance();
            MessageManager messageManager = MessageManager.getInstance();
            GroupManager   groupManager   = GroupManager.getInstance();

            // Initialise CSV files with headers + seed data if they don't exist
            userManager.initialise();
            messageManager.initialise();
            groupManager.initialise();

            // ── 2. Launch ─────────────────────────────────────────────────────
            CLIHelper.printLogo();
            System.out.println("  Welcome to UniLink — Find Your Perfect Study Partner!");
            System.out.println("  Default admin login: admin / admin123");

            // ── 3. Auth menu loop ─────────────────────────────────────────────
            boolean appRunning = true;
            while (appRunning) {
                CLIHelper.printBanner("UniLink — Main Menu");
                System.out.println("  [1] Login");
                System.out.println("  [2] Register");
                System.out.println("  [3] Exit");
                CLIHelper.printSeparator();

                int authChoice = CLIHelper.readInt(scanner, 1, 3);

                switch (authChoice) {
                    case 1 -> {
                        // LoginCommand will call the onLoginSuccess callback on success,
                        // which in turn runs the main-menu loop for the logged-in student.
                        Command loginCmd = new LoginCommand(
                                userManager, messageManager, scanner,
                                student -> runMainMenu(student, userManager, messageManager,
                                                       groupManager, scanner)
                        );
                        loginCmd.execute();
                    }
                    case 2 -> new RegisterCommand(userManager, scanner).execute();
                    case 3 -> {
                        System.out.println("\n  Thank you for using UniLink. Goodbye!");
                        appRunning = false;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("[FATAL] Initialisation error: " + e.getMessage());
            System.err.println("Please ensure the application has write permissions in the current directory.");
        }

        scanner.close();
    }

    // ── Main menu (logged-in) ─────────────────────────────────────────────────

    /**
     * Run the main menu loop for an authenticated student.
     * Called by LoginCommand via the onLoginSuccess callback.
     *
     * OBSERVER PATTERN: student.drainNotifications() is called at the top of
     * each iteration. Any notifications queued by MessageManager.notifyObservers()
     * (e.g., "You have a new message") are printed here — this is the
     * asynchronous delivery point.
     *
     * COMMAND PATTERN: All menu actions are encapsulated as Command objects.
     * Adding a new feature = adding a new Command class + one line in commands list.
     */
    private static void runMainMenu(Student student,
                                    UserManager userManager,
                                    MessageManager messageManager,
                                    GroupManager groupManager,
                                    Scanner scanner) {
        // Shared logout flag — set to true by LogoutCommand to exit the loop
        boolean[] loggedIn = {true};

        // Build the Command list for this user's session
        // COMMAND PATTERN: each entry is a self-contained Command object
        List<Command> mainCommands = new ArrayList<>();
        mainCommands.add(new ViewProfileCommand(student, scanner));
        mainCommands.add(new EditProfileCommand(student, userManager, scanner));
        mainCommands.add(new FindPartnersCommand(student, userManager, scanner));
        mainCommands.add(new SendMessageCommand(student, messageManager, userManager, scanner));
        mainCommands.add(new ReadMessagesCommand(student, messageManager, userManager, scanner));
        mainCommands.add(new CreateGroupCommand(student, groupManager, scanner));

        // Admin panel only visible to admin accounts
        if (student.isAdmin()) {
            mainCommands.add(new AdminPanelCommand(student, userManager, scanner));
        }

        mainCommands.add(new LogoutCommand(student, messageManager, scanner,
                () -> loggedIn[0] = false));

        // ── Main menu loop ────────────────────────────────────────────────────
        while (loggedIn[0]) {
            // OBSERVER PATTERN: drain any pending notifications before showing menu.
            // Notifications were enqueued by Student.update() when MessageManager
            // called notifyObservers() — this is where they surface to the user.
            student.drainNotifications();

            // COMMAND PATTERN: MenuBuilder is the Invoker — it calls execute()
            MenuBuilder.showMenu(
                "UniLink — " + student.getUsername() +
                " [" + student.getStateName() + "]",
                mainCommands, scanner
            );
        }
    }
}
