package strategy;

import model.Student;

import java.util.List;

/**
 * STRATEGY PATTERN — Concrete strategy: Complementary Skills Matching.
 *
 * This is the required algorithm specified in the project brief.
 * Scoring rules:
 *   - Base score:               20%
 *   - Same major bonus:        +20%
 *   - Per complementary pair:  +30%  (capped at 100%)
 *
 * A "complementary pair" exists when:
 *   current.weakness ∈ candidate.strengths
 *   AND candidate.weakness ∈ current.strengths
 *
 * The idea: if I'm weak in Databases and you're strong in Databases,
 * AND you're weak in Algorithms and I'm strong in Algorithms — we can
 * teach each other. That symmetry is what the +30% rewards.
 */
public class ComplementaryMatchingStrategy implements MatchingStrategy {

    @Override
    public String getStrategyName() {
        return "Complementary Skills Matching (Default)";
    }

    @Override
    public double calculateScore(Student current, Student candidate) {
        double score = 0.20; // base score

        // +20% bonus when both students share the same academic major
        if (!current.getMajor().isBlank() &&
            current.getMajor().equalsIgnoreCase(candidate.getMajor())) {
            score += 0.20;
        }

        // +30% per complementary skill pair
        List<String> myWeaknesses        = current.getWeaknesses();
        List<String> myStrengths         = current.getStrengths();
        List<String> theirStrengths      = candidate.getStrengths();
        List<String> theirWeaknesses     = candidate.getWeaknesses();

        for (String myWeak : myWeaknesses) {
            // They can help me with myWeak (it's their strength)
            boolean theyCanHelpMe = theirStrengths.stream()
                    .anyMatch(s -> s.equalsIgnoreCase(myWeak));

            if (theyCanHelpMe) {
                // AND I can help them with at least one of their weaknesses
                boolean iCanHelpThem = theirWeaknesses.stream()
                        .anyMatch(tw -> myStrengths.stream()
                                .anyMatch(ms -> ms.equalsIgnoreCase(tw)));
                if (iCanHelpThem) {
                    score += 0.30;
                }
            }
        }

        return Math.min(score, 1.0); // cap at 100%
    }
}
