package com.eatsmart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.exception.UnreadableReceiptException;
import com.eatsmart.domain.model.AnalyzeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

class AnalysisResultParserTest {

    private AnalysisResultParser parser;

    @BeforeEach
    void setUp() {
        parser = new AnalysisResultParser();
        parser.mapper = new ObjectMapper();
    }

    @Test
    void parse_validJson_returnsResponse() throws Exception {
        String json = """
                {"products": ["leche", "pan"], "score": 7, "suggestions": "Compra saludable"}
                """;
        AnalyzeResponse result = parser.parse(json);
        assertThat(result.products()).containsExactly("leche", "pan");
        assertThat(result.suggestions()).isEqualTo("Compra saludable");
        assertThat(result.score()).isEqualTo(7);
    }

    @Test
    void parse_jsonWithMarkdownFences_parsesCorrectly() throws Exception {
        String text = """
                ```json
                {"products": ["leche"], "score": 5, "suggestions": "Bien"}
                ```
                """;
        AnalyzeResponse result = parser.parse(text);
        assertThat(result.products()).containsExactly("leche");
    }

    @Test
    void parse_jsonWithSurroundingProse_parsesCorrectly() throws Exception {
        String text = """
                Aquí tienes el resultado:
                {"products": ["leche"], "score": 5, "suggestions": "Bien"}
                Espero que te sirva.
                """;
        AnalyzeResponse result = parser.parse(text);
        assertThat(result.products()).containsExactly("leche");
    }

    @Test
    void parse_null_throwsAnalysisException() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("no interpretable");
    }

    @Test
    void parse_emptyString_throwsAnalysisException() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(AnalysisException.class);
    }

    @Test
    void parse_notJson_throwsAnalysisException() {
        assertThatThrownBy(() -> parser.parse("esto no es json"))
                .isInstanceOf(AnalysisException.class);
    }

    @Test
    void parse_errorResponse_throwsUnreadableReceiptException() {
        String json = """
                {"error": "No se ve un ticket"}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(UnreadableReceiptException.class)
                .hasMessageContaining("No se ve un ticket");
    }

    @Test
    void parse_productsNotList_throwsAnalysisException() {
        String json = """
                {"products": "no es lista", "score": 5, "suggestions": "Bien"}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class);
    }

    @Test
    void parse_missingSuggestions_throwsAnalysisException() {
        String json = """
                {"products": ["leche"], "score": 5}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_blankSuggestions_throwsAnalysisException() {
        String json = """
                {"products": ["leche"], "score": 5, "suggestions": "   "}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_emptyProductsList_returnsResponse() throws Exception {
        String json = """
                {"products": [], "score": 3, "suggestions": "No encontré productos"}
                """;
        AnalyzeResponse result = parser.parse(json);
        assertThat(result.products()).isEmpty();
        assertThat(result.suggestions()).isEqualTo("No encontré productos");
        assertThat(result.score()).isEqualTo(3);
    }

    @Test
    void parse_missingProductsField_returnsEmptyProducts() throws Exception {
        String json = """
                {"score": 8, "suggestions": "Algo"}
                """;
        AnalyzeResponse result = parser.parse(json);
        assertThat(result.products()).isNull();
        assertThat(result.suggestions()).isEqualTo("Algo");
    }

    @Test
    void parse_missingScore_throwsAnalysisException() {
        String json = """
                {"products": ["leche"], "suggestions": "Bien"}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_scoreNotNumber_throwsAnalysisException() {
        String json = """
                {"products": ["leche"], "score": "alto", "suggestions": "Bien"}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_scoreAboveTen_clampedToTen() throws Exception {
        String json = """
                {"products": ["leche"], "score": 42, "suggestions": "Bien"}
                """;
        assertThat(parser.parse(json).score()).isEqualTo(10);
    }

    @Test
    void parse_scoreBelowZero_clampedToZero() throws Exception {
        String json = """
                {"products": ["leche"], "score": -3, "suggestions": "Bien"}
                """;
        assertThat(parser.parse(json).score()).isEqualTo(0);
    }

    @Test
    void parse_decimalScore_truncatedToInt() throws Exception {
        String json = """
                {"products": ["leche"], "score": 7.8, "suggestions": "Bien"}
                """;
        assertThat(parser.parse(json).score()).isEqualTo(7);
    }
}
