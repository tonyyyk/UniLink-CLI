package manager;

import model.StudyGroup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for GroupManager (Singleton Pattern).
 *
 * Same setup approach as UserManagerTest:
 * back up → write fresh CSV → reset singleton → run test → restore.
 */
public class GroupManagerTest {

    private static final Path CSV_PATH = Path.of("data/groups.csv");
    private static String originalCsv;

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
        Files.writeString(CSV_PATH, "groupId,groupName,creator,members,topic\n");
        resetSingleton();
    }

    static void resetSingleton() throws Exception {
        Field f = GroupManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    // ── createGroup ───────────────────────────────────────────────────────────

    @Test
    public void testCreateGroup_returnsId1_forFirstGroup() throws Exception {
        int id = GroupManager.getInstance().createGroup("Algo Squad", "alice", "Algorithms");
        assertEquals(1, id);
    }

    @Test
    public void testCreateGroup_idsIncrement() throws Exception {
        GroupManager gm = GroupManager.getInstance();
        int id1 = gm.createGroup("Group A", "alice", "CS");
        int id2 = gm.createGroup("Group B", "bob",   "Math");
        assertEquals(id1 + 1, id2);
    }

    @Test
    public void testCreateGroup_creatorIsAddedAsMember() throws Exception {
        GroupManager gm = GroupManager.getInstance();
        gm.createGroup("DB Study", "carol", "Databases");

        StudyGroup group = gm.getAllGroups().get(0);
        assertTrue(group.hasMember("carol"));
    }

    // ── joinGroup ─────────────────────────────────────────────────────────────

    @Test
    public void testJoinGroup_newMember_isAdded() throws Exception {
        GroupManager gm = GroupManager.getInstance();
        int id = gm.createGroup("Networks", "alice", "Networking");

        boolean result = gm.joinGroup(id, "bob");

        assertTrue(result);
        assertTrue(gm.getAllGroups().get(0).hasMember("bob"));
    }

    @Test
    public void testJoinGroup_alreadyMember_returnsTrueNoDuplicate() throws Exception {
        GroupManager gm = GroupManager.getInstance();
        int id = gm.createGroup("Math Club", "alice", "Calculus");
        gm.joinGroup(id, "bob");

        boolean result = gm.joinGroup(id, "bob"); // join again

        assertTrue(result);
        assertEquals(2, gm.getAllGroups().get(0).getMembers().size()); // still 2, not 3
    }

    @Test
    public void testJoinGroup_nonExistentGroup_returnsFalse() throws Exception {
        boolean result = GroupManager.getInstance().joinGroup(999, "alice");
        assertFalse(result);
    }

    // ── getGroupsForUser ──────────────────────────────────────────────────────

    @Test
    public void testGetGroupsForUser_onlyReturnsGroupsUserBelongsTo() throws Exception {
        GroupManager gm = GroupManager.getInstance();
        int id1 = gm.createGroup("Alice's Group", "alice", "AI");
        int id2 = gm.createGroup("Bob's Group",   "bob",   "OS");
        gm.joinGroup(id2, "alice"); // alice joins both groups

        List<StudyGroup> aliceGroups = gm.getGroupsForUser("alice");
        List<StudyGroup> bobGroups   = gm.getGroupsForUser("bob");

        assertEquals(2, aliceGroups.size());
        assertEquals(1, bobGroups.size());
    }

    // ── getAllGroups ──────────────────────────────────────────────────────────

    @Test
    public void testGetAllGroups_returnsAllCreatedGroups() throws Exception {
        GroupManager gm = GroupManager.getInstance();
        gm.createGroup("G1", "alice", "T1");
        gm.createGroup("G2", "bob",   "T2");

        assertEquals(2, gm.getAllGroups().size());
    }

    @Test
    public void testGetAllGroups_emptyWhenNoGroups() throws Exception {
        assertTrue(GroupManager.getInstance().getAllGroups().isEmpty());
    }

    // ── Singleton Pattern ─────────────────────────────────────────────────────

    @Test
    public void testSingleton_sameInstanceReturnedEveryTime() {
        GroupManager a = GroupManager.getInstance();
        GroupManager b = GroupManager.getInstance();
        assertSame(a, b);
    }
}
