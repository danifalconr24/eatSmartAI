package com.eatsmart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.exception.UnreadableReceiptException;
import com.eatsmart.domain.model.ProductAnalyzeResponse;
import com.eatsmart.domain.model.Source;
import com.fasterxml.jackson.databind.ObjectMapper;

class ProductResultParserTest {

    private ProductResultParser parser;

    @BeforeEach
    void setUp() {
        parser = new ProductResultParser();
        parser.mapper = new ObjectMapper();
    }

    @Test
    void parse_validJson_returnsResponse() throws Exception {
        String json = """
                {"product": "galletas oreo", "score": 4, "nutrition": "## Info", "alternative": {"name": "galletas integrales", "reason": "menos azúcar"}}
                """;
        ProductAnalyzeResponse result = parser.parse(json);
        assertThat(result.product()).isEqualTo("galletas oreo");
        assertThat(result.score()).isEqualTo(4);
        assertThat(result.nutrition()).isEqualTo("## Info");
        assertThat(result.alternative()).isNotNull();
        assertThat(result.alternative().name()).isEqualTo("galletas integrales");
        assertThat(result.sources()).isEmpty();
    }

    @Test
    void parse_withSources_returnsSources() throws Exception {
        String json = """
                {"product": "leche", "score": 5, "nutrition": "## Info", \
                "sources": [{"title": "OMS", "url": "https://www.who.int/es/nutrition"}], \
                "alternative": {"name": "soja", "reason": "x", \
                "sources": [{"title": "AESAN", "url": "https://www.aesan.gob.es/AECOSAN/web/home/home.htm"}]}}
                """;
        ProductAnalyzeResponse result = parser.parse(json);
        assertThat(result.sources()).containsExactly(new Source("OMS", "https://www.who.int/es/nutrition"));
        assertThat(result.alternative().sources()).containsExactly(
                new Source("AESAN", "https://www.aesan.gob.es/AECOSAN/web/home/home.htm"));
    }

    @Test
    void parse_invalidSourceUrl_skipsSource() throws Exception {
        String json = """
                {"product": "leche", "score": 5, "nutrition": "## Info", \
                "sources": [{"title": "Mal", "url": "http://inseguro.example"}, {"title": "OMS", "url": "https://www.who.int/es/nutrition"}]}
                """;
        ProductAnalyzeResponse result = parser.parse(json);
        assertThat(result.sources()).containsExactly(new Source("OMS", "https://www.who.int/es/nutrition"));
    }

    @Test
    void parse_noAlternative_returnsNullAlternative() throws Exception {
        String json = """
                {"product": "leche", "score": 8, "nutrition": "## Info"}
                """;
        ProductAnalyzeResponse result = parser.parse(json);
        assertThat(result.alternative()).isNull();
    }

    @Test
    void parse_jsonWithMarkdownFences_parsesCorrectly() throws Exception {
        String text = """
                ```json
                {"product": "yogur", "score": 6, "nutrition": "## Info"}
                ```
                """;
        ProductAnalyzeResponse result = parser.parse(text);
        assertThat(result.product()).isEqualTo("yogur");
    }

    @Test
    void parse_jsonWithSurroundingProse_parsesCorrectly() throws Exception {
        String text = """
                Aquí tienes el resultado:
                {"product": "pan", "score": 7, "nutrition": "## Info"}
                Espero que te sirva.
                """;
        ProductAnalyzeResponse result = parser.parse(text);
        assertThat(result.product()).isEqualTo("pan");
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
                {"error": "No se ve un producto reconocible"}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(UnreadableReceiptException.class)
                .hasMessageContaining("No se ve un producto");
    }

    @Test
    void parse_missingProduct_throwsAnalysisException() {
        String json = """
                {"score": 5, "nutrition": "## Info"}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_blankProduct_throwsAnalysisException() {
        String json = """
                {"product": "  ", "score": 5, "nutrition": "## Info"}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_missingNutrition_throwsAnalysisException() {
        String json = """
                {"product": "leche", "score": 5}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_blankNutrition_throwsAnalysisException() {
        String json = """
                {"product": "leche", "score": 5, "nutrition": "   "}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_missingScore_throwsAnalysisException() {
        String json = """
                {"product": "leche", "nutrition": "## Info"}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_scoreNotNumber_throwsAnalysisException() {
        String json = """
                {"product": "leche", "score": "alto", "nutrition": "## Info"}
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_scoreAboveTen_clampedToTen() throws Exception {
        String json = """
                {"product": "leche", "score": 42, "nutrition": "## Info"}
                """;
        assertThat(parser.parse(json).score()).isEqualTo(10);
    }

    @Test
    void parse_scoreBelowZero_clampedToZero() throws Exception {
        String json = """
                {"product": "leche", "score": -3, "nutrition": "## Info"}
                """;
        assertThat(parser.parse(json).score()).isEqualTo(0);
    }

    @Test
    void parse_decimalScore_truncatedToInt() throws Exception {
        String json = """
                {"product": "leche", "score": 7.8, "nutrition": "## Info"}
                """;
        assertThat(parser.parse(json).score()).isEqualTo(7);
    }

    @Test
    void parse_alternativeBlankName_alternativeIgnored() throws Exception {
        String json = """
                {"product": "leche", "score": 3, "nutrition": "## Info", "alternative": {"name": "  ", "reason": "x"}}
                """;
        ProductAnalyzeResponse result = parser.parse(json);
        assertThat(result.alternative()).isNull();
    }

    @Test
    void parse_alternativeMissingReason_alternativeWithEmptyReason() throws Exception {
        String json = """
                {"product": "leche", "score": 3, "nutrition": "## Info", "alternative": {"name": "soja"}}
                """;
        ProductAnalyzeResponse result = parser.parse(json);
        assertThat(result.alternative()).isNotNull();
        assertThat(result.alternative().name()).isEqualTo("soja");
        assertThat(result.alternative().reason()).isEmpty();
    }
}
