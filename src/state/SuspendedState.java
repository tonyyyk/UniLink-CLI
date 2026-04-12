package state;

/**
 * STATE PATTERN — Concrete state: Suspended.
 *
 * A suspended student is restricted: they cannot send messages and do not
 * appear in other students' partner searches. However they retain the ability
 * to update their own profile so their data remains accurate when reinstated.
 *
 * All restrictions are enforced purely by returning false here — no
 * if-suspended checks are scattered through the rest of the codebase.
 */
public class SuspendedState implements StudentState {

    @Override
    public boolean canSendMessage()    { return false; }  // restricted

    @Override
    public boolean canAppearInSearch() { return false; }  // restricted

    @Override
    public boolean canUpdateProfile()  { return true; }   // still allowed

    @Override
    public String getStateName()       { return "SUSPENDED"; }
}
