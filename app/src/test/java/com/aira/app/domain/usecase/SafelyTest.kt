package com.aira.app.domain.usecase

import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SafelyTest {

    @Test
    fun `a normal result is returned as a success`() {
        assertEquals(42, runCatchingCancellable { 42 }.getOrNull())
    }

    @Test
    fun `an exception becomes a failure instead of being thrown`() {
        val result = runCatchingCancellable { error("boom") }
        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test
    fun `a database style exception is caught too`() {
        assertTrue(runCatchingCancellable { throw java.io.IOException("disk full") }.isFailure)
    }

    @Test
    fun `a cancellation is rethrown so a cancelled job really stops`() {
        assertThrows(CancellationException::class.java) {
            runCatchingCancellable { throw CancellationException("cancelled") }
        }
    }

    @Test
    fun `the block runs only once`() {
        var runs = 0
        runCatchingCancellable { runs++ }
        assertEquals(1, runs)
    }
}
