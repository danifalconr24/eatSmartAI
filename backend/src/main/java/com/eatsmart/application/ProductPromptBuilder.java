package com.eatsmart.application;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Builds the Spanish nutritionist prompt for single-product analysis.
 * Pure business logic: no infrastructure dependencies.
 */
@ApplicationScoped
public class ProductPromptBuilder {

    public String build(String goal, boolean budgetMatters, String allergies, String dietPreference) {
        String goalText = switch (goal) {
            case "LOSE" -> "Perder peso";
            case "GAIN" -> "Ganar peso";
            default -> "Mantenerme";
        };
        String dietText = switch (dietPreference) {
            case "VEGETARIAN" -> "Vegetariano";
            case "VEGAN" -> "Vegano";
            case "OTHER" -> "Otra";
            default -> "Sin preferencia";
        };
        String allergiesText = (allergies == null || allergies.isBlank()) ? "Ninguna" : allergies.trim();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres un nutricionista experto que analiza productos de supermercado españoles ")
                .append("(Mercadona, Carrefour, Lidl, Dia, Alcampo, etc.) a partir de una foto del producto, ")
                .append("su envase o su etiqueta nutricional.\n\n");
        prompt.append("VALIDACIÓN (OBLIGATORIA, HAZLA ANTES QUE NADA):\n");
        prompt.append("Comprueba si la imagen muestra un producto de supermercado reconocible ")
                .append("(envase, etiqueta o producto alimentario identificable).\n");
        prompt.append("NO es un producto válido si la imagen muestra, por ejemplo: una persona, un selfie, un paisaje, ")
                .append("un ticket o recibo de compra, una captura de pantalla, ")
                .append("o una foto demasiado borrosa, oscura o ilegible.\n");
        prompt.append("Si NO muestra un producto de supermercado reconocible, responde ÚNICAMENTE con:\n");
        prompt.append("{\"error\": \"No se detecta un producto de supermercado en la imagen. Haz una foto clara del producto o su etiqueta e inténtalo de nuevo.\"}\n");
        prompt.append("Ejemplos de rechazo:\n");
        prompt.append("- Foto de una persona → {\"error\": \"No se detecta un producto de supermercado en la imagen. Haz una foto clara del producto o su etiqueta e inténtalo de nuevo.\"}\n");
        prompt.append("- Foto de un ticket de compra → {\"error\": \"La imagen muestra un ticket de compra, no un producto. Haz una foto del producto o su etiqueta e inténtalo de nuevo.\"}\n");
        prompt.append("- Imagen borrosa o ilegible → {\"error\": \"El producto no se ve con claridad. Haz la foto con buena luz y enfocando el envase o la etiqueta.\"}\n");
        prompt.append("Si SÍ muestra un producto reconocible, continúa con las tareas.\n\n");
        prompt.append("TAREAS:\n");
        prompt.append("1. Identifica el producto de la imagen y normaliza su nombre en español ")
                .append("(por ejemplo, \"Oreo Original\" → \"galletas oreo\").\n");
        prompt.append("2. Asigna al producto una puntuación de saludabilidad de 0 a 10, teniendo en cuenta ")
                .append("el perfil del usuario (0 = muy poco saludable, 10 = excelente).\n");
        prompt.append("3. Resume su información nutricional: valores aproximados por 100 g ")
                .append("(energía, grasas, grasas saturadas, hidratos, azúcares, proteínas, sal) ")
                .append("y aspectos relevantes (azúcares altos, ultraprocesado, buen aporte de fibra, etc.).\n");
        prompt.append("4. Si la puntuación es inferior a 7, propón un producto similar más saludable ")
                .append("disponible en supermercados españoles. Si es 7 o más, no propongas alternativa.\n\n");
        prompt.append("PERFIL DEL USUARIO:\n");
        prompt.append("- Objetivo: ").append(goalText).append('\n');
        prompt.append("- ¿Le importa el presupuesto?: ").append(budgetMatters ? "Sí" : "No").append('\n');
        prompt.append("- Alergias / intolerancias: ").append(allergiesText).append('\n');
        prompt.append("- Preferencia dietética: ").append(dietText).append("\n\n");
        prompt.append("REGLAS ESTRICTAS:\n");
        prompt.append("- NUNCA sugieras una alternativa que contenga un alérgeno o intolerancia indicada.\n");
        prompt.append("- Respeta la preferencia dietética: si es vegetariano, nada de carne o pescado; si es vegano, ")
                .append("ningún producto de origen animal.\n");
        prompt.append("- La alternativa debe ser del mismo tipo de producto ")
                .append("(por ejemplo, galletas por galletas más saludables, no galletas por fruta).\n");
        if (budgetMatters) {
            prompt.append("- La alternativa debe tener un precio similar o menor; prioriza marcas blancas.\n");
        }
        prompt.append("- Tono cercano y práctico, en español de España.\n\n");
        prompt.append("FORMATO DE RESPUESTA: responde ÚNICAMENTE con un objeto JSON válido. OBLIGATORIO:\n");
        prompt.append("- Nada de texto antes o después del JSON. Sin bloques ``` ni explicaciones.\n");
        prompt.append("- Comillas dobles siempre; sin comas finales; sin comentarios.\n");
        prompt.append("- Dentro de \"nutrition\" y \"reason\", escapa los saltos de línea como \\n.\n");
        prompt.append("{\n");
        prompt.append("  \"product\": \"nombre normalizado del producto\",\n");
        prompt.append("  \"score\": 4,\n");
        prompt.append("  \"nutrition\": \"<markdown>\",\n");
        prompt.append("  \"alternative\": { \"name\": \"producto más saludable\", \"reason\": \"por qué es mejor\" }\n");
        prompt.append("}\n\n");
        prompt.append("- \"score\": número entero de 0 a 10.\n");
        prompt.append("- \"alternative\": objeto con \"name\" y \"reason\" si \"score\" es inferior a 7; null en caso contrario.\n");
        prompt.append("El campo \"nutrition\" debe ser markdown con EXACTAMENTE estas secciones (con ##):\n");
        prompt.append("## Información nutricional\n");
        prompt.append("Tabla o lista breve con los valores aproximados por 100 g.\n");
        prompt.append("## Valoración\n");
        prompt.append("Valoración del producto en 2-3 frases como máximo: breve y directa.\n");
        prompt.append("\nRECUERDA: si la imagen NO muestra un producto de supermercado reconocible, responde SOLO con el JSON ");
        prompt.append("de error definido en VALIDACIÓN; no identifiques producto ni des valoración.\n");
        return prompt.toString();
    }
}
