package manager;

import model.GroupMessage;
import model.StudyGroup;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SINGLETON PATTERN — GroupManager.
 *
 * Manages all study group data persistence via groups.csv.
 * A Singleton ensures exactly one instance owns the file handle,
 * demonstrating the pattern on a second concrete manager class.
 *
 * CSV format (groups.csv):
 *   groupId,groupName,creator,members,topic
 *   Members field is semicolon-delimited and double-quoted.
 */
public class GroupManager {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static GroupManager instance;
    static final String FILE_PATH = "data/groups.csv";
    private static final String CSV_HEADER = "groupId,groupName,creator,members,topic";

    private GroupManager() {}

    public static GroupManager getInstance() {
        if (instance == null) {
            instance = new GroupManager();
        }
        return instance;
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    public void initialise() throws IOException {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(CSV_HEADER);
                bw.newLine();
            }
        }
    }

    // ── CRUD operations ───────────────────────────────────────────────────────

    /**
     * Create a new study group. The creator is automatically added as the
     * first member. Returns the new group's auto-incremented ID.
     *
     * @param groupName name of the group
     * @param creator   username of the creating student
     * @param topic     academic topic of focus
     * @return the newly assigned group ID
     */
    public int createGroup(String groupName, String creator, String topic)
            throws IOException {
        int newId = getNextId();
        List<String> members = new ArrayList<>();
        members.add(creator);
        StudyGroup group = new StudyGroup(newId, groupName, creator, members, topic);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            bw.write(group.toCsvRow());
            bw.newLine();
        }
        return newId;
    }

    /**
     * Add a student to an existing group.
     * Silently does nothing if the student is already a member.
     * Returns false if the group ID does not exist.
     */
    public boolean joinGroup(int groupId, String username) throws IOException {
        List<StudyGroup> groups = getAllGroups();
        boolean found = false;

        for (StudyGroup g : groups) {
            if (g.getGroupId() == groupId) {
                if (g.hasMember(username)) return true; // already a member
                g.addMember(username);
                found = true;
                break;
            }
        }

        if (!found) return false;
        persistAll(groups);
        return true;
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    /** Returns all study groups. */
    public List<StudyGroup> getAllGroups() throws IOException {
        List<StudyGroup> groups = new ArrayList<>();
        List<String> lines = readRawLines();
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("groupId")) continue;
            groups.add(StudyGroup.fromCsvRow(line));
        }
        return groups;
    }

    /** Returns only the groups that include the given username as a member. */
    public List<StudyGroup> getGroupsForUser(String username) throws IOException {
        List<StudyGroup> result = new ArrayList<>();
        for (StudyGroup g : getAllGroups()) {
            if (g.hasMember(username)) result.add(g);
        }
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Calculate the next available group ID (max existing ID + 1). */
    private int getNextId() throws IOException {
        int max = 0;
        for (StudyGroup g : getAllGroups()) {
            if (g.getGroupId() > max) max = g.getGroupId();
        }
        return max + 1;
    }

    /** Rewrite the entire groups.csv from a list of StudyGroup objects. */
    private void persistAll(List<StudyGroup> groups) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, false))) {
            bw.write(CSV_HEADER);
            bw.newLine();
            for (StudyGroup g : groups) {
                bw.write(g.toCsvRow());
                bw.newLine();
            }
        }
    }

    private List<String> readRawLines() throws IOException {
        List<String> lines = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return lines;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(line);
            }
        }
        return lines;
    }
}
