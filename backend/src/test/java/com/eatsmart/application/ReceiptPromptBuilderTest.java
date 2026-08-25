package com.eatsmart.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReceiptPromptBuilderTest {

    private ReceiptPromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ReceiptPromptBuilder();
    }

    @Test
    void build_withLoseGoal_containsPerderPeso() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("Perder peso");
    }

    @Test
    void build_withGainGoal_containsGanarPeso() {
        String prompt = builder.build("GAIN", false, "", "NONE");
        assertThat(prompt).contains("Ganar peso");
    }

    @Test
    void build_withMaintainGoal_containsMantenerme() {
        String prompt = builder.build("MAINTAIN", false, "", "NONE");
        assertThat(prompt).contains("Mantenerme");
    }

    @Test
    void build_withUnknownGoal_defaultsToMantenerme() {
        String prompt = builder.build("UNKNOWN", false, "", "NONE");
        assertThat(prompt).contains("Mantenerme");
    }

    @Test
    void build_withBudgetMattersTrue_containsBudgetSection() {
        String prompt = builder.build("LOSE", true, "", "NONE");
        assertThat(prompt).contains("## Optimización de presupuesto");
    }

    @Test
    void build_withBudgetMattersFalse_doesNotContainBudgetSection() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).doesNotContain("## Optimización de presupuesto");
    }

    @Test
    void build_withAllergies_containsAllergies() {
        String prompt = builder.build("LOSE", false, "Gluten, Lactosa", "NONE");
        assertThat(prompt).contains("Gluten, Lactosa");
    }

    @Test
    void build_withNullAllergies_containsNinguna() {
        String prompt = builder.build("LOSE", false, null, "NONE");
        assertThat(prompt).contains("Ninguna");
    }

    @Test
    void build_withBlankAllergies_containsNinguna() {
        String prompt = builder.build("LOSE", false, "   ", "NONE");
        assertThat(prompt).contains("Ninguna");
    }

    @Test
    void build_withVegetarianDiet_containsVegetariano() {
        String prompt = builder.build("LOSE", false, "", "VEGETARIAN");
        assertThat(prompt).contains("Vegetariano");
    }

    @Test
    void build_withVeganDiet_containsVegano() {
        String prompt = builder.build("LOSE", false, "", "VEGAN");
        assertThat(prompt).contains("Vegano");
    }

    @Test
    void build_withOtherDiet_containsOtra() {
        String prompt = builder.build("LOSE", false, "", "OTHER");
        assertThat(prompt).contains("Otra");
    }

    @Test
    void build_withNoneDiet_containsSinPreferencia() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("Sin preferencia");
    }

    @Test
    void build_containsRequiredJsonFormat() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("\"products\"");
        assertThat(prompt).contains("\"suggestions\"");
    }

    @Test
    void build_containsErrorFormat() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("{\"error\":");
    }

    @Test
    void build_containsStrictRules() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("REGLAS ESTRICTAS");
        assertThat(prompt).contains("NUNCA sugieras un producto que contenga un alérgeno");
    }

    @Test
    void build_withBudgetMattersTrue_containsPresupuestoSection() {
        String prompt = builder.build("LOSE", true, "", "NONE");
        assertThat(prompt).contains("Alternativas más baratas");
    }

    @Test
    void build_withBudgetMattersFalse_doesNotContainPresupuestoSection() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).doesNotContain("Alternativas más baratas");
    }
}
