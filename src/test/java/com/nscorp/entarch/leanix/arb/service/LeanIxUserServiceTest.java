/**
 * Unit tests of the name matching of LeanIxUserService.
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests how names are turned into keys for matching a manager to a LeanIX user.
 */
class LeanIxUserServiceTest {

    /** The key of "First Last" is the first and last name in lower case. */
    @Test
    void aFirstAndLastNameMakesTheKey() {
        assertEquals("matteo|toniolo", LeanIxUserService.nameKey("Matteo Toniolo"));
    }

    /** "Last, First" gives the same key as "First Last". */
    @Test
    void lastCommaFirstGivesTheSameKey() {
        assertEquals(LeanIxUserService.nameKey("Charlie Turnipseed"), LeanIxUserService.nameKey("Turnipseed, Charlie"));
    }

    /** A middle name or initial is ignored, as is punctuation. */
    @Test
    void aMiddleNameOrInitialIsIgnored() {
        assertEquals("mary|smith", LeanIxUserService.nameKey("Mary Ann Smith"));
        assertEquals("chris|hudson", LeanIxUserService.nameKey("Hudson, Chris C"));
    }

    /** A name without a first and last name has no key. */
    @Test
    void aSingleWordOrBlankNameHasNoKey() {
        assertNull(LeanIxUserService.nameKey("Madonna"));
        assertNull(LeanIxUserService.nameKey("  "));
        assertNull(LeanIxUserService.nameKey(null));
    }
}
