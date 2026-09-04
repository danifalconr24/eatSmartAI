package com.eatsmart.infrastructure.gemini;

import java.util.Base64;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.eatsmart.application.port.ChatGateway;
import com.eatsmart.application.port.ProductAnalysisGateway;
import com.eatsmart.application.port.ReceiptAnalysisGateway;
import com.eatsmart.application.port.ShoppingListGenerationGateway;
import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.model.ChatMessage;
import com.eatsmart.infrastructure.gemini.dto.GeminiGenerateRequest;
import com.eatsmart.infrastructure.gemini.dto.GeminiGenerateResponse;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

/**
 * Fallback provider: Google Gemini generateContent API.
 */
@ApplicationScoped
@Priority(1)
public class GeminiGateway
        implements ReceiptAnalysisGateway, ProductAnalysisGateway, ShoppingListGenerationGateway, ChatGateway {

    private static final Logger LOG = Logger.getLogger(GeminiGateway.class);
    private static final double TEMPERATURE = 0.3;
    private static final int MAX_LOG_BODY = 500;

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
                List.of(GeminiGenerateRequest.Content.of(List.of(
                        GeminiGenerateRequest.Part.text(prompt)))),
                new GeminiGenerateRequest.GenerationConfig("application/json", TEMPERATURE));
        return call(request);
    }

    @Override
    public String analyze(byte[] imageBytes, String mimeType, String prompt) throws AnalysisException {
        GeminiGenerateRequest request = new GeminiGenerateRequest(
                List.of(GeminiGenerateRequest.Content.of(List.of(
                        GeminiGenerateRequest.Part.image(mimeType, Base64.getEncoder().encodeToString(imageBytes)),
                        GeminiGenerateRequest.Part.text(prompt)))),
                new GeminiGenerateRequest.GenerationConfig("application/json", TEMPERATURE));
        return call(request);
    }

    @Override
    public String chat(String systemPrompt, List<ChatMessage> history, String question) throws AnalysisException {
        // Gemini generateContent has no system role in this DTO: the system
        // prompt travels as the first user turn. Chat needs free-text output,
        // so no responseMimeType is forced here.
        List<GeminiGenerateRequest.Content> contents = new java.util.ArrayList<>();
        contents.add(GeminiGenerateRequest.Content.user(List.of(
                GeminiGenerateRequest.Part.text(systemPrompt))));
        for (ChatMessage message : history) {
            var part = GeminiGenerateRequest.Part.text(message.content());
            contents.add(ChatMessage.ROLE_ASSISTANT.equals(message.role())
                    ? GeminiGenerateRequest.Content.model(List.of(part))
                    : GeminiGenerateRequest.Content.user(List.of(part)));
        }
        contents.add(GeminiGenerateRequest.Content.user(List.of(
                GeminiGenerateRequest.Part.text(question))));
        return call(new GeminiGenerateRequest(
                contents, new GeminiGenerateRequest.GenerationConfig(null, TEMPERATURE)));
    }

    private String call(GeminiGenerateRequest request) throws AnalysisException {
        GeminiGenerateResponse response;
        long start = System.nanoTime();
        try {
            response = client.generate(model, request);
        } catch (WebApplicationException e) {
            int status = e.getResponse() != null ? e.getResponse().getStatus() : -1;
            LOG.warnf("Gemini returned HTTP %d: %s", status, truncate(responseBody(e)));
            throw new AnalysisException("Gemini rechazó la petición de análisis.", e);
        } catch (Exception e) {
            // Any technical failure (timeout, transport, serialization) triggers fallback.
            LOG.warn("Communication error with Gemini", e);
            throw new AnalysisException("No se pudo conectar con Gemini.", e);
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            LOG.debugf("Gemini request completed in %d ms", elapsedMs);
        }

        String text = response != null ? response.firstText() : null;
        if (text == null || text.isBlank()) {
            LOG.warn("Gemini returned an empty response");
            throw new AnalysisException("Gemini devolvió una respuesta vacía.", null);
        }
        LOG.infof("Gemini answered via model '%s'", model);
        return text;
    }

    private static String responseBody(WebApplicationException e) {
        try {
            return e.getResponse() != null ? e.getResponse().readEntity(String.class) : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static String truncate(String text) {
        if (text == null || text.length() <= MAX_LOG_BODY) {
            return text;
        }
        return text.substring(0, MAX_LOG_BODY) + "...";
    }
}
