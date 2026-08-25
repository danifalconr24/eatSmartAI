package com.eatsmart.infrastructure.gemini.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class GeminiGenerateResponseTest {

    @Test
    void firstText_withContent_returnsText() {
        GeminiGenerateResponse response = new GeminiGenerateResponse(
                List.of(new GeminiGenerateResponse.Candidate(
                        new GeminiGenerateResponse.Content(
                                List.of(new GeminiGenerateResponse.Part("hello"))))));
        assertThat(response.firstText()).isEqualTo("hello");
    }

    @Test
    void firstText_nullCandidates_returnsNull() {
        GeminiGenerateResponse response = new GeminiGenerateResponse(null);
        assertThat(response.firstText()).isNull();
    }

    @Test
    void firstText_emptyCandidates_returnsNull() {
        GeminiGenerateResponse response = new GeminiGenerateResponse(List.of());
        assertThat(response.firstText()).isNull();
    }

    @Test
    void firstText_nullContent_returnsNull() {
        GeminiGenerateResponse response = new GeminiGenerateResponse(
                List.of(new GeminiGenerateResponse.Candidate(null)));
        assertThat(response.firstText()).isNull();
    }

    @Test
    void firstText_nullParts_returnsNull() {
        GeminiGenerateResponse response = new GeminiGenerateResponse(
                List.of(new GeminiGenerateResponse.Candidate(
                        new GeminiGenerateResponse.Content(null))));
        assertThat(response.firstText()).isNull();
    }

    @Test
    void firstText_emptyParts_returnsNull() {
        GeminiGenerateResponse response = new GeminiGenerateResponse(
                List.of(new GeminiGenerateResponse.Candidate(
                        new GeminiGenerateResponse.Content(List.of()))));
        assertThat(response.firstText()).isNull();
    }

    @Test
    void firstText_nullTextInPart_returnsNull() {
        GeminiGenerateResponse response = new GeminiGenerateResponse(
                List.of(new GeminiGenerateResponse.Candidate(
                        new GeminiGenerateResponse.Content(
                                List.of(new GeminiGenerateResponse.Part(null))))));
        assertThat(response.firstText()).isNull();
    }
}
