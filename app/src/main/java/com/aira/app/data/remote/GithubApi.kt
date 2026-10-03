package com.aira.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

/** GitHub's API for the app's releases (public, no key). Used only when the user taps "Check for updates". */
interface GithubApi {

    /** The newest published release of the Aira repository. */
    @GET("repos/$OWNER/$REPO/releases/latest")
    suspend fun latestRelease(): GithubRelease

    companion object {
        const val BASE_URL = "https://api.github.com/"
        const val OWNER = "Argha2004"
        const val REPO = "Aira"
    }
}

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tag: String,
    val name: String? = null,
    /** The release notes the developer wrote. */
    val body: String? = null,
    @SerialName("html_url") val pageUrl: String,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
)
