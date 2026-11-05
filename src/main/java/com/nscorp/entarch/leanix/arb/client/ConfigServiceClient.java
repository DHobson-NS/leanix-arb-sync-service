/**
 * REST client for the configuration service (leanix-config-service).
 *
 * @author <a href="mailto:darrell.hobson@nscorp.com">Darrell L. Hobson</a>
 * @version 1.0.0
 * @since 2026-11-05
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

import com.nscorp.entarch.leanix.arb.entity.ReviewTeam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * The part of the API of the configuration service that arb-sync uses: its own schedule and the review teams. Every call carries the
 * API token of that service as a bearer token.
 */
@RegisterRestClient(configKey = "config-service")
@ClientHeaderParam(name = "Authorization", value = "Bearer ${config-service.token}")
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ConfigServiceClient {

    /**
     * The schedule of a service.
     *
     * @param serviceName  The service, for example ARB_SYNC.
     * @param due          True if the service should start a run now.
     */
    record Schedule(String serviceName, boolean due) {
    }

    /**
     * Reads the schedule of a service.
     *
     * @param serviceName  The service.
     * @return              The schedule.
     * @throws jakarta.ws.rs.WebApplicationException If the service has no schedule (404) or the call fails.
     */
    @GET
    @Path("/schedules/{serviceName}")
    Schedule schedule(@PathParam("serviceName") String serviceName);

    /**
     * Records that the service starts a run now.
     *
     * @param serviceName  The service.
     */
    @POST
    @Path("/schedules/{serviceName}/runs")
    void startRun(@PathParam("serviceName") String serviceName);

    /**
     * Lists the review teams.
     *
     * @return  One team per manager.
     */
    @GET
    @Path("/review-teams")
    List<ReviewTeam> reviewTeams();

    /**
     * Saves a review team; every column is replaced.
     *
     * @param id    The id of the team.
     * @param team  The team with all its values.
     * @return       The saved team.
     */
    @PUT
    @Path("/review-teams/{id}")
    ReviewTeam updateReviewTeam(@PathParam("id") int id, ReviewTeam team);
}
