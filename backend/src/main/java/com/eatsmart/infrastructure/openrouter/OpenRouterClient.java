package com.eatsmart.infrastructure.openrouter;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.eatsmart.infrastructure.openrouter.dto.OpenRouterChatRequest;
import com.eatsmart.infrastructure.openrouter.dto.OpenRouterChatResponse;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@RegisterRestClient(configKey = "openrouter")
@ClientHeaderParam(name = "Authorization", value = "Bearer ${openrouter.api.key}")
@ClientHeaderParam(name = "X-Title", value = "eatSmart")
public interface OpenRouterClient {

    @POST
    @Path("/chat/completions")
    OpenRouterChatResponse chat(OpenRouterChatRequest request);
}
