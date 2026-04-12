package command;

import manager.UserManager;
import model.Student;
import ui.CLIHelper;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * COMMAND PATTERN — Edit the current user's profile (major, strengths, weaknesses).
 *
 * STATE PATTERN in action: canUpdateProfile() is checked first.
 * Both NormalState and SuspendedState return true for this, but the
 * check is here to make the pattern explicit and demonstrate the contract.
 */
public class EditProfileCommand implements Command {

    private final Student     currentUser;
    private final UserManager userManager;
    private final Scanner     scanner;

    public EditProfileCommand(Student currentUser, UserManager userManager, Scanner scanner) {
        this.currentUser = currentUser;
        this.userManager = userManager;
        this.scanner     = scanner;
    }

    @Override
    public String getLabel() { return "Edit My Profile"; }

    @Override
    public void execute() {
        // STATE PATTERN: check before performing action
        if (!currentUser.canUpdateProfile()) {
            System.out.println("\n  [!] Your account status does not permit profile updates.");
            CLIHelper.pause(scanner);
            return;
        }

        CLIHelper.printBanner("Edit Profile");
        System.out.println("  (Press Enter to keep current value)");
        CLIHelper.printSeparator();

        // Major
        System.out.printf("  Current Major     : %s%n", currentUser.getMajor());
        String major = CLIHelper.readOptional(scanner,
                "New Major (or Enter to keep): ");
        if (!major.isBlank()) {
            currentUser.setMajor(major);
        }

        // Strengths
        System.out.printf("  Current Strengths : %s%n",
                currentUser.getStrengths().isEmpty() ? "(none)" :
                        String.join(", ", currentUser.getStrengths()));
        List<String> strengths = CLIHelper.readSemicolonList(scanner,
                "New Strengths (or Enter to keep)");
        if (!strengths.isEmpty()) {
            currentUser.setStrengths(strengths);
        }

        // Weaknesses
        System.out.printf("  Current Weaknesses: %s%n",
                currentUser.getWeaknesses().isEmpty() ? "(none)" :
                        String.join(", ", currentUser.getWeaknesses()));
        List<String> weaknesses = CLIHelper.readSemicolonList(scanner,
                "New Weaknesses (or Enter to keep)");
        if (!weaknesses.isEmpty()) {
            currentUser.setWeaknesses(weaknesses);
        }

        // Persist changes to CSV
        try {
            userManager.updateProfile(currentUser);
            System.out.println("\n  [OK] Profile updated successfully.");
        } catch (IOException e) {
            System.out.println("  [ERROR] Could not save profile: " + e.getMessage());
        }
        CLIHelper.pause(scanner);
    }
}
