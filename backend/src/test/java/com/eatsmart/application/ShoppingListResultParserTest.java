package com.eatsmart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.model.ShoppingList;
import com.eatsmart.domain.model.ShoppingListItemType;
import com.fasterxml.jackson.databind.ObjectMapper;

class ShoppingListResultParserTest {

    private ShoppingListResultParser parser;

    @BeforeEach
    void setUp() {
        parser = new ShoppingListResultParser();
        parser.mapper = new ObjectMapper();
    }

    private static final String VALID = """
            {
              "categories": [
                {
                  "name": "Fruta y verdura",
                  "items": [
                    {"name": "Manzanas", "type": "KEEP", "replaces": null, "reason": null}
                  ]
                },
                {
                  "name": "Lácteos y alternativas",
                  "items": [
                    {"name": "Yogur natural", "type": "REPLACE", "replaces": "Yogur azucarado", "reason": "Menos azúcar"},
                    {"name": "Leche de avena", "type": "ADD", "replaces": null, "reason": null}
                  ]
                }
              ]
            }
            """;

    @Test
    void parse_validJson_returnsShoppingList() throws Exception {
        ShoppingList list = parser.parse(VALID);

        assertThat(list.categories()).hasSize(2);
        assertThat(list.categories().get(0).name()).isEqualTo("Fruta y verdura");
        assertThat(list.categories().get(0).items().get(0).type()).isEqualTo(ShoppingListItemType.KEEP);
        assertThat(list.categories().get(1).items().get(0).replaces()).isEqualTo("Yogur azucarado");
        assertThat(list.categories().get(1).items().get(1).type()).isEqualTo(ShoppingListItemType.ADD);
    }

    @Test
    void parse_wrappedInMarkdownFence_succeeds() throws Exception {
        ShoppingList list = parser.parse("```json\n" + VALID + "\n```");

        assertThat(list.categories()).isNotEmpty();
    }

    @Test
    void parse_notJson_throwsAnalysisException() {
        assertThatThrownBy(() -> parser.parse("no json at all"))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("no interpretable");
    }

    @Test
    void parse_emptyCategories_throwsAnalysisException() {
        assertThatThrownBy(() -> parser.parse("{\"categories\": []}"))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_unknownCategory_throwsAnalysisException() {
        String json = """
                {"categories": [{"name": "Snacks", "items": [
                    {"name": "Chicles", "type": "ADD", "replaces": null, "reason": null}]}]}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_duplicateItems_throwsAnalysisException() {
        String json = """
                {"categories": [{"name": "Despensa", "items": [
                    {"name": "Arroz", "type": "KEEP", "replaces": null, "reason": null},
                    {"name": "arroz", "type": "ADD", "replaces": null, "reason": null}]}]}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_replaceWithoutReplaces_throwsAnalysisException() {
        String json = """
                {"categories": [{"name": "Despensa", "items": [
                    {"name": "Pan integral", "type": "REPLACE", "replaces": null, "reason": "Más fibra"}]}]}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_replaceWithoutReason_throwsAnalysisException() {
        String json = """
                {"categories": [{"name": "Despensa", "items": [
                    {"name": "Pan integral", "type": "REPLACE", "replaces": "Pan blanco", "reason": null}]}]}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_keepWithReplaces_throwsAnalysisException() {
        String json = """
                {"categories": [{"name": "Despensa", "items": [
                    {"name": "Arroz", "type": "KEEP", "replaces": "Pasta", "reason": null}]}]}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_unknownType_throwsAnalysisException() {
        String json = """
                {"categories": [{"name": "Despensa", "items": [
                    {"name": "Arroz", "type": "MAYBE", "replaces": null, "reason": null}]}]}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }

    @Test
    void parse_emptyItemName_throwsAnalysisException() {
        String json = """
                {"categories": [{"name": "Despensa", "items": [
                    {"name": " ", "type": "KEEP", "replaces": null, "reason": null}]}]}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("incompleta");
    }
}
