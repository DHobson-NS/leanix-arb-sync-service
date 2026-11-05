/**
 * Configuration of the connection to LeanIX.
 * <p>
 * Mapped from the properties with the prefix "leanix" (leanix.host, leanix.api.token,
 * leanix.api.user and leanix.tag.arb-phase-open), which are filled from environment variables.
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
package com.nscorp.entarch.leanix.arb.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

/** LeanIX connection settings read from the application configuration. */
@ConfigMapping(prefix = "leanix")
public interface LeanIxConfig {

    /**
     * Returns the LeanIX host name.
     *
     * @return  The host name without protocol, for example "mycompany.leanix.net".
     */
    String host();

    /**
     * Returns the LeanIX API key.
     *
     * @return  The API key that is exchanged for a bearer token; it is a secret and never logged.
     */
    @WithName("api.token")
    String apiToken();

    /**
     * Returns the name of the LeanIX user the key belongs to.
     *
     * @return  The user name, used only in log messages.
     */
    @WithName("api.user")
    String apiUser();

    /**
     * Returns the id of the LeanIX tag that marks an open ARB phase.
     *
     * @return  The tag id applied to a new ArchitectureReview fact sheet.
     */
    @WithName("tag.arb-phase-open")
    String tagArbPhaseOpen();
}
