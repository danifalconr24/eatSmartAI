package com.eatsmart.application;

import com.eatsmart.domain.model.ChatContext;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Builds the Spanish system prompt for the nutritionist chat: role,
 * analysis context (receipt or product) and user profile. The conversation
 * history and the user question travel as chat messages, not in this prompt.
 * Pure business logic: no infrastructure dependencies.
 */
@ApplicationScoped
public class ChatPromptBuilder {

    public String build(ChatContext context) {
        String goalText = switch (context.goal() == null ? "" : context.goal()) {
            case "LOSE" -> "Perder peso";
            case "GAIN" -> "Ganar peso";
            default -> "Mantenerme";
        };
        String dietText = switch (context.dietPreference() == null ? "" : context.dietPreference()) {
            case "VEGETARIAN" -> "Vegetariano";
            case "VEGAN" -> "Vegano";
            case "OTHER" -> "Otra";
            default -> "Sin preferencia";
        };
        String allergiesText = (context.allergies() == null || context.allergies().isBlank())
                ? "Ninguna"
                : context.allergies().trim();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres un nutricionista experto que resuelve dudas a usuarios españoles sobre ")
                .append("el análisis nutricional que acabas de realizarles. Responde siempre en español, ")
                .append("de forma clara, cercana y concisa (máximo ~150 palabras por respuesta). ")
                .append("Puedes usar markdown ligero (negritas, listas) si ayuda a la lectura.\n\n");

        if (context.product() != null && !context.product().isBlank()) {
            prompt.append("ANÁLISIS REALIZADO (producto escaneado):\n");
            prompt.append("- Producto: ").append(context.product()).append('\n');
            if (context.score() != null) {
                prompt.append("- Puntuación: ").append(context.score()).append("/10\n");
            }
            prompt.append("- Información nutricional y valoración:\n");
            prompt.append(context.nutrition()).append('\n');
        } else {
            prompt.append("ANÁLISIS REALIZADO (ticket de supermercado):\n");
            prompt.append("- Productos detectados:\n");
            for (String product : context.products()) {
                prompt.append("  - ").append(product).append('\n');
            }
            if (context.score() != null) {
                prompt.append("- Puntuación global: ").append(context.score()).append("/10\n");
            }
            prompt.append("- Sugerencias de mejora:\n");
            prompt.append(context.suggestions()).append('\n');
        }

        prompt.append("\nPERFIL DEL USUARIO:\n");
        prompt.append("- Objetivo: ").append(goalText).append('\n');
        prompt.append("- ¿Le importa el presupuesto?: ").append(context.budgetMatters() ? "Sí" : "No").append('\n');
        prompt.append("- Alergias / intolerancias: ").append(allergiesText).append('\n');
        prompt.append("- Preferencia dietética: ").append(dietText).append("\n\n");

        prompt.append("REGLAS:\n");
        prompt.append("- Basa tus respuestas en el análisis anterior y el perfil del usuario.\n");
        prompt.append("- NUNCA recomiendes algo que contenga un alérgeno o intolerancia indicada.\n");
        prompt.append("- Respeta siempre la preferencia dietética del usuario.\n");
        prompt.append("- Si te preguntan algo ajeno a nutrición o al análisis, rechaza amablemente ")
                .append("y redirige la conversación al análisis.\n");
        prompt.append("- No inventes datos del análisis: si algo no está en el contexto, dilo.\n");
        return prompt.toString();
    }
}
