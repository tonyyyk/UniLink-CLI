package manager;

import model.GroupMessage;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SINGLETON PATTERN — GroupMessageManager.
 *
 * Manages group chat message persistence via group_messages.csv.
 * Mirrors the structure of MessageManager but scoped to group conversations.
 *
 * CSV format (group_messages.csv):
 *   groupId,sender,timestamp,content
 */
public class GroupMessageManager {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static GroupMessageManager instance;
    static final String FILE_PATH = "data/group_messages.csv";

    private GroupMessageManager() {}

    public static GroupMessageManager getInstance() {
        if (instance == null) {
            instance = new GroupMessageManager();
        }
        return instance;
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    public void initialise() throws IOException {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            file.createNewFile(); // empty file — no header needed, data only
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Append a new message to the group chat.
     */
    public void sendMessage(int groupId, String sender, String content) throws IOException {
        GroupMessage msg = new GroupMessage(groupId, sender, LocalDateTime.now(), content);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            bw.write(msg.toCsvRow());
            bw.newLine();
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Return all messages for the given group, in chronological order.
     */
    public List<GroupMessage> getMessages(int groupId) throws IOException {
        List<GroupMessage> result = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return result;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    GroupMessage msg = GroupMessage.fromCsvRow(line);
                    if (msg.getGroupId() == groupId) result.add(msg);
                } catch (Exception ignored) {
                    // skip malformed rows
                }
            }
        }
        return result;
    }
}
