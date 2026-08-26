package com.eatsmart.infrastructure.openrouter.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Supports OpenRouter model routing: {@code models} is an ordered fallback
 * list (first available model wins).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenRouterChatRequest(List<String> models, List<Message> messages, Double temperature) {

    public record Message(String role, List<Content> content) {
        public static Message user(List<Content> content) {
            return new Message("user", content);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Content(String type, String text, @JsonProperty("image_url") ImageUrl imageUrl) {
        public static Content text(String text) {
            return new Content("text", text, null);
        }

        public static Content image(String dataUrl) {
            return new Content("image_url", null, new ImageUrl(dataUrl));
        }
    }

    public record ImageUrl(String url) {
    }
}
