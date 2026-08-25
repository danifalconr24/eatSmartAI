package com.eatsmart.infrastructure.openrouter.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class OpenRouterChatResponseTest {

    @Test
    void firstContent_withContent_returnsContent() {
        OpenRouterChatResponse response = new OpenRouterChatResponse("model",
                List.of(new OpenRouterChatResponse.Choice(
                        new OpenRouterChatResponse.Message("hello"))));
        assertThat(response.firstContent()).isEqualTo("hello");
    }

    @Test
    void firstContent_nullChoices_returnsNull() {
        OpenRouterChatResponse response = new OpenRouterChatResponse("model", null);
        assertThat(response.firstContent()).isNull();
    }

    @Test
    void firstContent_emptyChoices_returnsNull() {
        OpenRouterChatResponse response = new OpenRouterChatResponse("model", List.of());
        assertThat(response.firstContent()).isNull();
    }

    @Test
    void firstContent_nullMessage_returnsNull() {
        OpenRouterChatResponse response = new OpenRouterChatResponse("model",
                List.of(new OpenRouterChatResponse.Choice(null)));
        assertThat(response.firstContent()).isNull();
    }

    @Test
    void firstContent_nullContentInMessage_returnsNull() {
        OpenRouterChatResponse response = new OpenRouterChatResponse("model",
                List.of(new OpenRouterChatResponse.Choice(
                        new OpenRouterChatResponse.Message(null))));
        assertThat(response.firstContent()).isNull();
    }
}
