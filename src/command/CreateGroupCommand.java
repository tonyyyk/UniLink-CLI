package command;

import manager.GroupManager;
import model.Student;
import model.StudyGroup;
import ui.CLIHelper;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * COMMAND PATTERN — Study Group management menu.
 *
 * Demonstrates the Singleton pattern via GroupManager.getInstance() and
 * provides a self-contained sub-menu for creating, browsing, and joining groups.
 * This additional feature gives the application genuine collaborative utility
 * beyond one-on-one matching.
 */
public class CreateGroupCommand implements Command {

    private final Student      currentUser;
    private final GroupManager groupManager;
    private final Scanner      scanner;

    public CreateGroupCommand(Student currentUser, GroupManager groupManager, Scanner scanner) {
        this.currentUser  = currentUser;
        this.groupManager = groupManager;
        this.scanner      = scanner;
    }

    @Override
    public String getLabel() { return "Study Groups"; }

    @Override
    public void execute() {
        boolean running = true;
        while (running) {
            CLIHelper.printBanner("Study Groups");
            System.out.println("  [1] Create a new group");
            System.out.println("  [2] Browse all groups");
            System.out.println("  [3] My groups");
            System.out.println("  [4] Join a group");
            System.out.println("  [5] Back");
            CLIHelper.printSeparator();

            int choice = CLIHelper.readInt(scanner, 1, 5);
            switch (choice) {
                case 1: createGroup();    break;
                case 2: browseGroups();   break;
                case 3: myGroups();       break;
                case 4: joinGroup();      break;
                case 5: running = false;  break;
            }
        }
    }

    private void createGroup() {
        CLIHelper.printBanner("Create Study Group");
        String name  = CLIHelper.readNonEmpty(scanner, "Group name: ");
        String topic = CLIHelper.readNonEmpty(scanner, "Topic/subject: ");

        try {
            int id = groupManager.createGroup(name, currentUser.getUsername(), topic);
            System.out.println("\n  [OK] Group \"" + name + "\" created with ID #" + id + ".");
        } catch (IOException e) {
            System.out.println("  [ERROR] " + e.getMessage());
        }
        CLIHelper.pause(scanner);
    }

    private void browseGroups() {
        CLIHelper.printBanner("All Study Groups");
        try {
            List<StudyGroup> groups = groupManager.getAllGroups();
            if (groups.isEmpty()) {
                System.out.println("  No groups exist yet. Be the first to create one!");
            } else {
                System.out.printf("  %-4s %-20s %-15s %-20s %s%n",
                        "ID", "Name", "Creator", "Topic", "Members");
                CLIHelper.printSeparator();
                for (StudyGroup g : groups) {
                    System.out.printf("  %-4d %-20s %-15s %-20s %s%n",
                            g.getGroupId(), g.getGroupName(), g.getCreator(),
                            g.getTopic(), String.join(", ", g.getMembers()));
                }
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] " + e.getMessage());
        }
        CLIHelper.pause(scanner);
    }

    private void myGroups() {
        CLIHelper.printBanner("My Groups");
        try {
            List<StudyGroup> groups = groupManager.getGroupsForUser(currentUser.getUsername());
            if (groups.isEmpty()) {
                System.out.println("  You are not a member of any group yet.");
            } else {
                for (StudyGroup g : groups) {
                    System.out.printf("  #%d — %s | Topic: %s | Members: %s%n",
                            g.getGroupId(), g.getGroupName(), g.getTopic(),
                            String.join(", ", g.getMembers()));
                }
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] " + e.getMessage());
        }
        CLIHelper.pause(scanner);
    }

    private void joinGroup() {
        CLIHelper.printBanner("Join a Group");
        try {
            List<StudyGroup> groups = groupManager.getAllGroups();
            if (groups.isEmpty()) {
                System.out.println("  No groups available to join.");
                CLIHelper.pause(scanner);
                return;
            }
            System.out.printf("  %-4s %-20s %-15s%n", "ID", "Name", "Topic");
            CLIHelper.printSeparator();
            for (StudyGroup g : groups) {
                String memberTag = g.hasMember(currentUser.getUsername()) ? " (joined)" : "";
                System.out.printf("  %-4d %-20s %-15s%s%n",
                        g.getGroupId(), g.getGroupName(), g.getTopic(), memberTag);
            }
            CLIHelper.printSeparator();

            System.out.print("  Enter group ID to join (0 to cancel): ");
            String input = scanner.nextLine().trim();
            int id;
            try {
                id = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input.");
                CLIHelper.pause(scanner);
                return;
            }
            if (id == 0) return;

            boolean success = groupManager.joinGroup(id, currentUser.getUsername());
            if (success) {
                System.out.println("  [OK] You have joined the group.");
            } else {
                System.out.println("  [!] Group #" + id + " not found.");
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] " + e.getMessage());
        }
        CLIHelper.pause(scanner);
    }
}
