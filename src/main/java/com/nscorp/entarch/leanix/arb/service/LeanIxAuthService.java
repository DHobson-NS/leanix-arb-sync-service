/**
 * Obtains and caches the LeanIX bearer token.
 * <p>
 * Exchanges the API key for a token with the OAuth2 client_credentials flow and reuses it until one minute before it expires.
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
package com.nscorp.entarch.leanix.arb.service;

import com.nscorp.entarch.leanix.arb.client.LeanIxAuthClient;
import com.nscorp.entarch.leanix.arb.config.LeanIxConfig;
import com.nscorp.entarch.leanix.arb.model.TokenResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/** Provides a valid LeanIX bearer token, refreshing it when needed. */
@ApplicationScoped
public class LeanIxAuthService {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(LeanIxAuthService.class);

    /** LeanIX connection settings. */
    @Inject
    LeanIxConfig config;

    /** REST client of the token endpoint. */
    @RestClient
    LeanIxAuthClient authClient;

    /** The access token currently in use, or null if none was requested yet. */
    private volatile String cachedToken;
    /** When the cached token expires. */
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    /**
     * Returns the Authorization header value for a LeanIX call.
     *
     * @return  "Bearer " followed by a valid access token; a new token is requested if the cached one is missing or about to expire.
     * @throws IllegalStateException If the token endpoint returns an empty token.
     */
    public String getBearerToken() {
        if (cachedToken == null || Instant.now().isAfter(tokenExpiresAt.minusSeconds(60))) {
            refreshToken();
        }
        return "Bearer " + cachedToken;
    }

    /**
     * Requests a new token unless another thread already did.
     *
     * @throws IllegalStateException If the token endpoint returns an empty token.
     */
    private synchronized void refreshToken() {
        // Double-checked locking: another thread may have refreshed while we waited
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return;
        }

        LOG.infof("Requesting OAuth token — POST https://%s/services/mtm/v1/oauth2/token (user=%s)",
                config.host(), config.apiUser());

        String credentials = "apitoken:" + config.apiToken();
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.add("grant_type", "client_credentials");

        TokenResponse response = authClient.getToken(basicAuth, form);

        if (response.accessToken == null || response.accessToken.isBlank()) {
            throw new IllegalStateException("LeanIX token endpoint returned an empty access_token");
        }

        cachedToken = response.accessToken;
        tokenExpiresAt = Instant.now().plusSeconds(response.expiresIn);
        LOG.infof("OAuth token acquired — expires in %d s (at %s)", response.expiresIn, tokenExpiresAt);
    }
}
