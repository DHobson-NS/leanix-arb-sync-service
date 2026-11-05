/**
 * REST client for the LeanIX OAuth2 token endpoint.
 * <p>
 * Quarkus REST client registered under the config key "leanix-auth"; its base URL comes
 * from quarkus.rest-client.leanix-auth.url. Exchanges the LeanIX API key for a bearer token
 * using the OAuth2 client_credentials flow (Basic auth, username "apitoken").
 *
 * @author <a href="mailto:darrell.hobson@nscorp.com">Darrell L. Hobson</a>
 * @version 1.0.0
 * @since 2026-10-30
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
package com.nscorp.entarch.leanix.arb.client;

import com.nscorp.entarch.leanix.arb.model.TokenResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for the LeanIX OAuth2 token endpoint.
 * <p>
 * Sends requests to POST /services/mtm/v1/oauth2/token on the host configured for "leanix-auth".
 */
@RegisterRestClient(configKey = "leanix-auth")
@Path("/services/mtm/v1")
public interface LeanIxAuthClient {

    /**
     * Requests an OAuth2 access token from LeanIX.
     * <p>
     * Uses the client_credentials flow. LeanIX expects Basic auth with the username
     * "apitoken" and the API key as the password, and a form body of
     * grant_type=client_credentials.
     *
     * @param basicAuth  The complete Authorization header value ("Basic " + base64 credentials).
     * @param form       The form parameters to send; must contain grant_type=client_credentials.
     * @return            The token response holding the access token and its lifetime in seconds.
     * @throws jakarta.ws.rs.WebApplicationException If LeanIX answers with a non-success status.
     */
    @POST
    @Path("/oauth2/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    TokenResponse getToken(
            @HeaderParam("Authorization") String basicAuth,
            MultivaluedMap<String, String> form);
}
