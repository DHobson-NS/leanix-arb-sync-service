/**
 * ApiTokenFilter.java
 *
 * Requires the API token on every call under /api.
 *
 * @author <a href="mailto:darrell.hobson@nscorp.com">Darrell L. Hobson</a>
 * @version 1.0.0
 * @since 1.0.0 (2026-11-05)
 *
 * @copyright Copyright 2026 Norfolk Southern Corporation (NS).
 * Title, ownership rights, and intellectual property
 * rights in and to this software remain with NS. NS hereby
 * reserves all rights in and to this software. This software
 * may not be copied or used without a license from NS. This
 * software is protected by international copyright laws and
 * treaties, and may be protected by other law. Violation of
 * copyright laws may result in civil liability and criminal
 * penalties.
 */
package com.nscorp.entarch.leanix.arb.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Lets a call to /api through only if it carries the API token as a bearer token. The health endpoints are not under /api and stay
 * open for the OpenShift probes. The token is a secret shared with the pages of the management service, set in the environment (API_TOKEN); the service
 * does not start without it. Send it only over a network you trust or over TLS.
 */
public class ApiTokenFilter {

    /** The token a caller must present. */
    @ConfigProperty(name = "api.token")
    String token;

    /**
     * Checks the Authorization header of a request.
     *
     * @param context  The request.
     * @return          Null if the request may go on, otherwise a 401 response.
     */
    @ServerRequestFilter
    public Response check(ContainerRequestContext context) {
        if (!context.getUriInfo().getPath().startsWith("/api")) {
            return null;
        }
        return allowed(token, context.getHeaderString("Authorization")) ? null
                : Response.status(401).header("WWW-Authenticate", "Bearer").entity(Map.of("error", "A valid API token is required")).build();
    }

    /**
     * Compares the Authorization header with the token, in constant time.
     *
     * @param token   The expected token.
     * @param header  The Authorization header, or null.
     * @return         True if the header is "Bearer " followed by the token.
     */
    static boolean allowed(String token, String header) {
        if (token == null || token.isBlank() || header == null || !header.startsWith("Bearer ")) {
            return false;
        }
        return MessageDigest.isEqual(header.substring(7).getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }
}
