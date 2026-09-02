package com.eatsmart.infrastructure.gemini.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiGenerateRequest(List<Content> contents,
        @JsonProperty("generationConfig") GenerationConfig generationConfig) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Content(String role, List<Part> parts) {
        public static Content of(List<Part> parts) {
            return new Content(null, parts);
        }

        public static Content user(List<Part> parts) {
            return new Content("user", parts);
        }

        public static Content model(List<Part> parts) {
            return new Content("model", parts);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(String text, @JsonProperty("inline_data") InlineData inlineData) {
        public static Part text(String text) {
            return new Part(text, null);
        }

        public static Part image(String mimeType, String base64Data) {
            return new Part(null, new InlineData(mimeType, base64Data));
        }
    }

    public record InlineData(@JsonProperty("mime_type") String mimeType, String data) {
    }

    public record GenerationConfig(@JsonProperty("responseMimeType") String responseMimeType, Double temperature) {
    }
}
