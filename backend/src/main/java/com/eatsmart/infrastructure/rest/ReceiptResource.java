package com.eatsmart.infrastructure.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

import com.eatsmart.infrastructure.rest.dto.ErrorResponse;
import io.smallrye.common.annotation.RunOnVirtualThread;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.multipart.FileItem;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

import com.eatsmart.application.AnalyzeReceiptUseCase;
import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.exception.UnreadableReceiptException;
import com.eatsmart.domain.model.AnalyzeResponse;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/analyze")
@Produces(MediaType.APPLICATION_JSON)
@RunOnVirtualThread
public class ReceiptResource {

    private static final Logger LOG = Logger.getLogger(ReceiptResource.class);

    private static final Set<String> VALID_GOALS = Set.of("LOSE", "MAINTAIN", "GAIN");
    private static final Set<String> VALID_DIETS = Set.of("NONE", "VEGETARIAN", "VEGAN", "OTHER");
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    @Inject
    AnalyzeReceiptUseCase analyzeReceipt;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response analyze(MultipartFormDataInput input) {
        FileItem image = fileValue(input, "image");
        if (image == null) {
            return badRequest("image", "Falta la imagen del ticket. Envía una foto en el campo 'image'.");
        }

        String goal = textValue(input, "goal");
        if (goal == null || !VALID_GOALS.contains(goal)) {
            return badRequest("goal", "Objetivo no válido. Valores admitidos: LOSE, MAINTAIN, GAIN.");
        }
        String diet = textValue(input, "dietPreference");
        diet = diet == null ? "NONE" : diet;
        if (!VALID_DIETS.contains(diet)) {
            return badRequest("dietPreference",
                    "Preferencia dietética no válida. Valores admitidos: NONE, VEGETARIAN, VEGAN, OTHER.");
        }
        boolean budgetMatters = Boolean.parseBoolean(textValue(input, "budgetMatters"));
        String allergies = textValue(input, "allergies");
        allergies = allergies == null ? "" : allergies;

        byte[] imageBytes;
        try {
            if (image.getFileSize() > MAX_IMAGE_BYTES) {
                return badRequest("image", "La imagen es demasiado grande (máximo 10 MB).");
            }
            imageBytes = readImageBytes(image);
        } catch (IOException e) {
            LOG.error("Failed to read uploaded image", e);
            return badRequest("image", "No se pudo leer la imagen enviada.");
        }

        try {
            AnalyzeResponse result = analyzeReceipt.analyze(
                    imageBytes, "image/jpeg", goal, budgetMatters, allergies, diet);
            return Response.ok(result).build();
        } catch (UnreadableReceiptException e) {
            // Business rejection (unreadable receipt): surfaced to the user, not a service failure.
            LOG.infof("Receipt rejected by business rule: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (AnalysisException e) {
            // Technical failure after exhausting providers: real service error.
            LOG.error("Receipt analysis failed after exhausting providers", e);
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    private static byte[] readImageBytes(FileItem image) throws IOException {
        try (InputStream in = image.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private static String textValue(MultipartFormDataInput input, String name) {
        Collection<FormValue> values = input.getValues().get(name);
        if (values == null) {
            return null;
        }
        return values.stream().filter(v -> !v.isFileItem()).map(FormValue::getValue)
                .findFirst().orElse(null);
    }

    private static FileItem fileValue(MultipartFormDataInput input, String name) {
        Collection<FormValue> values = input.getValues().get(name);
        if (values == null) {
            return null;
        }
        return values.stream().filter(FormValue::isFileItem).map(FormValue::getFileItem)
                .findFirst().orElse(null);
    }

    private Response badRequest(String field, String message) {
        LOG.warnf("Bad request rejected [field=%s]", field);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(message))
                .build();
    }
}
