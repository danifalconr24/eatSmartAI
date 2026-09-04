package com.eatsmart.infrastructure.rest;

import java.io.IOException;
import java.util.UUID;

import org.jboss.logging.MDC;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Adds a short per-request correlation id to the logging MDC, so every log
 * line emitted while handling a request (use cases, gateways, parsers) can be
 * traced back to that request. The id is echoed to the client via the
 * {@code X-Request-Id} response header to ease support troubleshooting.
 */
@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION - 100)
public class RequestIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    static final String MDC_KEY = "requestId";
    static final String HEADER = "X-Request-Id";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_KEY, requestId);
        requestContext.setProperty(MDC_KEY, requestId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        Object requestId = requestContext.getProperty(MDC_KEY);
        if (requestId != null) {
            responseContext.getHeaders().putSingle(HEADER, requestId.toString());
        }
        MDC.remove(MDC_KEY);
    }
}
