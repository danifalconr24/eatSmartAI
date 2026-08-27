package com.eatsmart.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.eatsmart.domain.model.ShoppingListCategory;

class ShoppingListPromptBuilderTest {

    private final ShoppingListPromptBuilder builder = new ShoppingListPromptBuilder();

    @Test
    void build_includesProductsSuggestionsAndProfile() {
        String prompt = builder.build(
                List.of("leche entera", "pan blanco"), "## Mejoras\npan blanco → pan integral",
                "LOSE", true, "gluten", "VEGAN");

        assertThat(prompt).contains("- leche entera");
        assertThat(prompt).contains("- pan blanco");
        assertThat(prompt).contains("pan blanco → pan integral");
        assertThat(prompt).contains("Perder peso");
        assertThat(prompt).contains("¿Le importa el presupuesto?: Sí");
        assertThat(prompt).contains("gluten");
        assertThat(prompt).contains("Vegano");
    }

    @Test
    void build_listsAllFixedCategories() {
        String prompt = builder.build(List.of("pan"), "sugerencias", "MAINTAIN", false, "", "NONE");

        for (String category : ShoppingListCategory.ALLOWED_NAMES) {
            assertThat(prompt).contains(category);
        }
    }

    @Test
    void build_requestsStrictJsonAndForbidsQuantities() {
        String prompt = builder.build(List.of("pan"), "sugerencias", null, false, null, null);

        assertThat(prompt).contains("JSON válido");
        assertThat(prompt).contains("PROHIBIDO incluir cantidades");
        assertThat(prompt).contains("KEEP");
        assertThat(prompt).contains("REPLACE");
        assertThat(prompt).contains("ADD");
        assertThat(prompt).contains("Ninguna");
        assertThat(prompt).contains("Mantenerme");
        assertThat(prompt).contains("Sin preferencia");
    }

    @Test
    void build_budgetMatters_addsBudgetRule() {
        String withBudget = builder.build(List.of("pan"), "sug", "MAINTAIN", true, "", "NONE");
        String withoutBudget = builder.build(List.of("pan"), "sug", "MAINTAIN", false, "", "NONE");

        assertThat(withBudget).contains("marca blanca");
        assertThat(withoutBudget).doesNotContain("marca blanca");
    }
}
