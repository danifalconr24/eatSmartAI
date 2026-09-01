package com.eatsmart.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.eatsmart.domain.model.ChatContext;

class ChatPromptBuilderTest {

    private final ChatPromptBuilder builder = new ChatPromptBuilder();

    private static ChatContext receiptContext() {
        return new ChatContext(
                List.of("leche entera", "pan blanco"), "## Mejoras\npan blanco → pan integral",
                null, null, 5, "LOSE", true, "gluten", "VEGAN");
    }

    private static ChatContext productContext() {
        return new ChatContext(
                null, null, "Yogur azucarado", "Mucho azúcar añadido", 4,
                "MAINTAIN", false, "", "NONE");
    }

    @Test
    void build_receiptContext_includesProductsSuggestionsAndScore() {
        String prompt = builder.build(receiptContext());

        assertThat(prompt).contains("ticket de supermercado");
        assertThat(prompt).contains("- leche entera");
        assertThat(prompt).contains("- pan blanco");
        assertThat(prompt).contains("pan blanco → pan integral");
        assertThat(prompt).contains("5/10");
    }

    @Test
    void build_productContext_includesProductNutritionAndScore() {
        String prompt = builder.build(productContext());

        assertThat(prompt).contains("producto escaneado");
        assertThat(prompt).contains("Yogur azucarado");
        assertThat(prompt).contains("Mucho azúcar añadido");
        assertThat(prompt).contains("4/10");
    }

    @Test
    void build_includesUserProfile() {
        String prompt = builder.build(receiptContext());

        assertThat(prompt).contains("Perder peso");
        assertThat(prompt).contains("¿Le importa el presupuesto?: Sí");
        assertThat(prompt).contains("gluten");
        assertThat(prompt).contains("Vegano");
    }

    @Test
    void build_defaultsWhenProfileMissing() {
        ChatContext context = new ChatContext(
                List.of("pan"), "sugerencias", null, null, null, null, false, null, null);

        String prompt = builder.build(context);

        assertThat(prompt).contains("Mantenerme");
        assertThat(prompt).contains("Ninguna");
        assertThat(prompt).contains("Sin preferencia");
        assertThat(prompt).doesNotContain("/10");
    }

    @Test
    void build_includesSafetyRules() {
        String prompt = builder.build(receiptContext());

        assertThat(prompt).contains("NUNCA recomiendes algo que contenga un alérgeno");
        assertThat(prompt).contains("siempre en español");
    }
}
