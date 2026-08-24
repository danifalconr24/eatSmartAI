package com.eatsmart.infrastructure.gemini;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.eatsmart.infrastructure.gemini.dto.GeminiGenerateRequest;
import com.eatsmart.infrastructure.gemini.dto.GeminiGenerateResponse;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@RegisterRestClient(configKey = "gemini")
@ClientHeaderParam(name = "x-goog-api-key", value = "${gemini.api.key}")
public interface GeminiClient {

    @POST
    @Path("/v1beta/models/{model}:generateContent")
    GeminiGenerateResponse generate(@PathParam("model") String model, GeminiGenerateRequest request);
}
