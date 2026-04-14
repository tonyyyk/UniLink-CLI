package model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for the Message model.
 *
 * Tests CSV save/load and read-status behaviour.
 */
public class MessageTest {

    // A fixed date/time so tests are predictable
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 4, 14, 10, 30, 0);

    // ── CSV round-trip ────────────────────────────────────────────────────────

    @Test
    public void testCsvRoundTrip_senderIsPreserved() {
        Message msg = new Message("alice", "bob", TIME, "Hello", false);
        Message restored = Message.fromCsvRow(msg.toCsvRow());
        assertEquals("alice", restored.getSender());
    }

    @Test
    public void testCsvRoundTrip_receiverIsPreserved() {
        Message msg = new Message("alice", "bob", TIME, "Hello", false);
        Message restored = Message.fromCsvRow(msg.toCsvRow());
        assertEquals("bob", restored.getReceiver());
    }

    @Test
    public void testCsvRoundTrip_contentIsPreserved() {
        Message msg = new Message("alice", "bob", TIME, "Hello", false);
        Message restored = Message.fromCsvRow(msg.toCsvRow());
        assertEquals("Hello", restored.getContent());
    }

    @Test
    public void testCsvRoundTrip_readFlagIsPreserved_false() {
        Message msg = new Message("alice", "bob", TIME, "Hi", false);
        Message restored = Message.fromCsvRow(msg.toCsvRow());
        assertFalse(restored.isRead());
    }

    @Test
    public void testCsvRoundTrip_readFlagIsPreserved_true() {
        Message msg = new Message("alice", "bob", TIME, "Hi", true);
        Message restored = Message.fromCsvRow(msg.toCsvRow());
        assertTrue(restored.isRead());
    }

    // ── Read status ───────────────────────────────────────────────────────────

    @Test
    public void testSetRead_changesFlag() {
        Message msg = new Message("alice", "bob", TIME, "Hi", false);
        msg.setRead(true);
        assertTrue(msg.isRead());
    }

    // ── Formatted timestamp ───────────────────────────────────────────────────

    @Test
    public void testGetFormattedTimestamp_correctFormat() {
        Message msg = new Message("a", "b", TIME, "x", false);
        assertEquals("2026-04-14 10:30", msg.getFormattedTimestamp());
    }
}
