package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Value object representing a single chat message.
 * Provides toCsvRow() / fromCsvRow() for File I/O serialisation.
 *
 * CSV format: sender,receiver,timestamp,content,read
 * Example:    alice,bob,2026-04-12T10:30:00,Hey want to study?,false
 *
 * Note: content may not contain commas — users are warned in the UI layer.
 */
public class Message {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final String sender;
    private final String receiver;
    private final LocalDateTime timestamp;
    private final String content;
    private boolean read;

    public Message(String sender, String receiver, LocalDateTime timestamp,
                   String content, boolean read) {
        this.sender    = sender;
        this.receiver  = receiver;
        this.timestamp = timestamp;
        this.content   = content;
        this.read      = read;
    }

    // ── Serialisation ─────────────────────────────────────────────────────────

    /** Serialise this message to a CSV row (no newline appended). */
    public String toCsvRow() {
        return sender + "," + receiver + "," +
               timestamp.format(FORMATTER) + "," + content + "," + read;
    }

    /**
     * Deserialise a CSV row back into a Message.
     * Splits on the first 4 commas only so the content field may contain
     * internal commas if needed (limit=5).
     */
    public static Message fromCsvRow(String line) {
        String[] parts = line.split(",", 5);
        String sender    = parts[0].trim();
        String receiver  = parts[1].trim();
        LocalDateTime ts = LocalDateTime.parse(parts[2].trim(), FORMATTER);
        String content   = parts[3].trim();
        boolean read     = Boolean.parseBoolean(parts[4].trim());
        return new Message(sender, receiver, ts, content, read);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getSender()        { return sender; }
    public String getReceiver()      { return receiver; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getContent()       { return content; }
    public boolean isRead()          { return read; }
    public void setRead(boolean read){ this.read = read; }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
