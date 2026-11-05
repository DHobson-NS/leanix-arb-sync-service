/**
 * Answer of the LeanIX OAuth2 token endpoint.
 * <p>
 * Deserialized from the JSON properties access_token, expires_in and token_type.
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
package com.nscorp.entarch.leanix.arb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** OAuth2 token response of LeanIX. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenResponse {

    /** The bearer token to send with every API call. */
    @JsonProperty("access_token")
    public String accessToken;

    /** Lifetime of the token in seconds. */
    @JsonProperty("expires_in")
    public long expiresIn;

    /** Type of the token; "bearer". */
    @JsonProperty("token_type")
    public String tokenType;
}
