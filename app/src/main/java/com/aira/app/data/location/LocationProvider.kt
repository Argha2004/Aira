package com.aira.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.usecase.LocationSource
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Gets the phone's coarse (city-block level) location. Returns null instead of throwing. */
@Singleton
class LocationProvider @Inject constructor(@ApplicationContext private val context: Context) : LocationSource {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * "Allow all the time". Android 10+ asks for it separately, and without it the background job cannot get
     * a location. Before Android 10 the normal location permission already covers the background.
     */
    fun hasBackgroundPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Current location, or the last known one; null if no permission or no fix in time. */
    override suspend fun getCurrentLocation(): GeoPoint? {
        if (!hasPermission()) return null
        return try {
            withTimeoutOrNull(TIMEOUT_MS) { current() ?: lastKnown() }
        } catch (e: SecurityException) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun current(): GeoPoint? = suspendCancellableCoroutine { cont ->
        val cancel = CancellationTokenSource()
        cont.invokeOnCancellation { cancel.cancel() }
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancel.token)
            .addOnSuccessListener { cont.resume(it?.toGeoPoint()) }
            .addOnFailureListener { cont.resume(null) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastKnown(): GeoPoint? = suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { cont.resume(it?.toGeoPoint()) }
            .addOnFailureListener { cont.resume(null) }
    }

    private fun Location.toGeoPoint() = GeoPoint(latitude, longitude)

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
