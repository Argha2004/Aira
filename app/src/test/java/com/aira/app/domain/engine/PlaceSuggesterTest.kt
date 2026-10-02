package com.aira.app.domain.engine

import com.aira.app.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceSuggesterTest {

    private val homeSpot = GeoPoint(22.573, 88.364)
    private val collegeSpot = GeoPoint(22.596, 88.264)

    private fun snaps(count: Int, lat: Double, lon: Double) = List(count) { snap(it, lat = lat, lon = lon) }

    @Test
    fun `no snapshots give no candidates`() = assertTrue(PlaceSuggester.candidates(emptyList()).isEmpty())

    @Test
    fun `the two most visited spots are returned, most visited first`() {
        val all = snaps(5, 22.5726, 88.3639) + snaps(3, 22.5958, 88.2636) + snaps(1, 22.7, 88.5)
        val result = PlaceSuggester.candidates(all)
        assertEquals(listOf(5, 3), result.map { it.snapshotCount })
        assertEquals(homeSpot.latitude, result[0].point.latitude, 1e-9)
        assertEquals(collegeSpot.longitude, result[1].point.longitude, 1e-9)
    }

    @Test
    fun `coordinates are rounded to 3 decimals before counting`() {
        val all = snaps(2, 22.57261, 88.36391) + snaps(2, 22.57264, 88.36394)
        assertEquals(4, PlaceSuggester.candidates(all).single().snapshotCount)
    }

    @Test
    fun `a spot within 300 m of a more popular one is skipped`() {
        // About 220 m north of the first spot.
        val all = snaps(5, 22.5726, 88.3639) + snaps(4, 22.5750, 88.3639) + snaps(2, 22.5958, 88.2636)
        val result = PlaceSuggester.candidates(all)
        assertEquals(listOf(5, 2), result.map { it.snapshotCount })
    }

    @Test
    fun `fewer spots than the limit gives fewer candidates`() =
        assertEquals(1, PlaceSuggester.candidates(snaps(3, 22.5726, 88.3639)).size)

    // ---- suggestFor ----

    private val candidates = listOf(PlaceCandidate(homeSpot, 50), PlaceCandidate(collegeSpot, 30))

    @Test
    fun `nothing set suggests the most visited for home and the next for college`() {
        val result = PlaceSuggester.suggestFor(candidates, home = null, college = null)
        assertEquals(homeSpot, result.home?.point)
        assertEquals(collegeSpot, result.college?.point)
    }

    @Test
    fun `when home is set only college is suggested and never near home`() {
        val result = PlaceSuggester.suggestFor(candidates, home = homeSpot, college = null)
        assertNull(result.home)
        assertEquals(collegeSpot, result.college?.point)
    }

    @Test
    fun `when college is set at the top spot home gets the other one`() {
        val result = PlaceSuggester.suggestFor(candidates, home = null, college = homeSpot)
        assertEquals(collegeSpot, result.home?.point)
        assertNull(result.college)
    }

    @Test
    fun `when both are set there is nothing to suggest`() {
        val result = PlaceSuggester.suggestFor(candidates, home = homeSpot, college = collegeSpot)
        assertNull(result.home)
        assertNull(result.college)
    }

    @Test
    fun `with no candidates nothing is suggested`() {
        val result = PlaceSuggester.suggestFor(emptyList(), null, null)
        assertNull(result.home)
        assertNull(result.college)
    }
}
