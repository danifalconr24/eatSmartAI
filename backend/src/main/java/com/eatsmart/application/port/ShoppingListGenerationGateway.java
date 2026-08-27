package com.eatsmart.application.port;

import com.eatsmart.domain.exception.AnalysisException;

/**
 * Driven port: an AI provider capable of generating a shopping list from a
 * text-only prompt (no image). Used by features that build on an
 * already-scanned result. Implementations live in the infrastructure layer.
 *
 * Returns the raw text produced by the model; parsing into the domain model
 * is the responsibility of the application layer.
 */
public interface ShoppingListGenerationGateway {

    /** Human-readable provider name, used in logs. */
    String name();

    /** Whether this provider is configured (e.g. API key present). */
    boolean isEnabled();

    /**
     * Sends a text-only prompt (no image) to the provider.
     *
     * @return raw text content returned by the model
     * @throws AnalysisException on transport errors, provider errors or empty/unusable responses
     */
    String generateText(String prompt) throws AnalysisException;
}
