package model;

import observer.Observer;
import state.NormalState;
import state.StudentState;
import state.SuspendedState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Core entity representing a UniLink user.
 *
 * Participates in TWO design patterns:
 *
 * 1. STATE PATTERN (as Context)
 *    All behaviour that changes with account status is delegated to the
 *    current StudentState instance (NormalState or SuspendedState).
 *    Student itself never inspects the state type — it just calls the
 *    interface methods. Swapping state is as simple as assigning a new object.
 *
 * 2. OBSERVER PATTERN (as Observer)
 *    MessageManager (Subject) calls update() when a new message arrives.
 *    The notification is enqueued here and drained to System.out the next
 *    time the student returns to the main menu — making delivery asynchronous.
 *
 * CSV format (users.csv):
 *   username,password,major,strengths,weaknesses,role,status
 *   Strengths and weaknesses are semicolon-delimited within the field.
 */
public class Student implements Observer {

    // ── Fields ────────────────────────────────────────────────────────────────

    private String       username;
    private String       password;
    private String       major;
    private List<String> strengths;
    private List<String> weaknesses;
    private String       role;        // "USER" or "ADMIN"

    // STATE PATTERN: the state object encapsulates all account-status behaviour
    private StudentState state;

    // OBSERVER PATTERN: pending notifications queued for async delivery
    // ConcurrentLinkedQueue used for thread-safety in web server mode
    private final Queue<String> pendingNotifications = new ConcurrentLinkedQueue<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    public Student(String username, String password, String major,
                   List<String> strengths, List<String> weaknesses,
                   String role, StudentState state) {
        this.username   = username;
        this.password   = password;
        this.major      = major;
        this.strengths  = new ArrayList<>(strengths);
        this.weaknesses = new ArrayList<>(weaknesses);
        this.role       = role;
        this.state      = state;
    }

    // ── OBSERVER PATTERN ──────────────────────────────────────────────────────

    /** Returns this student's unique identifier for observer routing. */
    @Override
    public String getUsername() { return username; }

    /**
     * Called by MessageManager (Subject) when a new message arrives.
     * The notification is stored in a queue — NOT displayed immediately.
     * This models asynchronous delivery: the recipient sees the alert
     * only when they next return to the main CLI menu.
     */
    @Override
    public void update(String notification) {
        pendingNotifications.add(notification);
    }

    /**
     * Drain and print all queued notifications to System.out.
     * Call this at the top of each main-menu loop iteration.
     */
    public void drainNotifications() {
        while (!pendingNotifications.isEmpty()) {
            System.out.println("\n  [!] " + pendingNotifications.poll());
        }
    }

    public boolean hasPendingNotifications() {
        return !pendingNotifications.isEmpty();
    }

    /**
     * Drain all queued notifications into a List and return them.
     * Used by the web server's notification polling endpoint.
     * (The CLI uses drainNotifications() which prints to stdout instead.)
     */
    public List<String> drainNotificationsAsList() {
        List<String> result = new ArrayList<>();
        while (!pendingNotifications.isEmpty()) {
            result.add(pendingNotifications.poll());
        }
        return result;
    }

    // ── STATE PATTERN — behaviour delegation ──────────────────────────────────

    /** Delegates to current state: suspended students cannot send messages. */
    public boolean canSendMessage()    { return state.canSendMessage(); }

    /** Delegates to current state: suspended students are hidden from search. */
    public boolean canAppearInSearch() { return state.canAppearInSearch(); }

    /** Delegates to current state: all students may update their profile. */
    public boolean canUpdateProfile()  { return state.canUpdateProfile(); }

    /** Human-readable state name for display and CSV persistence. */
    public String getStateName()       { return state.getStateName(); }

    // ── STATE PATTERN — state transitions ────────────────────────────────────

    /** Transition to SuspendedState (called by admin panel). */
    public void suspend()   { this.state = new SuspendedState(); }

    /** Transition back to NormalState (called by admin panel). */
    public void reinstate() { this.state = new NormalState(); }

    // ── Serialisation ─────────────────────────────────────────────────────────

    /**
     * Serialise this student to a CSV row.
     * Strengths and weaknesses use semicolons as internal delimiters.
     */
    public String toCsvRow() {
        String strengthsField  = strengths.isEmpty()  ? "-" : String.join(";", strengths);
        String weaknessesField = weaknesses.isEmpty() ? "-" : String.join(";", weaknesses);
        return username + "," + password + "," + major + "," +
               strengthsField + "," + weaknessesField + "," +
               role + "," + state.getStateName();
    }

    /**
     * Deserialise a CSV row into a Student.
     * Reconstructs the correct StudentState based on the stored status string.
     */
    public static Student fromCsvRow(String line) {
        String[] parts = line.split(",", 7);
        String username = parts[0].trim();
        String password = parts[1].trim();
        String major    = parts[2].trim();

        List<String> strengths = new ArrayList<>();
        if (!parts[3].trim().equals("-") && !parts[3].trim().isEmpty()) {
            strengths.addAll(Arrays.asList(parts[3].trim().split(";")));
        }

        List<String> weaknesses = new ArrayList<>();
        if (!parts[4].trim().equals("-") && !parts[4].trim().isEmpty()) {
            weaknesses.addAll(Arrays.asList(parts[4].trim().split(";")));
        }

        String role   = parts[5].trim();
        String status = parts[6].trim();

        // STATE PATTERN: reconstruct the correct state from the persisted string
        StudentState state = status.equalsIgnoreCase("SUSPENDED")
                ? new SuspendedState()
                : new NormalState();

        return new Student(username, password, major, strengths, weaknesses, role, state);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String       getPassword()   { return password; }
    public String       getMajor()      { return major; }
    public List<String> getStrengths()  { return strengths; }
    public List<String> getWeaknesses() { return weaknesses; }
    public String       getRole()       { return role; }

    public void setMajor(String major)            { this.major = major; }
    public void setStrengths(List<String> s)      { this.strengths  = new ArrayList<>(s); }
    public void setWeaknesses(List<String> w)     { this.weaknesses = new ArrayList<>(w); }

    public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(role); }
}
