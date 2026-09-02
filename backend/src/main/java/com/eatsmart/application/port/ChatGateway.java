package com.eatsmart.application.port;

import java.util.List;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.model.ChatMessage;

/**
 * Driven port: an AI provider capable of holding a multi-turn text-only
 * conversation about a previous analysis result. Implementations live in the
 * infrastructure layer.
 *
 * Returns the raw text produced by the model; the answer is free text
 * (markdown), so no parsing is required in the application layer.
 */
public interface ChatGateway {

    /** Human-readable provider name, used in logs. */
    String name();

    /** Whether this provider is configured (e.g. API key present). */
    boolean isEnabled();

    /**
     * Sends a chat turn to the provider.
     *
     * @param systemPrompt instructions + analysis context (role: system)
     * @param history      previous conversation turns (user/assistant), may be empty
     * @param question     the new user question
     * @return raw text content returned by the model
     * @throws AnalysisException on transport errors, provider errors or empty/unusable responses
     */
    String chat(String systemPrompt, List<ChatMessage> history, String question) throws AnalysisException;
}
