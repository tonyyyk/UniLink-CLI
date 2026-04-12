package manager;

import model.Message;
import observer.Observer;
import observer.Subject;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SINGLETON PATTERN + OBSERVER PATTERN (Subject) — MessageManager.
 *
 * This is the most design-pattern-rich class in the application:
 *
 * 1. SINGLETON: Only one instance manages messages.csv, preventing
 *    concurrent write conflicts and providing a single access point.
 *
 * 2. OBSERVER (Subject): When sendMessage() appends a row to the CSV,
 *    it immediately calls notifyObservers() to push a notification to the
 *    recipient Student (Observer) if they are currently logged in.
 *    If the recipient is offline, the notification is simply skipped —
 *    they can read their messages manually via ReadMessagesCommand.
 *    This models true asynchronous delivery.
 *
 * CSV format (messages.csv):
 *   sender,receiver,timestamp,content,read
 */
public class MessageManager implements Subject {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static MessageManager instance;
    static final String FILE_PATH = "data/messages.csv";
    private static final String CSV_HEADER = "sender,receiver,timestamp,content,read";

    private MessageManager() {}

    public static MessageManager getInstance() {
        if (instance == null) {
            instance = new MessageManager();
        }
        return instance;
    }

    // ── OBSERVER PATTERN — observer registry ──────────────────────────────────

    /**
     * Maps username → Observer (Student) for currently logged-in users.
     * Only students who are logged in are registered here. When they log
     * out, they are removed. This is why delivery is "asynchronous" —
     * if the recipient is not in this map, they miss the live notification
     * but can still read the persisted message from the CSV.
     */
    private final Map<String, Observer> registeredObservers = new HashMap<>();

    @Override
    public void registerObserver(Observer o) {
        registeredObservers.put(o.getUsername(), o);
    }

    @Override
    public void removeObserver(Observer o) {
        registeredObservers.remove(o.getUsername());
    }

    /**
     * Notify the recipient if they are currently logged in.
     * This is the "push" half of the Observer pattern.
     */
    @Override
    public void notifyObservers(String recipientUsername, String notification) {
        Observer target = registeredObservers.get(recipientUsername);
        if (target != null) {
            target.update(notification); // enqueued in Student.pendingNotifications
        }
        // If target == null, the recipient is offline — no action needed.
        // The message is already persisted to the CSV so they can read it later.
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

    // ── Business methods ──────────────────────────────────────────────────────

    /**
     * Send a message from one user to another.
     * 1. Appends a new row to messages.csv (File I/O).
     * 2. Calls notifyObservers() — OBSERVER PATTERN push notification.
     *
     * @param from    sender's username
     * @param to      recipient's username
     * @param content message body
     */
    public void sendMessage(String from, String to, String content)
            throws IOException {
        Message msg = new Message(from, to, LocalDateTime.now(), content, false);

        // File I/O: append the new message row to messages.csv
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            bw.write(msg.toCsvRow());
            bw.newLine();
        }

        // OBSERVER PATTERN: notify the recipient (if currently logged in)
        notifyObservers(to, "You have a new message from " + from + "!");
    }

    /**
     * Retrieve the full conversation between two users, sorted oldest-first.
     * Used by ReadMessagesCommand to display chat history.
     */
    public List<Message> getConversation(String user1, String user2)
            throws IOException {
        List<Message> result = new ArrayList<>();
        for (Message msg : readAllMessages()) {
            boolean match =
                (msg.getSender().equalsIgnoreCase(user1) &&
                 msg.getReceiver().equalsIgnoreCase(user2)) ||
                (msg.getSender().equalsIgnoreCase(user2) &&
                 msg.getReceiver().equalsIgnoreCase(user1));
            if (match) result.add(msg);
        }
        // Already in insertion (chronological) order since we append to the file
        return result;
    }

    /**
     * Returns all unread messages addressed to the given username.
     * Used to show the unread count in the menu and notifications.
     */
    public List<Message> getUnreadForUser(String username) throws IOException {
        List<Message> result = new ArrayList<>();
        for (Message msg : readAllMessages()) {
            if (msg.getReceiver().equalsIgnoreCase(username) && !msg.isRead()) {
                result.add(msg);
            }
        }
        return result;
    }

    /**
     * Mark all messages in a conversation as read.
     * Rewrites the entire CSV updating the 'read' flag for matching rows.
     */
    public void markConversationAsRead(String reader, String sender)
            throws IOException {
        List<String> lines = readRawLines();
        List<String> updated = new ArrayList<>();
        updated.add(CSV_HEADER);

        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("sender")) continue;
            Message msg = Message.fromCsvRow(line);
            if (msg.getSender().equalsIgnoreCase(sender) &&
                msg.getReceiver().equalsIgnoreCase(reader) && !msg.isRead()) {
                msg.setRead(true);
            }
            updated.add(msg.toCsvRow());
        }
        writeLines(updated);
    }

    // ── Private CSV I/O helpers ───────────────────────────────────────────────

    private List<Message> readAllMessages() throws IOException {
        List<Message> messages = new ArrayList<>();
        List<String> lines = readRawLines();
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("sender")) continue;
            messages.add(Message.fromCsvRow(line));
        }
        return messages;
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

    private void writeLines(List<String> lines) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, false))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
    }
}
