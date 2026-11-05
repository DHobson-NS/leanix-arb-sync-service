/**
 * REST client for the LeanIX GraphQL endpoint.
 * <p>
 * Quarkus REST client registered under the config key "leanix-graphql"; its base URL comes
 * from quarkus.rest-client.leanix-graphql.url. Sends every query and mutation of this service
 * to POST /services/pathfinder/v1/graphql.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.nscorp.entarch.leanix.arb.model.GraphQLRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for the LeanIX GraphQL endpoint.
 * <p>
 * Sends requests to POST /services/pathfinder/v1/graphql on the host configured for "leanix-graphql".
 */
@RegisterRestClient(configKey = "leanix-graphql")
@Path("/services/pathfinder/v1")
public interface LeanIxGraphQLClient {

    /**
     * Runs one GraphQL query or mutation.
     * <p>
     * The answer is returned as a JSON tree because the shape differs per query; the caller reads
     * the parts it needs and checks the "errors" element.
     *
     * @param bearerToken  The complete Authorization header value ("Bearer " + access token).
     * @param request      The GraphQL query or mutation and its variables.
     * @return              The complete JSON response of LeanIX, including any "errors" element.
     * @throws jakarta.ws.rs.WebApplicationException If LeanIX answers with a non-success status.
     */
    @POST
    @Path("/graphql")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    JsonNode execute(
            @HeaderParam("Authorization") String bearerToken,
            GraphQLRequest request);
}
