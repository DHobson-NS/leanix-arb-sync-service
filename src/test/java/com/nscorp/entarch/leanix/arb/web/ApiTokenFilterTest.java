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
