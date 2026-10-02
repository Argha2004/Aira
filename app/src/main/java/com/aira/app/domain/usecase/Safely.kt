package com.aira.app.domain.usecase

import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs [block] and returns its result, or the exception as a failure instead of throwing it.
 * Unlike the standard `runCatching`, a coroutine cancellation is still thrown, because swallowing it
 * would stop a cancelled job from ever stopping. Used where a failure of a side job (a notification button,
 * a timeline rebuild) must not crash the app or stop the main job.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
