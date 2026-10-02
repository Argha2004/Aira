package com.aira.app.domain.engine

import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.model.Snapshot
import kotlin.math.round

/** A frequently visited spot and how many snapshots were taken there. */
data class PlaceCandidate(val point: GeoPoint, val snapshotCount: Int)

/** A suggestion for each place that is not set yet (null = nothing to suggest). */
data class PlaceSuggestions(val home: PlaceCandidate?, val college: PlaceCandidate?)

/** Suggests Home and College from where the phone spends the most snapshots. Pure Kotlin. */
object PlaceSuggester {

    /**
     * Suggestions for the places that are still unset. The most visited spot is offered first; a spot
     * within 300 m of the other place (already set, or suggested for it) is never offered.
     */
    fun suggestFor(candidates: List<PlaceCandidate>, home: GeoPoint?, college: GeoPoint?): PlaceSuggestions {
        fun pick(avoid: GeoPoint?) = candidates.firstOrNull {
            avoid == null || GeoMath.distanceKm(it.point, avoid) * 1000 >= Thresholds.COMMUTE_RADIUS_M
        }
        val homeSuggestion = if (home == null) pick(college) else null
        val collegeSuggestion = if (college == null) pick(home ?: homeSuggestion?.point) else null
        return PlaceSuggestions(homeSuggestion, collegeSuggestion)
    }


    /**
     * The [limit] locations with the most snapshots. Coordinates are rounded to 3 decimals
     * (about 100 m) and counted. A spot within the commute radius of a more popular one is skipped,
     * so the suggestions are really different places.
     */
    fun candidates(snapshots: List<Snapshot>, limit: Int = 2): List<PlaceCandidate> {
        val counts = snapshots.groupingBy { round3(it.latitude) to round3(it.longitude) }.eachCount()
        val result = mutableListOf<PlaceCandidate>()
        for ((spot, count) in counts.entries.sortedByDescending { it.value }.map { it.key to it.value }) {
            val point = GeoPoint(spot.first, spot.second)
            val tooClose = result.any { GeoMath.distanceKm(it.point, point) * 1000 < Thresholds.COMMUTE_RADIUS_M }
            if (!tooClose) result += PlaceCandidate(point, count)
            if (result.size == limit) break
        }
        return result
    }

    private fun round3(value: Double) = round(value * 1000) / 1000
}
