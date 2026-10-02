package com.aira.app.domain.engine

import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place
import com.aira.app.domain.model.Snapshot
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.sin

/**
 * A made-up but realistic day (home, college, a sunny lunch, an afternoon shower, falling pressure)
 * for the viva demo when there is no time to collect real data. Used by the debug-only button in Settings.
 */
object DemoData {

    val HOME = GeoPoint(22.5726, 88.3639)
    val COLLEGE = GeoPoint(22.5958, 88.2636)
    private val ROAD = GeoPoint((HOME.latitude + COLLEGE.latitude) / 2, (HOME.longitude + COLLEGE.longitude) / 2)

    private class Slot(
        val place: Place,
        val movement: Movement,
        val lux: Float,
        val temp: Double,
        val feels: Double,
        val rain: Double = 0.0,
        val steps: Int = 0,
        val at: GeoPoint,
    )

    /** One snapshot every 30 minutes from 06:30 to 22:30 on [date]. */
    fun day(date: LocalDate, zone: ZoneId): List<Snapshot> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        var totalSteps = 12_000L
        return schedule().map { (minute, slot) ->
            totalSteps += slot.steps
            Snapshot(
                timestamp = start + minute * 60_000L,
                latitude = slot.at.latitude,
                longitude = slot.at.longitude,
                temperature = slot.temp,
                feelsLike = slot.feels,
                humidity = 65,
                rainMm = slot.rain,
                uvIndex = uvAt(minute),
                windSpeed = 8.0,
                lux = slot.lux,
                stepsDelta = slot.steps,
                totalSteps = totalSteps,
                pressure = pressureAt(minute),
                inPocket = false,
                place = slot.place,
                movement = slot.movement,
            )
        }
    }

    private fun schedule(): List<Pair<Int, Slot>> = buildList {
        fun at(minute: Int, slot: Slot) = add(minute to slot)
        fun indoor(from: Int, to: Int, lux: Float, temp: Double, feels: Double, where: GeoPoint) {
            for (m in from..to step 30) at(m, Slot(Place.INDOOR, Movement.STILL, lux, temp, feels, at = where))
        }

        indoor(390, 450, 150f, 28.0, 32.0, HOME) // 06:30 - 07:30 at home
        at(480, Slot(Place.OUTDOOR, Movement.WALKING, 12_000f, 31.0, 36.0, steps = 900, at = HOME)) // walk to the bus
        at(510, Slot(Place.UNKNOWN, Movement.VEHICLE, 500f, 31.0, 36.0, at = ROAD)) // on the bus
        at(540, Slot(Place.OUTDOOR, Movement.WALKING, 15_000f, 32.0, 37.0, steps = 600, at = COLLEGE))
        indoor(570, 750, 250f, 29.0, 32.0, COLLEGE) // 09:30 - 12:30 classes
        for (m in 780..840 step 30) { // sunny lunch break
            at(m, Slot(Place.OUTDOOR, Movement.STILL, 48_000f, 34.0, 38.0, at = COLLEGE))
        }
        for (m in 870..900 step 30) { // afternoon shower, walking
            at(m, Slot(Place.OUTDOOR, Movement.WALKING, 3_000f, 29.0, 33.0, rain = 1.5, steps = 300, at = COLLEGE))
        }
        indoor(930, 990, 220f, 28.0, 31.0, COLLEGE)
        at(1020, Slot(Place.OUTDOOR, Movement.WALKING, 9_000f, 32.0, 37.0, steps = 700, at = COLLEGE))
        at(1050, Slot(Place.UNKNOWN, Movement.VEHICLE, 400f, 31.0, 35.0, at = ROAD))
        at(1080, Slot(Place.OUTDOOR, Movement.WALKING, 200f, 30.0, 34.0, steps = 500, at = HOME))
        indoor(1110, 1350, 120f, 28.0, 31.0, HOME) // 18:30 - 22:30 at home
    }

    /** Steady 1011 hPa until noon, then falling 1 hPa per 40 minutes down to 1003.5. */
    private fun pressureAt(minute: Int): Float =
        if (minute < 720) 1011f else maxOf(1011f - (minute - 720) / 40f, 1003.5f)

    /** UV index over the day: 0 at 06:00 and 18:00, 9 at noon. */
    private fun uvAt(minute: Int): Double = maxOf(0.0, 9.0 * sin(PI * (minute - 360) / 720.0))
}
