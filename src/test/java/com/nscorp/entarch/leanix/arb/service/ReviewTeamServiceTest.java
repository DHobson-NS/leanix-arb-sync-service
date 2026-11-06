/**
 * ReviewTeamServiceTest.java
 *
 * Unit tests of the name matching of ReviewTeamService.
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
package com.nscorp.entarch.leanix.arb.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Tests of the name handling of {@link ReviewTeamService}. */
class ReviewTeamServiceTest {

    /** The "Last; First" form of an Initiative drops the middle initial. */
    @Test
    void splitsLastSemicolonFirst() {
        assertArrayEquals(new String[] {"Logan", "Moore"}, ReviewTeamService.splitName("Moore; Logan A."));
        assertArrayEquals(new String[] {"Chris", "Hudson"}, ReviewTeamService.splitName("Hudson; Chris C"));
    }

    /** A leading initial is skipped in favour of the name used. */
    @Test
    void skipsLeadingInitial() {
        assertArrayEquals(new String[] {"Hugh", "Starling"}, ReviewTeamService.splitName("Starling; V. Hugh"));
    }

    /** The other forms of a name work too. */
    @Test
    void splitsOtherForms() {
        assertArrayEquals(new String[] {"Judy", "Reece"}, ReviewTeamService.splitName("Reece, Judy"));
        assertArrayEquals(new String[] {"Judy", "Reece"}, ReviewTeamService.splitName("Judy Reece"));
    }

    /** A blank or one-word name has no first and last name. */
    @Test
    void rejectsIncompleteNames() {
        assertNull(ReviewTeamService.splitName(null));
        assertNull(ReviewTeamService.splitName("  "));
        assertNull(ReviewTeamService.splitName("Madonna"));
    }

    /** A name is read from an address of the form first.last@host, ignoring digits. */
    @Test
    void readsNameFromEmail() {
        assertArrayEquals(new String[] {"Kiran", "Chahar"}, ReviewTeamService.nameFromEmail("kiran.chahar3@nscorp.com"));
        assertNull(ReviewTeamService.nameFromEmail("support@nscorp.com"));
    }
}
