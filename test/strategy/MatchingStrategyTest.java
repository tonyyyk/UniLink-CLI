package strategy;

import model.Student;
import org.junit.jupiter.api.Test;
import state.NormalState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for the Strategy Pattern — partner matching algorithms.
 *
 * Two strategies are tested:
 *
 *   ComplementaryMatchingStrategy:
 *     - Base:           20%
 *     - Same major:    +20%
 *     - Per pair:      +30%  (your weakness = their strength AND vice versa)
 *
 *   SameMajorMatchingStrategy:
 *     - Base:           20%
 *     - Same major:    +40%
 *     - Shared strength: +15% each
 *     - Shared weakness: +10% each
 *
 *   Both cap at 100%.
 */
public class MatchingStrategyTest {

    // Helper: build a Student with given major, strengths, weaknesses
    private Student makeStudent(String major, List<String> strengths, List<String> weaknesses) {
        return new Student("user", "pass", major, strengths, weaknesses, "USER", new NormalState());
    }

    // ── ComplementaryMatchingStrategy ─────────────────────────────────────────

    @Test
    public void testComplementary_baseScore_is20Percent() {
        // No shared skills, different majors → only the 20% base
        Student alice = makeStudent("CS",   Collections.emptyList(), Collections.emptyList());
        Student bob   = makeStudent("Math", Collections.emptyList(), Collections.emptyList());

        ComplementaryMatchingStrategy strategy = new ComplementaryMatchingStrategy();
        int score = (int) Math.round(strategy.calculateScore(alice, bob) * 100);

        assertEquals(20, score);
    }

    @Test
    public void testComplementary_sameMajor_adds20Percent() {
        // Same major, no complementary pairs → 20% base + 20% major = 40%
        Student alice = makeStudent("CS", List.of("Java"),    List.of("Networks"));
        Student bob   = makeStudent("CS", List.of("Calculus"), List.of("Algorithms"));

        ComplementaryMatchingStrategy strategy = new ComplementaryMatchingStrategy();
        int score = (int) Math.round(strategy.calculateScore(alice, bob) * 100);

        assertEquals(40, score);
    }

    @Test
    public void testComplementary_oneComplementaryPair_adds30Percent() {
        // alice weak in "Databases", bob strong in "Databases"
        // bob weak in "Java", alice strong in "Java"  → one complementary pair
        // Different majors → 20% base + 30% pair = 50%
        Student alice = makeStudent("CS",   List.of("Java"),      List.of("Databases"));
        Student bob   = makeStudent("Math", List.of("Databases"), List.of("Java"));

        ComplementaryMatchingStrategy strategy = new ComplementaryMatchingStrategy();
        int score = (int) Math.round(strategy.calculateScore(alice, bob) * 100);

        assertEquals(50, score);
    }

    @Test
    public void testComplementary_cappedAt100Percent() {
        // Three pairs + same major would be 20+20+90 = 130% → capped to 100%
        Student alice = makeStudent("CS",
            Arrays.asList("Java", "Algorithms", "Math"),
            Arrays.asList("Networks", "Databases", "Physics"));
        Student bob = makeStudent("CS",
            Arrays.asList("Networks", "Databases", "Physics"),
            Arrays.asList("Java", "Algorithms", "Math"));

        ComplementaryMatchingStrategy strategy = new ComplementaryMatchingStrategy();
        double raw = strategy.calculateScore(alice, bob);

        assertEquals(1.0, raw, 0.001);
    }

    // ── SameMajorMatchingStrategy ─────────────────────────────────────────────

    @Test
    public void testSameMajor_baseScore_is20Percent() {
        Student alice = makeStudent("CS",   Collections.emptyList(), Collections.emptyList());
        Student bob   = makeStudent("Math", Collections.emptyList(), Collections.emptyList());

        SameMajorMatchingStrategy strategy = new SameMajorMatchingStrategy();
        int score = (int) Math.round(strategy.calculateScore(alice, bob) * 100);

        assertEquals(20, score);
    }

    @Test
    public void testSameMajor_sameMajor_adds40Percent() {
        // Same major, no shared skills → 20% base + 40% major = 60%
        Student alice = makeStudent("CS", List.of("Java"),    List.of("Databases"));
        Student bob   = makeStudent("CS", List.of("Calculus"), List.of("Networks"));

        SameMajorMatchingStrategy strategy = new SameMajorMatchingStrategy();
        int score = (int) Math.round(strategy.calculateScore(alice, bob) * 100);

        assertEquals(60, score);
    }

    @Test
    public void testSameMajor_oneSharedStrength_adds15Percent() {
        // Same major + 1 shared strength → 20% + 40% + 15% = 75%
        Student alice = makeStudent("CS", List.of("Java"), Collections.emptyList());
        Student bob   = makeStudent("CS", List.of("Java"), Collections.emptyList());

        SameMajorMatchingStrategy strategy = new SameMajorMatchingStrategy();
        int score = (int) Math.round(strategy.calculateScore(alice, bob) * 100);

        assertEquals(75, score);
    }

    @Test
    public void testSameMajor_oneSharedWeakness_adds10Percent() {
        // Different major + 1 shared weakness → 20% base + 10% = 30%
        Student alice = makeStudent("CS",   Collections.emptyList(), List.of("Networking"));
        Student bob   = makeStudent("Math", Collections.emptyList(), List.of("Networking"));

        SameMajorMatchingStrategy strategy = new SameMajorMatchingStrategy();
        int score = (int) Math.round(strategy.calculateScore(alice, bob) * 100);

        assertEquals(30, score);
    }

    @Test
    public void testSameMajor_cappedAt100Percent() {
        Student alice = makeStudent("CS",
            Arrays.asList("Java", "Algorithms", "Math", "Physics", "ML"),
            Arrays.asList("Networks", "Databases", "OS"));
        Student bob = makeStudent("CS",
            Arrays.asList("Java", "Algorithms", "Math", "Physics", "ML"),
            Arrays.asList("Networks", "Databases", "OS"));

        SameMajorMatchingStrategy strategy = new SameMajorMatchingStrategy();
        double raw = strategy.calculateScore(alice, bob);

        assertEquals(1.0, raw, 0.001);
    }

    // ── Strategy Pattern: swapping changes the result ─────────────────────────

    @Test
    public void testStrategySwap_differentStrategiesGiveDifferentScores() {
        // This is the core demo of the Strategy Pattern:
        // same students, different strategy → different result
        Student alice = makeStudent("CS",   List.of("Java"),      List.of("Databases"));
        Student bob   = makeStudent("Math", List.of("Databases"), List.of("Java"));

        MatchingStrategy complementary = new ComplementaryMatchingStrategy();
        MatchingStrategy sameMajor     = new SameMajorMatchingStrategy();

        double score1 = complementary.calculateScore(alice, bob);
        double score2 = sameMajor.calculateScore(alice, bob);

        assertNotEquals(score1, score2);
    }
}
