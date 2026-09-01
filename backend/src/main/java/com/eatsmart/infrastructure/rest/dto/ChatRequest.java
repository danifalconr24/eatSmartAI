package com.eatsmart.infrastructure.rest.dto;

import java.util.List;

/**
 * Request body for POST /api/chat. Exactly one analysis kind is expected:
 * receipt ({@code products} + {@code suggestions}) or product
 * ({@code product} + {@code nutrition}). {@code messages} holds the previous
 * conversation turns; {@code question} is the new user question.
 */
public record ChatRequest(
        List<String> products,
        String suggestions,
        String product,
        String nutrition,
        Integer score,
        String goal,
        String dietPreference,
        boolean budgetMatters,
        String allergies,
        List<Message> messages,
        String question) {

    public record Message(String role, String content) {
    }
}
