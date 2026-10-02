package com.aira.app.domain.engine

import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place
import com.aira.app.domain.model.SavedPlace
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MINUTE = 60_000L

class CommuteDetectorTest {

    private val home = SavedPlace("Home", 22.5726, 88.3639)
    private val college = SavedPlace("College", 22.5958, 88.2636)

    private fun atHome(minute: Int, feels: Double = 30.0, rain: Double = 0.0, place: Place = Place.INDOOR,
                       movement: Movement = Movement.STILL, pending: Boolean = false) =
        snap(minute, place, movement, feels = feels, rain = rain, lat = home.latitude, lon = home.longitude, pending = pending)

    private fun atCollege(minute: Int, feels: Double = 30.0) =
        snap(minute, feels = feels, lat = college.latitude, lon = college.longitude)

    private fun onTheRoad(minute: Int, feels: Double = 30.0, rain: Double = 0.0, movement: Movement = Movement.VEHICLE,
                          place: Place = Place.UNKNOWN) =
        snap(minute, place, movement, feels = feels, rain = rain, lat = 22.585, lon = 88.31)

    private fun detect(vararg snapshots: com.aira.app.domain.model.Snapshot) =
        CommuteDetector.detect(snapshots.toList(), home, college)

    @Test
    fun `a trip from home to college is a commute`() {
        val commutes = detect(atHome(0), onTheRoad(30), atCollege(60))
        assertEquals(1, commutes.size)
        assertEquals("Home", commutes[0].fromPlace)
        assertEquals("College", commutes[0].toPlace)
        assertEquals(0L, commutes[0].startTime)
        assertEquals(60 * MINUTE, commutes[0].endTime)
    }

    @Test
    fun `the trip back is found too`() {
        val commutes = detect(atHome(0), onTheRoad(30), atCollege(60), atCollege(300), onTheRoad(330), atHome(360))
        assertEquals(listOf("Home" to "College", "College" to "Home"), commutes.map { it.fromPlace to it.toPlace })
    }

    @Test
    fun `a trip that takes more than 2 hours is not a commute`() =
        assertTrue(detect(atHome(0), onTheRoad(90), atCollege(180)).isEmpty())

    @Test
    fun `exactly 2 hours is still a commute`() =
        assertEquals(1, detect(atHome(0), onTheRoad(60), atCollege(120)).size)

    @Test
    fun `staying at home is not a commute`() =
        assertTrue(detect(atHome(0), atHome(30), atHome(60)).isEmpty())

    @Test
    fun `leaving home and coming back is not a commute`() =
        assertTrue(detect(atHome(0), onTheRoad(30), atHome(60)).isEmpty())

    @Test
    fun `the trip starts at the last snapshot at the first place`() {
        val commutes = detect(atHome(0), atHome(30), atHome(60), onTheRoad(90), atCollege(120))
        assertEquals(60 * MINUTE, commutes.single().startTime)
    }

    @Test
    fun `rain on the way is recorded`() {
        val walkingInRain = onTheRoad(30, rain = 2.0, movement = Movement.WALKING, place = Place.OUTDOOR)
        assertTrue(detect(atHome(0), walkingInRain, atCollege(60)).single().hadRain)
    }

    @Test
    fun `rain while sitting at home is not rain on the commute`() {
        assertFalse(detect(atHome(0, rain = 5.0), onTheRoad(30), atCollege(60)).single().hadRain)
    }

    @Test
    fun `max feels like is the highest on the trip`() {
        val commute = detect(atHome(0, feels = 30.0), onTheRoad(30, feels = 38.0), atCollege(60, feels = 35.0)).single()
        assertEquals(38.0, commute.maxFeelsLike, 0.0)
    }

    @Test
    fun `snapshots waiting for weather are ignored for rain and heat`() {
        val pending = atHome(0, feels = 50.0, rain = 9.0, place = Place.OUTDOOR, pending = true)
        val commute = detect(pending, onTheRoad(30, feels = 33.0), atCollege(60, feels = 32.0)).single()
        assertFalse(commute.hadRain)
        assertEquals(33.0, commute.maxFeelsLike, 0.0)
    }

    @Test
    fun `a spot 250 m from home counts as home`() {
        val nearHome = snap(0, lat = home.latitude + 0.00225, lon = home.longitude)
        assertEquals(1, detect(nearHome, atCollege(60)).size)
    }

    @Test
    fun `a spot 400 m from home does not count as home`() {
        val notHome = snap(0, lat = home.latitude + 0.0036, lon = home.longitude)
        assertTrue(detect(notHome, atCollege(60)).isEmpty())
    }

    @Test
    fun `snapshots in any order give the same result`() {
        val ordered = detect(atHome(0), onTheRoad(30), atCollege(60))
        val shuffled = detect(atCollege(60), atHome(0), onTheRoad(30))
        assertEquals(ordered, shuffled)
    }

    @Test
    fun `no snapshots gives no commutes`() = assertTrue(detect().isEmpty())

    @Test
    fun `the demo day has a commute each way and neither was in the rain`() {
        val demo = DemoData.day(LocalDate.of(2026, 9, 29), ZoneOffset.UTC)
        val homePlace = SavedPlace("Home", DemoData.HOME.latitude, DemoData.HOME.longitude)
        val collegePlace = SavedPlace("College", DemoData.COLLEGE.latitude, DemoData.COLLEGE.longitude)
        val commutes = CommuteDetector.detect(demo, homePlace, collegePlace)
        assertEquals(listOf("Home" to "College", "College" to "Home"), commutes.map { it.fromPlace to it.toPlace })
        assertTrue(commutes.none { it.hadRain })
        assertTrue(commutes.all { it.maxFeelsLike >= 35.0 })
    }
}
