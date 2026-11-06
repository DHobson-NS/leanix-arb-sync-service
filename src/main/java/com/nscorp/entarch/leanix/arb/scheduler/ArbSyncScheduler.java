/**
 * ArbSyncScheduler.java
 *
 * Runs the ARB sync when its schedule says so.
 * <p>
 * Wakes up every minute and asks the configuration service whether the ARB_SYNC schedule is due. Without that schedule, or if the
 * configuration service cannot be reached, the sync does not run.
 *
 * @author <a href="mailto:darrell.hobson@nscorp.com">Darrell L. Hobson</a>
 * @version 1.0.0
 * @since 1.0.0 (2026-10-30)
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
package com.nscorp.entarch.leanix.arb.scheduler;

import com.nscorp.entarch.leanix.arb.client.ConfigServiceClient;
import com.nscorp.entarch.leanix.arb.service.ArbSyncService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Runs the ARB sync when its schedule says so.
 * <p>
 * Wakes up every minute. The schedule (how often the sync runs, an optional start time, and whether it runs at all) is configuration
 * kept by the configuration service and edited on the admin site; there is no schedule in this service. Without that schedule the
 * sync does not run.
 */
@ApplicationScoped
public class ArbSyncScheduler {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(ArbSyncScheduler.class);

    /** Service that performs the sync. */
    @Inject
    ArbSyncService arbSyncService;

    /** The configuration service, which owns the schedule. */
    @Inject
    @RestClient
    ConfigServiceClient config;

    /** Name of the schedule of this service. */
    static final String SERVICE_NAME = "ARB_SYNC";

    /**
     * Checks once a minute whether the ARB sync is due and, if so, runs it.
     * <p>
     * Records the start of the run before it begins, so a slow run is not started a second time. Any exception is logged and
     * does not stop the scheduler.
     */
    @Scheduled(
            cron = "0 * * * * ?",
            identity = "arb-sync-tick",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP
    )
    void tick() {
        try {
            if (!config.schedule(SERVICE_NAME).due()) {
                return;
            }
            config.startRun(SERVICE_NAME);
            LOG.info("Scheduler triggered ARB sync");
            arbSyncService.sync();
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                LOG.debugf("No %s schedule in the configuration service; the ARB sync does not run", SERVICE_NAME);
            } else {
                LOG.warnf("Could not read the %s schedule (HTTP %d); the ARB sync does not run this time", SERVICE_NAME, e.getResponse().getStatus());
            }
        } catch (ProcessingException e) {
            LOG.warnf("The configuration service cannot be reached (%s); the ARB sync does not run this time", e.getMessage());
        } catch (Exception e) {
            LOG.error("Unhandled exception during ARB sync", e);
        }
    }
}
