package com.eatsmart.application;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Builds the Spanish nutritionist prompt for receipt analysis.
 * Pure business logic: no infrastructure dependencies.
 */
@ApplicationScoped
public class ReceiptPromptBuilder {

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
        prompt.append("Eres un nutricionista experto que analiza tickets de supermercado españoles ")
                .append("(Mercadona, Carrefour, Lidl, Dia, Alcampo, etc.).\n\n");
        prompt.append("VALIDACIÓN (OBLIGATORIA, HAZLA ANTES QUE NADA):\n");
        prompt.append("Comprueba si la imagen es un ticket o recibo de compra de supermercado.\n");
        prompt.append("NO es un ticket válido si la imagen muestra, por ejemplo: una persona, un selfie, un paisaje, ")
                .append("un producto suelto sin ticket, una captura de pantalla, un documento que no sea de compra, ")
                .append("o una foto demasiado borrosa, oscura o ilegible.\n");
        prompt.append("Si NO es un ticket de supermercado legible, responde ÚNICAMENTE con:\n");
        prompt.append("{\"error\": \"No se detecta un ticket de supermercado en la imagen. Haz una foto clara y completa del ticket de compra e inténtalo de nuevo.\"}\n");
        prompt.append("Ejemplos de rechazo:\n");
        prompt.append("- Foto de una persona → {\"error\": \"No se detecta un ticket de supermercado en la imagen. Haz una foto clara y completa del ticket de compra e inténtalo de nuevo.\"}\n");
        prompt.append("- Foto de un producto sin ticket → {\"error\": \"La imagen muestra un producto, no un ticket. Haz una foto del ticket de compra completo e inténtalo de nuevo.\"}\n");
        prompt.append("- Imagen borrosa o ilegible → {\"error\": \"El ticket no se lee con claridad. Haz la foto con buena luz, sin arrugas y enfocando todo el ticket.\"}\n");
        prompt.append("Si SÍ es un ticket legible, continúa con las tareas.\n\n");
        prompt.append("TAREAS:\n");
        prompt.append("1. Extrae la lista de productos comprados del ticket de la imagen. ")
                .append("Ignora líneas de precios sueltos, descuentos, programas de fidelidad, totales, IVA y fechas. ")
                .append("Quédate solo con los nombres de los productos, normalizados en español ")
                .append("(por ejemplo, \"L.ENTERA PASCUAL 1L\" → \"leche entera\").\n");
        prompt.append("2. Asigna a la compra una puntuación de saludabilidad de 0 a 10, teniendo en cuenta ")
                .append("el perfil del usuario (0 = muy poco saludable, 10 = excelente).\n\n");
        prompt.append("PERFIL DEL USUARIO:\n");
        prompt.append("- Objetivo: ").append(goalText).append('\n');
        prompt.append("- ¿Le importa el presupuesto?: ").append(budgetMatters ? "Sí" : "No").append('\n');
        prompt.append("- Alergias / intolerancias: ").append(allergiesText).append('\n');
        prompt.append("- Preferencia dietética: ").append(dietText).append("\n\n");
        prompt.append("REGLAS ESTRICTAS:\n");
        prompt.append("- NUNCA sugieras un producto que contenga un alérgeno o intolerancia indicada.\n");
        prompt.append("- Respeta la preferencia dietética: si es vegetariano, nada de carne o pescado; si es vegano, ")
                .append("ningún producto de origen animal.\n");
        prompt.append("- Referencia productos reales detectados en el ticket al proponer cambios.\n");
        prompt.append("- Tono cercano y práctico, en español de España.\n\n");
        prompt.append("FORMATO DE RESPUESTA: responde ÚNICAMENTE con un objeto JSON válido. OBLIGATORIO:\n");
        prompt.append("- Nada de texto antes o después del JSON. Sin bloques ``` ni explicaciones.\n");
        prompt.append("- Comillas dobles siempre; sin comas finales; sin comentarios.\n");
        prompt.append("- Dentro de \"suggestions\", escapa los saltos de línea como \\n.\n");
        prompt.append("{\n");
        prompt.append("  \"products\": [\"producto 1\", \"producto 2\"],\n");
        prompt.append("  \"score\": 7,\n");
        prompt.append("  \"suggestions\": \"<markdown>\"\n");
        prompt.append("}\n\n");
        prompt.append("- \"score\": número entero de 0 a 10.\n");
        prompt.append("El campo \"suggestions\" debe ser markdown con EXACTAMENTE estas secciones (con ##):\n");
        prompt.append("## Resumen general\n");
        prompt.append("Valoración de la compra en 2-3 frases como máximo: breve y directa.\n");
        prompt.append("## Grupos de alimentos que faltan\n");
        prompt.append("Categorías ausentes o escasas (pescado, legumbres, fruta, verdura, cereales integrales, etc.).\n");
        prompt.append("## Mejoras en tu selección\n");
        prompt.append("Cambios concretos de productos (por ejemplo, \"pan blanco → pan integral\"), ")
                .append("referenciando productos reales del ticket.\n");
        if (budgetMatters) {
            prompt.append("## Optimización de presupuesto\n");
            prompt.append("Alternativas más baratas, cambios de marca (marca blanca), consejos de compra a granel.\n");
        }
        prompt.append("\nRECUERDA: si la imagen NO es un ticket de supermercado legible, responde SOLO con el JSON de error ");
        prompt.append("definido en VALIDACIÓN; no extraigas productos ni des sugerencias.\n");
        return prompt.toString();
    }
}
