package com.eatsmart.application;

import java.util.List;

import com.eatsmart.domain.model.ShoppingListCategory;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Builds the Spanish nutritionist prompt for shopping list generation.
 * Pure business logic: no infrastructure dependencies.
 */
@ApplicationScoped
public class ShoppingListPromptBuilder {

    public String build(List<String> products, String suggestions, String goal,
            boolean budgetMatters, String allergies, String dietPreference) {
        String goalText = switch (goal == null ? "" : goal) {
            case "LOSE" -> "Perder peso";
            case "GAIN" -> "Ganar peso";
            default -> "Mantenerme";
        };
        String dietText = switch (dietPreference == null ? "" : dietPreference) {
            case "VEGETARIAN" -> "Vegetariano";
            case "VEGAN" -> "Vegano";
            case "OTHER" -> "Otra";
            default -> "Sin preferencia";
        };
        String allergiesText = (allergies == null || allergies.isBlank()) ? "Ninguna" : allergies.trim();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres un nutricionista experto que genera listas de la compra para usuarios españoles.\n\n");
        prompt.append("CONTEXTO: el usuario ya ha escaneado un ticket de supermercado. ")
                .append("A partir de los productos detectados y las sugerencias de mejora, ")
                .append("genera la lista de la compra ideal para su próxima visita.\n\n");
        prompt.append("PRODUCTOS DETECTADOS EN EL TICKET:\n");
        for (String product : products) {
            prompt.append("- ").append(product).append('\n');
        }
        prompt.append("\nSUGERENCIAS DEL ANÁLISIS PREVIO (markdown libre, úsalo como contexto):\n");
        prompt.append(suggestions).append("\n\n");
        prompt.append("PERFIL DEL USUARIO:\n");
        prompt.append("- Objetivo: ").append(goalText).append('\n');
        prompt.append("- ¿Le importa el presupuesto?: ").append(budgetMatters ? "Sí" : "No").append('\n');
        prompt.append("- Alergias / intolerancias: ").append(allergiesText).append('\n');
        prompt.append("- Preferencia dietética: ").append(dietText).append("\n\n");
        prompt.append("TAREAS:\n");
        prompt.append("1. KEEP: conserva los productos del ticket que ya son apropiados para el perfil.\n");
        prompt.append("2. REPLACE: sustituye productos poco saludables o inadecuados por alternativas concretas, ")
                .append("siguiendo las sugerencias del análisis. Indica siempre qué producto reemplazan y por qué.\n");
        prompt.append("3. ADD: añade artículos necesarios que faltan en el ticket y son coherentes con el objetivo ")
                .append("(grupos de alimentos ausentes, básicos de despensa, etc.).\n\n");
        prompt.append("REGLAS ESTRICTAS:\n");
        prompt.append("- NUNCA incluyas un producto que contenga un alérgeno o intolerancia indicada.\n");
        prompt.append("- Respeta la preferencia dietética: si es vegetariano, nada de carne o pescado; si es vegano, ")
                .append("ningún producto de origen animal.\n");
        prompt.append("- Solo nombres de artículos: PROHIBIDO incluir cantidades, pesos, unidades o marcas con formato ")
                .append("de cantidad (nada de \"2 kg de manzanas\"; escribe \"Manzanas\").\n");
        prompt.append("- Sin artículos duplicados en toda la lista.\n");
        prompt.append("- Cada artículo debe pertenecer a UNA de estas categorías, escritas exactamente así:\n");
        for (String category : ShoppingListCategory.ALLOWED_NAMES) {
            prompt.append("  - ").append(category).append('\n');
        }
        prompt.append("- Omite categorías sin artículos.\n");
        if (budgetMatters) {
            prompt.append("- Prioriza alternativas económicas (marca blanca, productos de temporada).\n");
        }
        prompt.append("\nFORMATO DE RESPUESTA: responde ÚNICAMENTE con un objeto JSON válido. OBLIGATORIO:\n");
        prompt.append("- Nada de texto antes o después del JSON. Sin bloques ``` ni explicaciones.\n");
        prompt.append("- Comillas dobles siempre; sin comas finales; sin comentarios.\n");
        prompt.append("{\n");
        prompt.append("  \"categories\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": \"Fruta y verdura\",\n");
        prompt.append("      \"items\": [\n");
        prompt.append("        {\"name\": \"Manzanas\", \"type\": \"KEEP\", \"replaces\": null, \"reason\": null},\n");
        prompt.append("        {\"name\": \"Yogur natural sin azúcar\", \"type\": \"REPLACE\", ")
                .append("\"replaces\": \"Yogur azucarado\", \"reason\": \"Menos azúcar añadido\"}\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("- \"type\" solo puede ser KEEP, REPLACE o ADD.\n");
        prompt.append("- Para REPLACE, \"replaces\" y \"reason\" son obligatorios; para KEEP y ADD deben ser null.\n");
        prompt.append("- La lista debe contener al menos un artículo en total.\n");
        return prompt.toString();
    }
}
