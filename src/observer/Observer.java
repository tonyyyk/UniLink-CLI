package observer;

/**
 * OBSERVER PATTERN — Observer interface.
 *
 * Any object that wants to receive async notifications from a Subject
 * must implement this interface. In UniLink, Student implements Observer
 * so it can be notified when a new message arrives.
 */
public interface Observer {
    /** The unique identifier used to route notifications to this observer. */
    String getUsername();

    /**
     * Called by the Subject when an event relevant to this observer occurs.
     * The notification is queued internally and displayed the next time the
     * student returns to the main menu (asynchronous delivery).
     *
     * @param notification human-readable alert message
     */
    void update(String notification);
}
