package com.eatsmart.domain.model;

/**
 * One message of a nutritionist chat conversation.
 *
 * @param role    "user" or "assistant"
 * @param content message text
 */
public record ChatMessage(String role, String content) {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    public static ChatMessage user(String content) {
        return new ChatMessage(ROLE_USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ROLE_ASSISTANT, content);
    }
}
