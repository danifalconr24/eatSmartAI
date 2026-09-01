package com.eatsmart.domain.model;

import java.util.List;

/**
 * Context of a previous analysis the user is chatting about. Exactly one of
 * the two analysis kinds is expected to be present:
 * <ul>
 * <li>Receipt analysis: {@code products} + {@code suggestions} + {@code score}</li>
 * <li>Product analysis: {@code product} + {@code nutrition} + {@code score}</li>
 * </ul>
 * Unused fields stay null. User profile fields apply to both kinds.
 */
public record ChatContext(
        List<String> products,
        String suggestions,
        String product,
        String nutrition,
        Integer score,
        String goal,
        boolean budgetMatters,
        String allergies,
        String dietPreference) {
}
