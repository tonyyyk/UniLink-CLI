package model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for the StudyGroup model.
 *
 * Tests CSV save/load and group membership logic.
 */
public class StudyGroupTest {

    // ── CSV round-trip ────────────────────────────────────────────────────────

    @Test
    public void testCsvRoundTrip_groupIdIsPreserved() {
        StudyGroup group = new StudyGroup(1, "Algo Squad", "alice", List.of("alice"), "Algorithms");
        StudyGroup restored = StudyGroup.fromCsvRow(group.toCsvRow());
        assertEquals(1, restored.getGroupId());
    }

    @Test
    public void testCsvRoundTrip_groupNameIsPreserved() {
        StudyGroup group = new StudyGroup(1, "Algo Squad", "alice", List.of("alice"), "Algorithms");
        StudyGroup restored = StudyGroup.fromCsvRow(group.toCsvRow());
        assertEquals("Algo Squad", restored.getGroupName());
    }

    @Test
    public void testCsvRoundTrip_topicIsPreserved() {
        StudyGroup group = new StudyGroup(1, "Algo Squad", "alice", List.of("alice"), "Algorithms");
        StudyGroup restored = StudyGroup.fromCsvRow(group.toCsvRow());
        assertEquals("Algorithms", restored.getTopic());
    }

    @Test
    public void testCsvRoundTrip_multipleMembersArePreserved() {
        StudyGroup group = new StudyGroup(2, "Math Club", "bob",
            Arrays.asList("bob", "carol", "dave"), "Calculus");

        StudyGroup restored = StudyGroup.fromCsvRow(group.toCsvRow());

        assertEquals(3, restored.getMembers().size());
        assertTrue(restored.getMembers().containsAll(List.of("bob", "carol", "dave")));
    }

    // ── Membership ────────────────────────────────────────────────────────────

    @Test
    public void testHasMember_returnsTrue_forExistingMember() {
        StudyGroup group = new StudyGroup(1, "G", "alice", List.of("alice", "bob"), "T");
        assertTrue(group.hasMember("alice"));
        assertTrue(group.hasMember("bob"));
    }

    @Test
    public void testHasMember_returnsFalse_forNonMember() {
        StudyGroup group = new StudyGroup(1, "G", "alice", List.of("alice"), "T");
        assertFalse(group.hasMember("carol"));
    }

    @Test
    public void testAddMember_addsNewUser() {
        StudyGroup group = new StudyGroup(1, "G", "alice", List.of("alice"), "T");
        group.addMember("bob");
        assertTrue(group.hasMember("bob"));
        assertEquals(2, group.getMembers().size());
    }

    @Test
    public void testAddMember_doesNotAddDuplicate() {
        StudyGroup group = new StudyGroup(1, "G", "alice", List.of("alice"), "T");
        group.addMember("alice"); // already a member
        assertEquals(1, group.getMembers().size());
    }
}
