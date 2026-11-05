/**
 * REST client for the LeanIX account and user management API.
 * <p>
 * Quarkus REST client registered under the config key "leanix-mtm"; its base URL comes from
 * quarkus.rest-client.leanix-mtm.url. Used to read the users of the workspace.
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for the LeanIX user management API.
 * <p>
 * Sends requests to /services/mtm/v1 on the host configured for "leanix-mtm".
 */
@RegisterRestClient(configKey = "leanix-mtm")
@Path("/services/mtm/v1")
public interface LeanIxMtmClient {

    /**
     * Reads one page of the users of a workspace.
     *
     * @param bearerToken  The complete Authorization header value ("Bearer " + access token).
     * @param workspaceId  The id of the workspace.
     * @param pageSize     The number of users per page.
     * @param page         The page number, starting at 1.
     * @return              The JSON answer, with the users in "data" and their number in "total".
     * @throws jakarta.ws.rs.WebApplicationException If LeanIX answers with a non-success status.
     */
    @GET
    @Path("/workspaces/{workspaceId}/users")
    @Produces(MediaType.APPLICATION_JSON)
    JsonNode users(
            @HeaderParam("Authorization") String bearerToken,
            @PathParam("workspaceId") String workspaceId,
            @QueryParam("pageSize") int pageSize,
            @QueryParam("page") int page);
}
