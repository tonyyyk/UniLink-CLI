package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import state.NormalState;
import state.SuspendedState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for the Student model.
 *
 * Tests three things:
 *   1. CSV save and load (toCsvRow / fromCsvRow)
 *   2. State Pattern — suspend and reinstate
 *   3. Observer Pattern — notification queue
 */
public class StudentTest {

    private Student alice;

    // @BeforeEach runs before every test to set up a fresh Student
    @BeforeEach
    public void setUp() {
        alice = new Student(
            "alice", "alice123", "Computer Science",
            Arrays.asList("Java", "Algorithms"),
            Arrays.asList("Databases", "Networks"),
            "USER", new NormalState()
        );
    }

    // ── CSV round-trip ────────────────────────────────────────────────────────

    @Test
    public void testCsvRoundTrip_usernameIsPreserved() {
        Student restored = Student.fromCsvRow(alice.toCsvRow());
        assertEquals("alice", restored.getUsername());
    }

    @Test
    public void testCsvRoundTrip_passwordIsPreserved() {
        Student restored = Student.fromCsvRow(alice.toCsvRow());
        assertEquals("alice123", restored.getPassword());
    }

    @Test
    public void testCsvRoundTrip_majorIsPreserved() {
        Student restored = Student.fromCsvRow(alice.toCsvRow());
        assertEquals("Computer Science", restored.getMajor());
    }

    @Test
    public void testCsvRoundTrip_strengthsArePreserved() {
        Student restored = Student.fromCsvRow(alice.toCsvRow());
        assertEquals(Arrays.asList("Java", "Algorithms"), restored.getStrengths());
    }

    @Test
    public void testCsvRoundTrip_weaknessesArePreserved() {
        Student restored = Student.fromCsvRow(alice.toCsvRow());
        assertEquals(Arrays.asList("Databases", "Networks"), restored.getWeaknesses());
    }

    @Test
    public void testCsvRoundTrip_oldFormatBackwardCompatible() {
        // An old 7-column CSV row (no extended fields) should still parse fine
        String oldRow = "carol,carol123,Physics,Math;Lab,-,USER,NORMAL";
        Student s = Student.fromCsvRow(oldRow);
        assertEquals("carol", s.getUsername());
        assertEquals("", s.getDateOfBirth());   // empty — not in old format
        assertEquals("", s.getIntroduction());  // empty — not in old format
    }

    // ── State Pattern ─────────────────────────────────────────────────────────

    @Test
    public void testNormalStudent_canSendMessage() {
        assertTrue(alice.canSendMessage());
    }

    @Test
    public void testSuspend_blocksMessaging() {
        alice.suspend();
        assertFalse(alice.canSendMessage());
    }

    @Test
    public void testSuspend_hidesFromSearch() {
        alice.suspend();
        assertFalse(alice.canAppearInSearch());
    }

    @Test
    public void testReinstate_restoresMessaging() {
        alice.suspend();
        alice.reinstate();
        assertTrue(alice.canSendMessage());
    }

    @Test
    public void testStateName_changesAfterSuspend() {
        assertEquals("NORMAL", alice.getStateName());
        alice.suspend();
        assertEquals("SUSPENDED", alice.getStateName());
    }

    @Test
    public void testIsAdmin_falseForRegularUser() {
        assertFalse(alice.isAdmin());
    }

    @Test
    public void testIsAdmin_trueForAdminRole() {
        Student admin = new Student(
            "admin", "admin123", "-",
            Collections.emptyList(), Collections.emptyList(),
            "ADMIN", new NormalState()
        );
        assertTrue(admin.isAdmin());
    }

    // ── Observer Pattern ──────────────────────────────────────────────────────

    @Test
    public void testUpdate_queuesNotification() {
        // Before any notification
        assertFalse(alice.hasPendingNotifications());

        // Receive a notification (Observer pattern — called by MessageManager)
        alice.update("You have a new message from bob!");

        assertTrue(alice.hasPendingNotifications());
    }

    @Test
    public void testDrainNotifications_returnsAllAndClearsQueue() {
        alice.update("Message 1");
        alice.update("Message 2");

        List<String> drained = alice.drainNotificationsAsList();

        assertEquals(2, drained.size());
        assertFalse(alice.hasPendingNotifications()); // queue is now empty
    }
}
