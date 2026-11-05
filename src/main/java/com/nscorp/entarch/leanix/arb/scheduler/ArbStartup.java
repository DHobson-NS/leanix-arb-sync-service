/**
 * Fills in the Initiative names of older reviews once the service has started.
 * <p>
 * Runs on its own thread so a slow LeanIX call does not hold up startup, and only reads from LeanIX.
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
package com.nscorp.entarch.leanix.arb.scheduler;

import com.nscorp.entarch.leanix.arb.service.ArbSyncService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/** Starts the filling in of Initiative names when the application starts. */
@ApplicationScoped
public class ArbStartup {

    /** Service that fills in the names. */
    @Inject
    ArbSyncService arbSyncService;

    /**
     * Starts a background thread that fills in the missing Initiative names.
     *
     * @param event  The Quarkus startup event.
     */
    void onStart(@Observes StartupEvent event) {
        Thread thread = new Thread(arbSyncService::fillInitiativeNames, "arb-fill-names");
        thread.setDaemon(true);
        thread.start();
    }
}
