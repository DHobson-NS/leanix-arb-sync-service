/**
 * ApiTokenFilterTest.java
 *
 * Tests for the ApiTokenFilter bearer token checks.
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the check of the bearer token. */
class ApiTokenFilterTest {

    @Test
    void acceptsTheToken() {
        assertTrue(ApiTokenFilter.allowed("s3cret", "Bearer s3cret"));
    }

    @Test
    void rejectsAWrongMissingOrMalformedToken() {
        assertFalse(ApiTokenFilter.allowed("s3cret", "Bearer other"));
        assertFalse(ApiTokenFilter.allowed("s3cret", "s3cret"));
        assertFalse(ApiTokenFilter.allowed("s3cret", "Basic czNjcmV0"));
        assertFalse(ApiTokenFilter.allowed("s3cret", null));
    }

    @Test
    void rejectsEverythingWhenNoTokenIsConfigured() {
        assertFalse(ApiTokenFilter.allowed("", "Bearer "));
        assertFalse(ApiTokenFilter.allowed(null, "Bearer x"));
    }
}
