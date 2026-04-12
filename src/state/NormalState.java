package state;

/**
 * STATE PATTERN — Concrete state: Normal.
 *
 * A student in NormalState has full access: they can send messages,
 * appear in partner searches, and update their profile. This is the
 * default state assigned at registration.
 */
public class NormalState implements StudentState {

    @Override
    public boolean canSendMessage()    { return true; }

    @Override
    public boolean canAppearInSearch() { return true; }

    @Override
    public boolean canUpdateProfile()  { return true; }

    @Override
    public String getStateName()       { return "NORMAL"; }
}
