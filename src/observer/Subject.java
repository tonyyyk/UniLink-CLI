package observer;

/**
 * OBSERVER PATTERN — Subject interface.
 *
 * A Subject maintains a list of observers and broadcasts notifications
 * to them when relevant events happen. MessageManager implements this
 * interface and notifies the recipient Student whenever a new message
 * is written to messages.csv.
 */
public interface Subject {
    /** Register an observer to receive future notifications. */
    void registerObserver(Observer o);

    /** Unregister an observer (e.g., on logout). */
    void removeObserver(Observer o);

    /**
     * Push a notification to the specific registered observer identified
     * by recipientUsername. If no matching observer is registered (e.g.,
     * the recipient is offline), the notification is silently skipped —
     * the recipient will read messages manually via ReadMessagesCommand.
     *
     * @param recipientUsername the username of the target observer
     * @param notification      the message to deliver
     */
    void notifyObservers(String recipientUsername, String notification);
}
