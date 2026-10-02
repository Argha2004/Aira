package com.aira.app.domain.engine

import com.aira.app.domain.model.Commute
import com.aira.app.domain.model.SavedPlace
import com.aira.app.domain.model.Snapshot

/** Finds trips between Home and College in a day's snapshots. Pure Kotlin. */
object CommuteDetector {

    /**
     * A commute is a trip that leaves from within 300 m of one place and arrives within 300 m of the
     * other place no more than 2 hours later. [first] and [second] are the two places (Home and College,
     * in either order); trips in both directions are found. For each trip we record whether the user
     * met rain on the way and the highest feels-like temperature.
     */
    fun detect(snapshots: List<Snapshot>, first: SavedPlace, second: SavedPlace): List<Commute> {
        val sorted = snapshots.sortedBy { it.timestamp }
        val places = listOf(first, second)
        val radiusKm = Thresholds.COMMUTE_RADIUS_M / 1000

        fun placeIndexAt(s: Snapshot): Int? =
            places.indices.firstOrNull { GeoMath.distanceKm(s.latitude, s.longitude, places[it].latitude, places[it].longitude) <= radiusKm }

        val commutes = mutableListOf<Commute>()
        var lastIndex = -1 // index of the newest snapshot that was at one of the places
        var lastPlace = -1
        sorted.forEachIndexed { i, snapshot ->
            val here = placeIndexAt(snapshot) ?: return@forEachIndexed
            val tookOffFromOtherPlace = lastIndex >= 0 && here != lastPlace &&
                snapshot.timestamp - sorted[lastIndex].timestamp <= Thresholds.COMMUTE_MAX_MS
            if (tookOffFromOtherPlace) {
                commutes += buildCommute(sorted.subList(lastIndex, i + 1), places[lastPlace], places[here])
            }
            lastIndex = i
            lastPlace = here
        }
        return commutes
    }

    private fun buildCommute(trip: List<Snapshot>, from: SavedPlace, to: SavedPlace): Commute {
        // Snapshots waiting for weather have placeholder numbers, so they are ignored.
        val withWeather = trip.filter { !it.weatherPending }
        return Commute(
            fromPlace = from.label,
            toPlace = to.label,
            startTime = trip.first().timestamp,
            endTime = trip.last().timestamp,
            hadRain = withWeather.any { ExposureRules.isRainEncounter(it.place, it.movement, it.rainMm) },
            maxFeelsLike = withWeather.maxOfOrNull { it.feelsLike } ?: 0.0,
        )
    }
}
