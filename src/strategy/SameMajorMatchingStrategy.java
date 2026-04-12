package strategy;

import model.Student;

import java.util.List;

/**
 * STRATEGY PATTERN — Concrete strategy: Same-Major Priority Matching.
 *
 * An alternative algorithm that prioritises students in the same major
 * and rewards shared strengths (good for study sessions where everyone
 * already knows the topic well and wants to practice together).
 *
 * Scoring rules:
 *   - Base score:              20%
 *   - Same major bonus:       +40%
 *   - Per shared strength:    +15%  (shared topic = study together)
 *   - Per shared weakness:    +10%  (shared struggle = peer support)
 *   - Capped at 100%
 *
 * Switching to this strategy at runtime (without touching any other class)
 * is the live demo of the Strategy Pattern's Open/Closed Principle.
 */
public class SameMajorMatchingStrategy implements MatchingStrategy {

    @Override
    public String getStrategyName() {
        return "Same-Major Priority Matching";
    }

    @Override
    public double calculateScore(Student current, Student candidate) {
        double score = 0.20; // base score

        // +40% if same major (higher weight than the complementary strategy)
        if (!current.getMajor().isBlank() &&
            current.getMajor().equalsIgnoreCase(candidate.getMajor())) {
            score += 0.40;
        }

        List<String> myStrengths     = current.getStrengths();
        List<String> theirStrengths  = candidate.getStrengths();
        List<String> myWeaknesses    = current.getWeaknesses();
        List<String> theirWeaknesses = candidate.getWeaknesses();

        // +15% per shared strength (both are good at this — great for tutoring sessions)
        long sharedStrengths = myStrengths.stream()
                .filter(s -> theirStrengths.stream().anyMatch(ts -> ts.equalsIgnoreCase(s)))
                .count();
        score += sharedStrengths * 0.15;

        // +10% per shared weakness (empathy + peer accountability)
        long sharedWeaknesses = myWeaknesses.stream()
                .filter(w -> theirWeaknesses.stream().anyMatch(tw -> tw.equalsIgnoreCase(w)))
                .count();
        score += sharedWeaknesses * 0.10;

        return Math.min(score, 1.0); // cap at 100%
    }
}
