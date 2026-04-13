package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Value object representing a single group chat message.
 * Provides toCsvRow() / fromCsvRow() for File I/O serialisation.
 *
 * CSV format: groupId,sender,timestamp,content
 */
public class GroupMessage {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final int    groupId;
    private final String sender;
    private final LocalDateTime timestamp;
    private final String content;

    public GroupMessage(int groupId, String sender, LocalDateTime timestamp, String content) {
        this.groupId   = groupId;
        this.sender    = sender;
        this.timestamp = timestamp;
        this.content   = content;
    }

    /** Serialise to a CSV row (no newline). */
    public String toCsvRow() {
        return groupId + "," + sender + "," + timestamp.format(FORMATTER) + "," + content;
    }

    /**
     * Deserialise a CSV row back into a GroupMessage.
     * Splits on the first 3 commas; content may contain semicolons.
     */
    public static GroupMessage fromCsvRow(String line) {
        String[] parts = line.split(",", 4);
        int groupId       = Integer.parseInt(parts[0].trim());
        String sender     = parts[1].trim();
        LocalDateTime ts  = LocalDateTime.parse(parts[2].trim(), FORMATTER);
        String content    = parts[3].trim();
        return new GroupMessage(groupId, sender, ts, content);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int    getGroupId()   { return groupId; }
    public String getSender()    { return sender; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getContent()   { return content; }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
