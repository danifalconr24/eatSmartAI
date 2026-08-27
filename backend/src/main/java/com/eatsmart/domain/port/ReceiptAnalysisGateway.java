package com.eatsmart.domain.port;

import com.eatsmart.domain.exception.AnalysisException;

/**
 * Driven port: an AI provider capable of extracting text from a receipt image
 * given a prompt. Implementations live in the infrastructure layer.
 *
 * Returns the raw text produced by the model; parsing into the domain model
 * is the responsibility of the application layer.
 */
public interface ReceiptAnalysisGateway {

    /** Human-readable provider name, used in logs. */
    String name();

    /** Whether this provider is configured (e.g. API key present). */
    boolean isEnabled();

    /**
     * Sends the receipt image plus prompt to the provider.
     *
     * @return raw text content returned by the model
     * @throws AnalysisException on transport errors, provider errors or empty/unusable responses
     */
    String analyze(byte[] imageBytes, String mimeType, String prompt) throws AnalysisException;

    /**
     * Sends a text-only prompt (no image) to the provider. Used by features
     * that build on an already-scanned result, e.g. shopping list generation.
     *
     * @return raw text content returned by the model
     * @throws AnalysisException on transport errors, provider errors or empty/unusable responses
     */
    default String generateText(String prompt) throws AnalysisException {
        throw new AnalysisException(name() + " no admite peticiones sin imagen.", null);
    }
}
