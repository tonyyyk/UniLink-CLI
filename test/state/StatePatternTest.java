package state;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for the State Pattern.
 *
 * UniLink has two account states:
 *   - NormalState    → full access
 *   - SuspendedState → restricted (no messaging, hidden from search)
 *
 * Each test checks one specific rule.
 */
public class StatePatternTest {

    // ── NormalState tests ─────────────────────────────────────────────────────

    @Test
    public void testNormalState_canSendMessage() {
        NormalState state = new NormalState();
        assertTrue(state.canSendMessage());
    }

    @Test
    public void testNormalState_canAppearInSearch() {
        NormalState state = new NormalState();
        assertTrue(state.canAppearInSearch());
    }

    @Test
    public void testNormalState_canUpdateProfile() {
        NormalState state = new NormalState();
        assertTrue(state.canUpdateProfile());
    }

    @Test
    public void testNormalState_nameIsNORMAL() {
        NormalState state = new NormalState();
        assertEquals("NORMAL", state.getStateName());
    }

    // ── SuspendedState tests ──────────────────────────────────────────────────

    @Test
    public void testSuspendedState_cannotSendMessage() {
        SuspendedState state = new SuspendedState();
        assertFalse(state.canSendMessage());
    }

    @Test
    public void testSuspendedState_cannotAppearInSearch() {
        SuspendedState state = new SuspendedState();
        assertFalse(state.canAppearInSearch());
    }

    @Test
    public void testSuspendedState_canStillUpdateProfile() {
        // Suspended users can still edit their profile to appeal a suspension
        SuspendedState state = new SuspendedState();
        assertTrue(state.canUpdateProfile());
    }

    @Test
    public void testSuspendedState_nameIsSUSPENDED() {
        SuspendedState state = new SuspendedState();
        assertEquals("SUSPENDED", state.getStateName());
    }
}
