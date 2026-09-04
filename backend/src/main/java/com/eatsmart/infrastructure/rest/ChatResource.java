package com.eatsmart.infrastructure.rest;

import java.util.List;
import java.util.Set;

import com.eatsmart.application.ChatWithNutritionistUseCase;
import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.model.ChatContext;
import com.eatsmart.domain.model.ChatMessage;

import com.eatsmart.infrastructure.rest.dto.ChatRequest;
import com.eatsmart.infrastructure.rest.dto.ChatResponse;
import com.eatsmart.infrastructure.rest.dto.ErrorResponse;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/api/chat")
@Produces(MediaType.APPLICATION_JSON)
@RunOnVirtualThread
public class ChatResource {

    private static final Logger LOG = Logger.getLogger(ChatResource.class);

    private static final Set<String> VALID_GOALS = Set.of("LOSE", "MAINTAIN", "GAIN");
    private static final Set<String> VALID_DIETS = Set.of("NONE", "VEGETARIAN", "VEGAN", "OTHER");
    private static final Set<String> VALID_ROLES = Set.of(ChatMessage.ROLE_USER, ChatMessage.ROLE_ASSISTANT);
    private static final int MAX_HISTORY = 20;

    @Inject
    ChatWithNutritionistUseCase chatWithNutritionist;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response chat(ChatRequest request) {
        if (request == null) {
            return badRequest("body", "Cuerpo de la petición vacío o no válido.");
        }
        String question = request.question();
        if (question == null || question.isBlank()) {
            return badRequest("question", "Falta la pregunta del usuario. Envía un texto no vacío en 'question'.");
        }

        List<String> products = request.products() == null
                ? List.of()
                : request.products().stream().map(String::trim).filter(p -> !p.isEmpty()).toList();
        boolean isProductChat = request.product() != null && !request.product().isBlank();
        if (isProductChat) {
            if (request.nutrition() == null || request.nutrition().isBlank()) {
                return badRequest("nutrition", "Falta la información nutricional del producto. Envía un texto no vacío en 'nutrition'.");
            }
        } else {
            if (products.isEmpty()) {
                return badRequest("products", "Faltan los productos del ticket. Envía una lista no vacía en 'products'.");
            }
            if (request.suggestions() == null || request.suggestions().isBlank()) {
                return badRequest("suggestions", "Faltan las sugerencias del análisis. Envía un texto no vacío en 'suggestions'.");
            }
        }

        String goal = request.goal();
        if (goal == null || !VALID_GOALS.contains(goal)) {
            return badRequest("goal", "Objetivo no válido. Valores admitidos: LOSE, MAINTAIN, GAIN.");
        }
        String diet = request.dietPreference();
        diet = diet == null ? "NONE" : diet;
        if (!VALID_DIETS.contains(diet)) {
            return badRequest("dietPreference",
                    "Preferencia dietética no válida. Valores admitidos: NONE, VEGETARIAN, VEGAN, OTHER.");
        }
        String allergies = request.allergies() == null ? "" : request.allergies();

        List<ChatMessage> history = List.of();
        if (request.messages() != null) {
            history = request.messages().stream()
                    .filter(m -> m.role() != null && VALID_ROLES.contains(m.role()))
                    .filter(m -> m.content() != null && !m.content().isBlank())
                    .map(m -> new ChatMessage(m.role(), m.content()))
                    .skip(Math.max(0, request.messages().size() - MAX_HISTORY))
                    .toList();
        }

        ChatContext context = new ChatContext(
                products, request.suggestions(), request.product(), request.nutrition(),
                request.score(), goal, request.budgetMatters(), allergies, diet);

        try {
            String answer = chatWithNutritionist.chat(context, history, question.trim());
            return Response.ok(new ChatResponse(answer)).build();
        } catch (AnalysisException e) {
            LOG.error("Nutritionist chat failed after exhausting providers", e);
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    private Response badRequest(String field, String message) {
        LOG.warnf("Bad request rejected [field=%s]", field);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(message))
                .build();
    }
}
