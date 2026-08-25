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
                {"products": ["leche", "pan"], "suggestions": "Compra saludable"}
                """;
        AnalyzeResponse result = parser.parse(json);
        assertThat(result.products()).containsExactly("leche", "pan");
        assertThat(result.suggestions()).isEqualTo("Compra saludable");
    }

    @Test
    void parse_jsonWithMarkdownFences_parsesCorrectly() throws Exception {
        String text = """
                ```json
                {"products": ["leche"], "suggestions": "Bien"}
                ```
                """;
        AnalyzeResponse result = parser.parse(text);
        assertThat(result.products()).containsExactly("leche");
    }

    @Test
    void parse_jsonWithSurroundingProse_parsesCorrectly() throws Exception {
        String text = """
                Aquí tienes el resultado:
                {"products": ["leche"], "suggestions": "Bien"}
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
                {"products": "no es lista", "suggestions": "Bien"}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class);
    }

    @Test
    void parse_missingSuggestions_throwsAnalysisException() {
        String json = """
                {"products": ["leche"]}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_blankSuggestions_throwsAnalysisException() {
        String json = """
                {"products": ["leche"], "suggestions": "   "}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_emptyProductsList_returnsResponse() throws Exception {
        String json = """
                {"products": [], "suggestions": "No encontré productos"}
                """;
        AnalyzeResponse result = parser.parse(json);
        assertThat(result.products()).isEmpty();
        assertThat(result.suggestions()).isEqualTo("No encontré productos");
    }

    @Test
    void parse_missingProductsField_returnsEmptyProducts() throws Exception {
        String json = """
                {"suggestions": "Algo"}
                """;
        AnalyzeResponse result = parser.parse(json);
        assertThat(result.products()).isNull();
        assertThat(result.suggestions()).isEqualTo("Algo");
    }
}
