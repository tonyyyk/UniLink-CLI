package ui;

import command.Command;

import java.util.List;
import java.util.Scanner;

/**
 * Utility class that renders a numbered CLI menu from a List of Command objects
 * and dispatches execute() on the chosen entry.
 *
 * COMMAND PATTERN: This is the "Invoker" — it knows nothing about what the
 * commands do; it just calls execute() on whichever the user selects.
 * To add a new menu option, create a new Command class and add it to the list
 * passed into showMenu(). The MenuBuilder never changes.
 */
public class MenuBuilder {

    private MenuBuilder() {} // utility class

    /**
     * Display a numbered menu and execute the chosen command.
     * Does not loop — caller is responsible for looping if needed.
     *
     * @param title    banner title shown at the top
     * @param commands ordered list of commands (indexed 1..n)
     * @param sc       shared Scanner for user input
     */
    public static void showMenu(String title, List<Command> commands, Scanner sc) {
        CLIHelper.printBanner(title);
        for (int i = 0; i < commands.size(); i++) {
            System.out.printf("  [%d] %s%n", i + 1, commands.get(i).getLabel());
        }
        CLIHelper.printSeparator();
        int choice = CLIHelper.readInt(sc, 1, commands.size());
        commands.get(choice - 1).execute();
    }
}
