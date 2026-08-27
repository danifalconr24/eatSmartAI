package com.eatsmart.infrastructure.rest.dto;

import java.util.List;

/** Request body for POST /api/shopping-lists/generate. */
public record GenerateShoppingListRequest(
        List<String> products,
        String suggestions,
        String goal,
        String dietPreference,
        boolean budgetMatters,
        String allergies) {
}
