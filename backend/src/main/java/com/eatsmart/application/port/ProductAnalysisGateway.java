package com.eatsmart.application.port;

import com.eatsmart.domain.exception.AnalysisException;

/**
 * Driven port: an AI provider capable of analyzing a single product photo
 * given a prompt. Implementations live in the infrastructure layer.
 *
 * Returns the raw text produced by the model; parsing into the domain model
 * is the responsibility of the application layer.
 */
public interface ProductAnalysisGateway {

    /** Human-readable provider name, used in logs. */
    String name();

    /** Whether this provider is configured (e.g. API key present). */
    boolean isEnabled();

    /**
     * Sends the product image plus prompt to the provider.
     *
     * @return raw text content returned by the model
     * @throws AnalysisException on transport errors, provider errors or empty/unusable responses
     */
    String analyze(byte[] imageBytes, String mimeType, String prompt) throws AnalysisException;
}
