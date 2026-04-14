package manager;

import model.Student;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for UserManager (Singleton Pattern).
 *
 * Setup:
 *   - Before all tests, we back up the real users.csv
 *   - Before each test, we write a fresh empty CSV and reset the singleton
 *   - After all tests, we restore the original users.csv
 *
 * This means every test starts with zero users — clean and isolated.
 */
public class UserManagerTest {

    private static final Path CSV_PATH = Path.of("data/users.csv");
    private static String originalCsv; // backup of the real file

    @BeforeAll
    static void backupRealData() throws Exception {
        new File("data").mkdirs();
        if (Files.exists(CSV_PATH)) {
            originalCsv = Files.readString(CSV_PATH);
        }
    }

    @AfterAll
    static void restoreRealData() throws Exception {
        if (originalCsv != null) {
            Files.writeString(CSV_PATH, originalCsv);
        }
        resetSingleton();
    }

    @BeforeEach
    void freshSetup() throws Exception {
        // Write a clean CSV with only the header (no users)
        Files.writeString(CSV_PATH, "username,password,major,strengths,weaknesses,role,status\n");
        resetSingleton(); // force UserManager to start fresh
    }

    // Resets the singleton so the next getInstance() creates a new one
    static void resetSingleton() throws Exception {
        Field f = UserManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    public void testRegister_newUser_returnsTrue() throws Exception {
        boolean result = UserManager.getInstance().register("alice", "alice123", "CS");
        assertTrue(result);
    }

    @Test
    public void testRegister_duplicateUsername_returnsFalse() throws Exception {
        UserManager um = UserManager.getInstance();
        um.register("alice", "alice123", "CS");

        boolean duplicate = um.register("alice", "other", "Math");
        assertFalse(duplicate);
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    public void testLogin_correctPassword_returnsStudent() throws Exception {
        UserManager um = UserManager.getInstance();
        um.register("bob", "bob123", "Math");

        Student student = um.login("bob", "bob123");
        assertNotNull(student);
        assertEquals("bob", student.getUsername());
    }

    @Test
    public void testLogin_wrongPassword_returnsNull() throws Exception {
        UserManager um = UserManager.getInstance();
        um.register("bob", "bob123", "Math");

        Student student = um.login("bob", "WRONG");
        assertNull(student);
    }

    @Test
    public void testLogin_unknownUser_returnsNull() throws Exception {
        Student student = UserManager.getInstance().login("nobody", "pass");
        assertNull(student);
    }

    // ── usernameExists ────────────────────────────────────────────────────────

    @Test
    public void testUsernameExists_afterRegister_returnsTrue() throws Exception {
        UserManager um = UserManager.getInstance();
        um.register("carol", "carol123", "Physics");
        assertTrue(um.usernameExists("carol"));
    }

    @Test
    public void testUsernameExists_unknownUser_returnsFalse() throws Exception {
        assertFalse(UserManager.getInstance().usernameExists("ghost"));
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    public void testUpdateProfile_majorChange_isPersisted() throws Exception {
        UserManager um = UserManager.getInstance();
        um.register("dave", "dave123", "CS");

        Student dave = um.login("dave", "dave123");
        dave.setMajor("Data Science");
        um.updateProfile(dave);

        // Reset singleton to force re-read from file
        resetSingleton();
        Student reloaded = UserManager.getInstance().login("dave", "dave123");
        assertEquals("Data Science", reloaded.getMajor());
    }

    // ── suspend / reinstate ───────────────────────────────────────────────────

    @Test
    public void testSuspendUser_statusBecomeSUSPENDED() throws Exception {
        UserManager um = UserManager.getInstance();
        um.register("emma", "emma123", "Math");
        um.suspendUser("emma");

        resetSingleton();
        Student emma = UserManager.getInstance().login("emma", "emma123");
        assertEquals("SUSPENDED", emma.getStateName());
    }

    @Test
    public void testReinstateUser_statusBecomesNORMAL() throws Exception {
        UserManager um = UserManager.getInstance();
        um.register("frank", "frank123", "CS");
        um.suspendUser("frank");
        um.reinstateUser("frank");

        resetSingleton();
        Student frank = UserManager.getInstance().login("frank", "frank123");
        assertEquals("NORMAL", frank.getStateName());
    }

    // ── Singleton Pattern ─────────────────────────────────────────────────────

    @Test
    public void testSingleton_sameInstanceReturnedEveryTime() {
        UserManager a = UserManager.getInstance();
        UserManager b = UserManager.getInstance();
        assertSame(a, b); // must be the exact same object
    }
}
