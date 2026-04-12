package command;

import model.Student;
import ui.CLIHelper;

import java.util.Scanner;

/**
 * COMMAND PATTERN — Display the current user's profile.
 */
public class ViewProfileCommand implements Command {

    private final Student currentUser;
    private final Scanner scanner;

    public ViewProfileCommand(Student currentUser, Scanner scanner) {
        this.currentUser = currentUser;
        this.scanner     = scanner;
    }

    @Override
    public String getLabel() { return "View My Profile"; }

    @Override
    public void execute() {
        CLIHelper.printBanner("My Profile");

        System.out.printf("  Username  : %s%n", currentUser.getUsername());
        System.out.printf("  Major     : %s%n", currentUser.getMajor().isBlank() ? "(not set)" : currentUser.getMajor());
        System.out.printf("  Role      : %s%n", currentUser.getRole());
        System.out.printf("  Status    : %s%n", currentUser.getStateName());

        CLIHelper.printSeparator();

        String strengths = currentUser.getStrengths().isEmpty()
                ? "(none set)"
                : String.join(", ", currentUser.getStrengths());
        String weaknesses = currentUser.getWeaknesses().isEmpty()
                ? "(none set)"
                : String.join(", ", currentUser.getWeaknesses());

        System.out.printf("  Strengths : %s%n", strengths);
        System.out.printf("  Weaknesses: %s%n", weaknesses);

        CLIHelper.printSeparator();
        System.out.println("  [STATE: " + currentUser.getStateName() + "] " +
                "Can send messages: " + currentUser.canSendMessage() +
                " | Appears in search: " + currentUser.canAppearInSearch());

        CLIHelper.pause(scanner);
    }
}
