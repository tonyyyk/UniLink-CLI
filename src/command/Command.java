package command;

/**
 * COMMAND PATTERN — Command interface.
 *
 * Every action available in the CLI menu is encapsulated as a Command object.
 * MenuBuilder builds a numbered list from a List<Command> and dispatches
 * execute() on the chosen entry. Adding a new menu option requires only a new
 * Command class — nothing in the menu loop ever changes (Open/Closed Principle).
 */
public interface Command {
    /** Label displayed in the numbered CLI menu for this action. */
    String getLabel();

    /** Runs the action. All Scanner I/O and business logic lives here. */
    void execute();
}
