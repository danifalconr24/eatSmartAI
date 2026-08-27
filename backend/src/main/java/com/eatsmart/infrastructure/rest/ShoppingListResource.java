package com.eatsmart.infrastructure.rest;

import java.util.List;
import java.util.Set;

import com.eatsmart.application.GenerateShoppingListUseCase;
import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.model.ShoppingList;

import com.eatsmart.infrastructure.rest.dto.ErrorResponse;
import com.eatsmart.infrastructure.rest.dto.GenerateShoppingListRequest;
import com.eatsmart.infrastructure.rest.dto.GenerateShoppingListResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/shopping-lists")
@Produces(MediaType.APPLICATION_JSON)
public class ShoppingListResource {

    private static final Set<String> VALID_GOALS = Set.of("LOSE", "MAINTAIN", "GAIN");
    private static final Set<String> VALID_DIETS = Set.of("NONE", "VEGETARIAN", "VEGAN", "OTHER");

    @Inject
    GenerateShoppingListUseCase generateShoppingList;

    @POST
    @Path("/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generate(GenerateShoppingListRequest request) {
        if (request == null) {
            return badRequest("Cuerpo de la petición vacío o no válido.");
        }
        List<String> products = request.products() == null
                ? List.of()
                : request.products().stream().map(String::trim).filter(p -> !p.isEmpty()).toList();
        if (products.isEmpty()) {
            return badRequest("Faltan los productos del ticket. Envía una lista no vacía en 'products'.");
        }
        String suggestions = request.suggestions();
        if (suggestions == null || suggestions.isBlank()) {
            return badRequest("Faltan las sugerencias del análisis. Envía un texto no vacío en 'suggestions'.");
        }
        String goal = request.goal();
        if (goal == null || !VALID_GOALS.contains(goal)) {
            return badRequest("Objetivo no válido. Valores admitidos: LOSE, MAINTAIN, GAIN.");
        }
        String diet = request.dietPreference();
        diet = diet == null ? "NONE" : diet;
        if (!VALID_DIETS.contains(diet)) {
            return badRequest("Preferencia dietética no válida. Valores admitidos: NONE, VEGETARIAN, VEGAN, OTHER.");
        }
        String allergies = request.allergies() == null ? "" : request.allergies();

        try {
            ShoppingList result = generateShoppingList.generate(
                    products, suggestions, goal, request.budgetMatters(), allergies, diet);
            return Response.ok(GenerateShoppingListResponse.from(result)).build();
        } catch (AnalysisException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(message))
                .build();
    }
}
