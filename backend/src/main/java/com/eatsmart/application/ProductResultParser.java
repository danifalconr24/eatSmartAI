package com.eatsmart.application;

import java.io.IOException;

import org.jboss.logging.Logger;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.exception.UnreadableReceiptException;
import com.eatsmart.domain.model.ProductAnalyzeResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Maps the raw text returned by an AI provider for a product photo into the
 * domain model. Tolerant with markdown fences and surrounding prose; strict
 * with the expected JSON shape.
 */
@ApplicationScoped
public class ProductResultParser {

    private static final Logger LOG = Logger.getLogger(ProductResultParser.class);
    private static final int MAX_LOG_RAW = 500;

    @Inject
    ObjectMapper mapper;

    public ProductAnalyzeResponse parse(String text) throws UnreadableReceiptException, AnalysisException {
        JsonNode result = parseJsonObject(text);
        if (result == null) {
            LOG.warnf("Provider response is not valid JSON: %s", truncate(text));
            throw new AnalysisException("El proveedor devolvió una respuesta no interpretable.", null);
        }

        if (result.hasNonNull("error")) {
            String errorMsg = result.path("error").asText();
            LOG.infof("Product not recognizable according to provider: %s", errorMsg);
            throw new UnreadableReceiptException(errorMsg);
        }

        String product = result.path("product").asText("");
        if (product.isBlank()) {
            throw new AnalysisException("El proveedor devolvió una respuesta incompleta.", null);
        }
        String nutrition = result.path("nutrition").asText("");
        if (nutrition.isBlank()) {
            throw new AnalysisException("El proveedor devolvió una respuesta incompleta.", null);
        }
        JsonNode scoreNode = result.path("score");
        if (!scoreNode.isNumber()) {
            LOG.warnf("Provider 'score' field is not a number: %s", truncate(text));
            throw new AnalysisException("El proveedor devolvió una respuesta incompleta.", null);
        }
        int score = Math.clamp(scoreNode.intValue(), 0, 10);

        ProductAnalyzeResponse.Alternative alternative = null;
        JsonNode altNode = result.path("alternative");
        if (altNode.isObject()) {
            String altName = altNode.path("name").asText("");
            String altReason = altNode.path("reason").asText("");
            if (!altName.isBlank()) {
                alternative = new ProductAnalyzeResponse.Alternative(altName, altReason);
            }
        }
        return new ProductAnalyzeResponse(product, score, nutrition, alternative);
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
