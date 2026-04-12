package command;

import manager.UserManager;
import model.Student;
import strategy.ComplementaryMatchingStrategy;
import strategy.MatchingStrategy;
import strategy.SameMajorMatchingStrategy;
import ui.CLIHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * COMMAND PATTERN — Find compatible study partners.
 *
 * Integrates THREE design patterns:
 *
 * 1. COMMAND: This action is encapsulated as an object in the menu list.
 *
 * 2. STRATEGY: The user can switch the matching algorithm at runtime.
 *    Changing the algorithm does not require modifying this class —
 *    just swap the MatchingStrategy instance. This is the live demo of
 *    the Open/Closed Principle during the presentation.
 *
 * 3. STATE: getAllActiveStudents() filters out SUSPENDED students because
 *    SuspendedState.canAppearInSearch() returns false. The student entity
 *    itself decides whether it should be visible — not this command.
 */
public class FindPartnersCommand implements Command {

    private final Student     currentUser;
    private final UserManager userManager;
    private final Scanner     scanner;

    // STRATEGY PATTERN: holds the current algorithm; replaceable at runtime
    private MatchingStrategy matchingStrategy;

    // Available strategies for the selector menu
    private static final MatchingStrategy[] STRATEGIES = {
        new ComplementaryMatchingStrategy(),
        new SameMajorMatchingStrategy()
    };

    public FindPartnersCommand(Student currentUser, UserManager userManager, Scanner scanner) {
        this.currentUser      = currentUser;
        this.userManager      = userManager;
        this.scanner          = scanner;
        this.matchingStrategy = STRATEGIES[0]; // default: complementary skills
    }

    @Override
    public String getLabel() { return "Find Study Partners"; }

    @Override
    public void execute() {
        CLIHelper.printBanner("Find Study Partners");

        // STRATEGY PATTERN: let the user pick the algorithm
        System.out.println("  Current algorithm: " + matchingStrategy.getStrategyName());
        System.out.println("\n  [1] Use current algorithm");
        System.out.println("  [2] Switch to: " + getOtherStrategy().getStrategyName());
        int choice = CLIHelper.readInt(scanner, 1, 2);
        if (choice == 2) {
            matchingStrategy = getOtherStrategy();
            System.out.println("\n  Switched to: " + matchingStrategy.getStrategyName());
        }
        CLIHelper.printSeparator();

        try {
            // STATE PATTERN: getAllActiveStudents() excludes SUSPENDED users
            List<Student> candidates = userManager.getAllActiveStudents();

            // Remove self from the candidate list
            candidates.removeIf(s -> s.getUsername().equalsIgnoreCase(currentUser.getUsername()));

            if (candidates.isEmpty()) {
                System.out.println("  No other active students found in the system.");
                CLIHelper.pause(scanner);
                return;
            }

            // Compute scores and sort descending
            List<StudentScore> scored = new ArrayList<>();
            for (Student candidate : candidates) {
                double score = matchingStrategy.calculateScore(currentUser, candidate);
                scored.add(new StudentScore(candidate, score));
            }
            scored.sort(Comparator.comparingDouble(StudentScore::getScore).reversed());

            // Display results
            System.out.println("  Algorithm : " + matchingStrategy.getStrategyName());
            System.out.println("  Your Major: " + (currentUser.getMajor().isBlank() ? "(not set)" : currentUser.getMajor()));
            CLIHelper.printSeparator();
            System.out.printf("  %-20s %-15s %-15s %s%n",
                    "Username", "Major", "Score", "Status");
            CLIHelper.printSeparator();

            for (StudentScore ss : scored) {
                Student s = ss.getStudent();
                System.out.printf("  %-20s %-15s %-15s%n",
                        s.getUsername(),
                        s.getMajor().isBlank() ? "-" : s.getMajor(),
                        String.format("%.0f%%", ss.getScore() * 100));
            }

            CLIHelper.printSeparator();
            System.out.println("  Total matches found: " + scored.size());

        } catch (IOException e) {
            System.out.println("  [ERROR] Could not load students: " + e.getMessage());
        }

        CLIHelper.pause(scanner);
    }

    private MatchingStrategy getOtherStrategy() {
        return (matchingStrategy instanceof ComplementaryMatchingStrategy)
                ? STRATEGIES[1] : STRATEGIES[0];
    }

    /** Private helper: pairs a Student with their computed compatibility score. */
    private static class StudentScore {
        private final Student student;
        private final double  score;

        StudentScore(Student student, double score) {
            this.student = student;
            this.score   = score;
        }

        Student getStudent() { return student; }
        double  getScore()   { return score; }
    }
}
