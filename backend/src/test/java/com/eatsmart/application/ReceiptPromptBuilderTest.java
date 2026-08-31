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
        assertThat(prompt).contains("\"score\"");
    }

    @Test
    void build_definesScoreRange() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("número entero de 0 a 10");
    }

    @Test
    void build_requiresShortSummary() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("2-3 frases como máximo");
    }

    @Test
    void build_forbidsMarkdownFences() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("Sin bloques ```");
    }

    @Test
    void build_containsErrorFormat() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("{\"error\":");
    }

    @Test
    void build_containsMandatoryValidationSection() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("VALIDACIÓN (OBLIGATORIA, HAZLA ANTES QUE NADA)");
    }

    @Test
    void build_validationListsNonTicketExamples() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("una persona");
        assertThat(prompt).contains("un producto suelto sin ticket");
        assertThat(prompt).contains("captura de pantalla");
        assertThat(prompt).contains("ilegible");
    }

    @Test
    void build_validationContainsRejectionExamples() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("Ejemplos de rechazo:");
        assertThat(prompt).contains("Foto de una persona → {\"error\":");
    }

    @Test
    void build_validationErrorMessageIsSpanishAndActionable() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("No se detecta un ticket de supermercado en la imagen");
        assertThat(prompt).contains("inténtalo de nuevo");
    }

    @Test
    void build_finalReminderForbidsAnalysisOnInvalidImage() {
        String prompt = builder.build("LOSE", false, "", "NONE");
        assertThat(prompt).contains("no extraigas productos ni des sugerencias");
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
