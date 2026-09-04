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
    private static final int MAX_LOG_RAW = 500;

    @Inject
    ObjectMapper mapper;

    public AnalyzeResponse parse(String text) throws UnreadableReceiptException, AnalysisException {
        JsonNode result = parseJsonObject(text);
        if (result == null) {
            LOG.warnf("Provider response is not valid JSON: %s", truncate(text));
            throw new AnalysisException("El proveedor devolvió una respuesta no interpretable.", null);
        }

        if (result.hasNonNull("error")) {
            String errorMsg = result.path("error").asText();
            LOG.infof("Receipt unreadable according to provider: %s", errorMsg);
            throw new UnreadableReceiptException(errorMsg);
        }

        List<String> products;
        try {
            products = mapper.convertValue(result.path("products"),
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (IllegalArgumentException e) {
            LOG.warnf("Provider 'products' field is not a valid list: %s", truncate(text));
            throw new AnalysisException("El proveedor devolvió una respuesta no interpretable.", e);
        }
        if (products == null || products.isEmpty()) {
            LOG.infof("Provider detected no products and did not reject the image: %s", truncate(text));
            throw new UnreadableReceiptException(
                    "No se detecta un ticket de supermercado en la imagen. Haz una foto clara y completa del ticket de compra e inténtalo de nuevo.");
        }
        String suggestions = result.path("suggestions").asText("");
        if (suggestions.isBlank()) {
            throw new AnalysisException("El proveedor devolvió una respuesta incompleta.", null);
        }
        JsonNode scoreNode = result.path("score");
        if (!scoreNode.isNumber()) {
            LOG.warnf("Provider 'score' field is not a number: %s", truncate(text));
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

    private static String truncate(String text) {
        if (text == null || text.length() <= MAX_LOG_RAW) {
            return text;
        }
        return text.substring(0, MAX_LOG_RAW) + "...";
    }
}
