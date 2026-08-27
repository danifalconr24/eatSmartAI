package com.eatsmart.infrastructure.openrouter;

import java.util.Base64;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.port.ReceiptAnalysisGateway;
import com.eatsmart.infrastructure.openrouter.dto.OpenRouterChatRequest;
import com.eatsmart.infrastructure.openrouter.dto.OpenRouterChatResponse;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

/**
 * Primary provider: OpenRouter.
 * OpenAI-compatible chat completions API. Sends an ordered fallback list of
 * vision-capable models ({@code openrouter.models}); without it, model routers
 * such as openrouter/free can route to non-vision models (e.g. content-safety
 * classifiers) that return unusable answers.
 */
@ApplicationScoped
@Priority(1)
public class OpenRouterGateway implements ReceiptAnalysisGateway {

    private static final Logger LOG = Logger.getLogger(OpenRouterGateway.class);
    private static final double TEMPERATURE = 0.3;

    @ConfigProperty(name = "openrouter.api.key")
    String apiKey;

    @ConfigProperty(name = "openrouter.models")
    List<String> models;

    @RestClient
    OpenRouterClient client;

    @Override
    public String name() {
        return "OpenRouter";
    }

    @Override
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String generateText(String prompt) throws AnalysisException {
        OpenRouterChatRequest request = new OpenRouterChatRequest(
                models,
                List.of(OpenRouterChatRequest.Message.user(List.of(
                        OpenRouterChatRequest.Content.text(prompt)))),
                TEMPERATURE);
        return call(request);
    }

    @Override
    public String analyze(byte[] imageBytes, String mimeType, String prompt) throws AnalysisException {
        String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        OpenRouterChatRequest request = new OpenRouterChatRequest(
                models,
                List.of(OpenRouterChatRequest.Message.user(List.of(
                        OpenRouterChatRequest.Content.image(dataUrl),
                        OpenRouterChatRequest.Content.text(prompt)))),
                TEMPERATURE);
        return call(request);
    }

    private String call(OpenRouterChatRequest request) throws AnalysisException {
        OpenRouterChatResponse response;
        try {
            response = client.chat(request);
        } catch (WebApplicationException e) {
            LOG.warnf("OpenRouter respondió %d: %s",
                    e.getResponse() != null ? e.getResponse().getStatus() : -1, responseBody(e));
            throw new AnalysisException("OpenRouter rechazó la petición de análisis.", e);
        } catch (Exception e) {
            // Cualquier fallo técnico (timeout, transporte, serialización) dispara el fallback.
            LOG.warn("Error de comunicación con OpenRouter", e);
            throw new AnalysisException("No se pudo conectar con OpenRouter.", e);
        }

        String content = response != null ? response.firstContent() : null;
        if (content == null || content.isBlank()) {
            throw new AnalysisException("OpenRouter devolvió una respuesta vacía.", null);
        }
        LOG.infof("OpenRouter respondió vía modelo '%s'", response.model());
        return content;
    }

    private static String responseBody(WebApplicationException e) {
        try {
            return e.getResponse() != null ? e.getResponse().readEntity(String.class) : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
