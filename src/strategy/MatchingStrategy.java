package strategy;

import model.Student;

/**
 * STRATEGY PATTERN — Strategy interface.
 *
 * Defines the algorithm contract for calculating compatibility between two
 * students. Concrete strategies are swappable at runtime inside
 * FindPartnersCommand without changing any other class — this is the
 * classic demonstration of the Open/Closed Principle.
 */
public interface MatchingStrategy {
    /** Human-readable name displayed when the user selects an algorithm. */
    String getStrategyName();

    /**
     * Calculate a compatibility score in the range [0.0, 1.0] between the
     * currently logged-in student and a candidate partner.
     *
     * @param current   the logged-in student initiating the search
     * @param candidate a potential study partner from the user database
     * @return score between 0.0 (no match) and 1.0 (perfect match)
     */
    double calculateScore(Student current, Student candidate);
}
