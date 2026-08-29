package com.eatsmart.infrastructure.gemini;

import java.util.Base64;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.eatsmart.application.port.ProductAnalysisGateway;
import com.eatsmart.application.port.ReceiptAnalysisGateway;
import com.eatsmart.application.port.ShoppingListGenerationGateway;
import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.infrastructure.gemini.dto.GeminiGenerateRequest;
import com.eatsmart.infrastructure.gemini.dto.GeminiGenerateResponse;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

/**
 * Fallback provider: Google Gemini generateContent API.
 */
@ApplicationScoped
@Priority(2)
public class GeminiGateway
        implements ReceiptAnalysisGateway, ProductAnalysisGateway, ShoppingListGenerationGateway {

    private static final Logger LOG = Logger.getLogger(GeminiGateway.class);
    private static final double TEMPERATURE = 0.3;

    @ConfigProperty(name = "gemini.api.key")
    String apiKey;

    @ConfigProperty(name = "gemini.model", defaultValue = "gemini-3.6-flash")
    String model;

    @RestClient
    GeminiClient client;

    @Override
    public String name() {
        return "Gemini";
    }

    @Override
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String generateText(String prompt) throws AnalysisException {
        GeminiGenerateRequest request = new GeminiGenerateRequest(
                List.of(new GeminiGenerateRequest.Content(List.of(
                        GeminiGenerateRequest.Part.text(prompt)))),
                new GeminiGenerateRequest.GenerationConfig("application/json", TEMPERATURE));
        return call(request);
    }

    @Override
    public String analyze(byte[] imageBytes, String mimeType, String prompt) throws AnalysisException {
        GeminiGenerateRequest request = new GeminiGenerateRequest(
                List.of(new GeminiGenerateRequest.Content(List.of(
                        GeminiGenerateRequest.Part.image(mimeType, Base64.getEncoder().encodeToString(imageBytes)),
                        GeminiGenerateRequest.Part.text(prompt)))),
                new GeminiGenerateRequest.GenerationConfig("application/json", TEMPERATURE));
        return call(request);
    }

    private String call(GeminiGenerateRequest request) throws AnalysisException {
        GeminiGenerateResponse response;
        try {
            response = client.generate(model, request);
        } catch (WebApplicationException e) {
            LOG.warnf("Gemini respondió %d: %s",
                    e.getResponse() != null ? e.getResponse().getStatus() : -1, responseBody(e));
            throw new AnalysisException("Gemini rechazó la petición de análisis.", e);
        } catch (Exception e) {
            // Cualquier fallo técnico (timeout, transporte, serialización) dispara el fallback.
            LOG.warn("Error de comunicación con Gemini", e);
            throw new AnalysisException("No se pudo conectar con Gemini.", e);
        }

        String text = response != null ? response.firstText() : null;
        if (text == null || text.isBlank()) {
            throw new AnalysisException("Gemini devolvió una respuesta vacía.", null);
        }
        return text;
    }

    private static String responseBody(WebApplicationException e) {
        try {
            return e.getResponse() != null ? e.getResponse().readEntity(String.class) : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
