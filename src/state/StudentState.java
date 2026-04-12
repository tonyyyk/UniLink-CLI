package state;

/**
 * STATE PATTERN — State interface.
 *
 * Defines the behaviour contract that changes depending on a student's
 * account lifecycle (Normal vs Suspended). By delegating these checks to
 * the state object rather than scattering if-suspended blocks throughout
 * the codebase, we keep Student clean and easy to extend.
 */
public interface StudentState {
    /** Whether the student may send messages to others. */
    boolean canSendMessage();

    /** Whether the student appears in partner-search results. */
    boolean canAppearInSearch();

    /** Whether the student may edit their own profile. */
    boolean canUpdateProfile();

    /** Returns the string name of this state (used when persisting to CSV). */
    String getStateName();
}
