package ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Static utility class providing common CLI input/output helpers.
 * Keeps all formatting and input-validation logic in one place so
 * Command classes stay focused on business logic.
 */
public class CLIHelper {

    private CLIHelper() {} // utility class — not instantiated

    // ── Output helpers ────────────────────────────────────────────────────────

    /** Print a prominent title banner surrounded by dashes. */
    public static void printBanner(String title) {
        String border = "=".repeat(50);
        System.out.println("\n" + border);
        System.out.printf("  %s%n", title);
        System.out.println(border);
    }

    /** Print a thin separator line. */
    public static void printSeparator() {
        System.out.println("-".repeat(50));
    }

    /** Print the UniLink welcome logo on startup. */
    public static void printLogo() {
        System.out.println();
        System.out.println("  ██╗   ██╗███╗   ██╗██╗██╗     ██╗███╗   ██╗██╗  ██╗");
        System.out.println("  ██║   ██║████╗  ██║██║██║     ██║████╗  ██║██║ ██╔╝");
        System.out.println("  ██║   ██║██╔██╗ ██║██║██║     ██║██╔██╗ ██║█████╔╝ ");
        System.out.println("  ██║   ██║██║╚██╗██║██║██║     ██║██║╚██╗██║██╔═██╗ ");
        System.out.println("  ╚██████╔╝██║ ╚████║██║███████╗██║██║ ╚████║██║  ██╗");
        System.out.println("   ╚═════╝ ╚═╝  ╚═══╝╚═╝╚══════╝╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝");
        System.out.println();
        System.out.println("        Find Your Perfect Study Partner");
        System.out.println();
    }

    // ── Input helpers ─────────────────────────────────────────────────────────

    /**
     * Read an integer from the user in the range [min, max] (inclusive).
     * Loops until valid input is entered.
     */
    public static int readInt(Scanner sc, int min, int max) {
        while (true) {
            System.out.print("  Enter choice [" + min + "-" + max + "]: ");
            String line = sc.nextLine().trim();
            try {
                int val = Integer.parseInt(line);
                if (val >= min && val <= max) return val;
                System.out.println("  Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter a number.");
            }
        }
    }

    /**
     * Prompt the user and read a non-empty string.
     * Loops until the user provides non-blank input.
     */
    public static String readNonEmpty(Scanner sc, String prompt) {
        while (true) {
            System.out.print("  " + prompt);
            String line = sc.nextLine().trim();
            if (!line.isEmpty()) return line;
            System.out.println("  Input cannot be empty. Please try again.");
        }
    }

    /**
     * Read an optional string (may be empty — returns empty string if blank).
     */
    public static String readOptional(Scanner sc, String prompt) {
        System.out.print("  " + prompt);
        return sc.nextLine().trim();
    }

    /**
     * Read a semicolon-delimited list of skill names.
     * Example input: "Java;Algorithms;Networks"
     * Returns an empty list if the user just presses Enter.
     *
     * @param prompt the label displayed before the input field
     */
    public static List<String> readSemicolonList(Scanner sc, String prompt) {
        System.out.print("  " + prompt + " (semicolon-separated, e.g. Java;Algorithms): ");
        String line = sc.nextLine().trim();
        if (line.isEmpty()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String item : line.split(";")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    /**
     * Print a "Press Enter to continue..." prompt and wait for the user.
     * Used after displaying a result screen so the user can read it.
     */
    public static void pause(Scanner sc) {
        System.out.print("\n  Press Enter to continue...");
        sc.nextLine();
    }

    /**
     * Read a yes/no answer from the user.
     * Accepts: y / yes / n / no (case-insensitive).
     * Returns true for yes, false for no.
     */
    public static boolean readYesNo(Scanner sc, String prompt) {
        while (true) {
            System.out.print("  " + prompt + " [y/n]: ");
            String input = sc.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) return true;
            if (input.equals("n") || input.equals("no"))  return false;
            System.out.println("  Please enter 'y' or 'n'.");
        }
    }
}
