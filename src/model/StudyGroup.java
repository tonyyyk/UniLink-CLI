package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Value object representing a study group.
 * Provides toCsvRow() / fromCsvRow() for File I/O serialisation.
 *
 * CSV format: groupId,groupName,creator,members,topic
 * Example:    1,AlgoStudy,alice,"alice;bob;carol",Algorithms
 *
 * Members list is semicolon-delimited within the field.
 */
public class StudyGroup {

    private final int    groupId;
    private final String groupName;
    private final String creator;
    private List<String> members;
    private final String topic;

    public StudyGroup(int groupId, String groupName, String creator,
                      List<String> members, String topic) {
        this.groupId   = groupId;
        this.groupName = groupName;
        this.creator   = creator;
        this.members   = new ArrayList<>(members);
        this.topic     = topic;
    }

    // ── Serialisation ─────────────────────────────────────────────────────────

    /** Serialise to a CSV row. The members field is quoted to handle semicolons. */
    public String toCsvRow() {
        String membersField = String.join(";", members);
        return groupId + "," + groupName + "," + creator + ",\"" +
               membersField + "\"," + topic;
    }

    /**
     * Deserialise a CSV row back into a StudyGroup.
     * Handles the quoted members field by stripping surrounding quotes.
     */
    public static StudyGroup fromCsvRow(String line) {
        // Parse carefully: groupId,groupName,creator,"m1;m2;m3",topic
        // We split on comma but must respect the quoted members field.
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        int    id      = Integer.parseInt(parts[0].trim());
        String name    = parts[1].trim();
        String creator = parts[2].trim();
        String membersRaw = parts[3].trim().replaceAll("^\"|\"$", "");
        List<String> members = new ArrayList<>();
        if (!membersRaw.isEmpty()) {
            members.addAll(Arrays.asList(membersRaw.split(";")));
        }
        String topic = parts[4].trim();
        return new StudyGroup(id, name, creator, members, topic);
    }

    // ── Mutation ──────────────────────────────────────────────────────────────

    public void addMember(String username) {
        if (!members.contains(username)) {
            members.add(username);
        }
    }

    public boolean hasMember(String username) {
        return members.contains(username);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int     getGroupId()   { return groupId; }
    public String  getGroupName() { return groupName; }
    public String  getCreator()   { return creator; }
    public List<String> getMembers() { return members; }
    public String  getTopic()     { return topic; }
}
