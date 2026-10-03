package com.aira.app.data.repository

import com.aira.app.data.remote.GithubApi
import com.aira.app.domain.engine.VersionCompare
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** The answer to "is there a newer version?". */
sealed interface UpdateResult {
    data object UpToDate : UpdateResult

    /** [apkUrl] is the APK attached to the release (null if there is none); [pageUrl] is the release page. */
    data class Available(val version: String, val notes: String, val apkUrl: String?, val pageUrl: String) : UpdateResult

    data object Failed : UpdateResult
}

/**
 * Asks GitHub for the newest release and compares its tag (for example "v1.6.0") with the installed version.
 * This is the only place the app talks to GitHub, and only when the user asks.
 */
@Singleton
class UpdateRepository @Inject constructor(private val api: GithubApi) {

    suspend fun check(installedVersion: String): UpdateResult = try {
        val release = api.latestRelease()
        if (VersionCompare.isNewer(installedVersion, release.tag)) {
            UpdateResult.Available(
                version = release.tag.removePrefix("v"),
                notes = release.body.orEmpty().trim(),
                apkUrl = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }?.downloadUrl,
                pageUrl = release.pageUrl,
            )
        } else {
            UpdateResult.UpToDate
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        UpdateResult.Failed
    }
}
