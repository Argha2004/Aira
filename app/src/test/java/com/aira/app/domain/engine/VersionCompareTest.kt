package com.aira.app.domain.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {
    @Test
    fun higherPatchMinorAndMajorAreNewer() {
        assertTrue(VersionCompare.isNewer("1.5.2", "1.5.3"))
        assertTrue(VersionCompare.isNewer("1.5.2", "1.6.0"))
        assertTrue(VersionCompare.isNewer("1.5.2", "2.0.0"))
    }

    @Test
    fun numbersAreComparedAsNumbersNotText() {
        assertTrue(VersionCompare.isNewer("1.9.0", "1.10.0"))
        assertFalse(VersionCompare.isNewer("1.10.0", "1.9.0"))
    }

    @Test
    fun sameOrOlderIsNotNewer() {
        assertFalse(VersionCompare.isNewer("1.5.2", "1.5.2"))
        assertFalse(VersionCompare.isNewer("1.5.2", "1.5.1"))
    }

    @Test
    fun leadingVAndMissingNumbersAreHandled() {
        assertTrue(VersionCompare.isNewer("1.5.2", "v1.5.3"))
        assertFalse(VersionCompare.isNewer("1.6.0", "v1.6"))
        assertTrue(VersionCompare.isNewer("1.5", "1.5.1"))
    }

    @Test
    fun textThatIsNotAVersionIsNeverNewer() {
        assertFalse(VersionCompare.isNewer("1.5.2", "latest"))
        assertFalse(VersionCompare.isNewer("1.5.2", ""))
        assertFalse(VersionCompare.isNewer("beta", "1.0.0"))
    }
}
