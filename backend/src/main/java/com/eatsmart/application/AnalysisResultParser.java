package com.eatsmart.application;

import java.io.IOException;
import java.util.List;

import org.jboss.logging.Logger;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.exception.UnreadableReceiptException;
import com.eatsmart.domain.model.AnalyzeResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Maps the raw text returned by an AI provider into the domain model.
 * Tolerant with markdown fences and surrounding prose; strict with the
 * expected JSON shape.
 */
@ApplicationScoped
public class AnalysisResultParser {

    private static final Logger LOG = Logger.getLogger(AnalysisResultParser.class);

    @Inject
    ObjectMapper mapper;

    public AnalyzeResponse parse(String text) throws UnreadableReceiptException, AnalysisException {
        JsonNode result = parseJsonObject(text);
        if (result == null) {
            LOG.warnf("El texto del proveedor no es JSON válido: %s", text);
            throw new AnalysisException("El proveedor devolvió una respuesta no interpretable.", null);
        }

        if (result.hasNonNull("error")) {
            String errorMsg = result.path("error").asText();
            LOG.infof("Ticket no legible según el proveedor: %s", errorMsg);
            throw new UnreadableReceiptException(errorMsg);
        }

        List<String> products;
        try {
            products = mapper.convertValue(result.path("products"),
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (IllegalArgumentException e) {
            LOG.warnf("El campo 'products' del proveedor no es una lista válida: %s", text);
            throw new AnalysisException("El proveedor devolvió una respuesta no interpretable.", e);
        }
        String suggestions = result.path("suggestions").asText("");
        if (suggestions.isBlank()) {
            throw new AnalysisException("El proveedor devolvió una respuesta incompleta.", null);
        }
        JsonNode scoreNode = result.path("score");
        if (!scoreNode.isNumber()) {
            LOG.warnf("El campo 'score' del proveedor no es un número: %s", text);
            throw new AnalysisException("El proveedor devolvió una respuesta incompleta.", null);
        }
        int score = Math.clamp(scoreNode.intValue(), 0, 10);
        return new AnalyzeResponse(products, suggestions, score);
    }

    private JsonNode parseJsonObject(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```(json)?\\s*", "").replaceAll("\\s*```$", "");
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return mapper.readTree(cleaned.substring(start, end + 1));
        } catch (IOException e) {
            return null;
        }
    }
}
