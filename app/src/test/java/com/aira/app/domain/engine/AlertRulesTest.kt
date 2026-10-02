package com.aira.app.domain.engine

import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEngineTest {

    private fun sunny(minute: Int) = snap(minute, Place.OUTDOOR, lux = 40_000f, feels = 30.0)

    private fun alerts(vararg snapshots: com.aira.app.domain.model.Snapshot) = AlertEngine.evaluate(snapshots.toList())

    // ---- HEAT ----

    @Test
    fun `heat when outdoors and feels like above 38`() =
        assertTrue(AlertType.HEAT in alerts(snap(0, Place.OUTDOOR, lux = 5000f, feels = 38.1)))

    @Test
    fun `exactly 38 is not a heat alert`() =
        assertFalse(AlertType.HEAT in alerts(snap(0, Place.OUTDOOR, lux = 5000f, feels = 38.0)))

    @Test
    fun `no heat alert indoors`() = assertFalse(AlertType.HEAT in alerts(snap(0, Place.INDOOR, feels = 45.0)))

    @Test
    fun `no heat alert while weather is pending`() =
        assertFalse(AlertType.HEAT in alerts(snap(0, Place.OUTDOOR, lux = 5000f, feels = 45.0, pending = true)))

    // ---- STRONG_SUN ----

    @Test
    fun `strong sun for 45 minutes gives an alert`() =
        assertTrue(AlertType.STRONG_SUN in alerts(sunny(0), sunny(15), sunny(30), sunny(45)))

    @Test
    fun `strong sun for only 30 minutes gives no alert`() =
        assertFalse(AlertType.STRONG_SUN in alerts(sunny(0), sunny(15), sunny(30)))

    @Test
    fun `a break in the sun restarts the count`() {
        val broken = alerts(sunny(0), sunny(15), snap(30, Place.INDOOR), sunny(45), sunny(60))
        assertFalse(AlertType.STRONG_SUN in broken)
    }

    @Test
    fun `a gap of more than 60 minutes between snapshots is a break`() =
        assertFalse(AlertType.STRONG_SUN in alerts(sunny(0), sunny(90)))

    @Test
    fun `sun that has ended gives no alert`() =
        assertFalse(AlertType.STRONG_SUN in alerts(sunny(0), sunny(30), sunny(60), snap(75, Place.INDOOR)))

    @Test
    fun `sun minutes are measured from the first snapshot of the run to the newest`() {
        assertEquals(45.0, AlertEngine.minutesInStrongSun(listOf(sunny(0), sunny(15), sunny(30), sunny(45))), 0.0)
        assertEquals(0.0, AlertEngine.minutesInStrongSun(emptyList()), 0.0)
    }

    // ---- PRESSURE_DROP ----

    @Test
    fun `falling pressure gives an alert`() {
        val result = alerts(snap(0, pressure = 1012f), snap(60, pressure = 1010f), snap(120, pressure = 1008.5f))
        assertTrue(AlertType.PRESSURE_DROP in result)
    }

    @Test
    fun `phone without barometer never gets a pressure alert`() =
        assertFalse(AlertType.PRESSURE_DROP in alerts(snap(0), snap(120)))

    // ---- RAIN_NOW ----

    @Test
    fun `raining while outdoors gives a rain now alert`() =
        assertTrue(AlertType.RAIN_NOW in alerts(snap(0, Place.OUTDOOR, rain = 1.0)))

    @Test
    fun `walking in rain gives a rain now alert`() =
        assertTrue(AlertType.RAIN_NOW in alerts(snap(0, Place.UNKNOWN, Movement.WALKING, rain = 1.0)))

    @Test
    fun `rain while indoors gives no alert`() =
        assertFalse(AlertType.RAIN_NOW in alerts(snap(0, Place.INDOOR, rain = 5.0)))

    // ---- general ----

    @Test
    fun `no snapshots give no alerts`() = assertTrue(alerts().isEmpty())

    @Test
    fun `the newest snapshot decides, whatever the order of the list`() {
        val hotNow = snap(60, Place.OUTDOOR, lux = 5000f, feels = 40.0)
        val coolBefore = snap(0, Place.OUTDOOR, lux = 5000f, feels = 30.0)
        assertTrue(AlertType.HEAT in alerts(hotNow, coolBefore))
        assertFalse(AlertType.HEAT in alerts(coolBefore, snap(60, Place.OUTDOOR, lux = 5000f, feels = 30.0)))
    }

    @Test
    fun `several alerts can apply at once`() {
        val result = alerts(
            sunny(0), sunny(15), sunny(30),
            snap(45, Place.OUTDOOR, lux = 40_000f, feels = 40.0, rain = 1.0),
        )
        assertTrue(result.containsAll(listOf(AlertType.RAIN_NOW, AlertType.HEAT, AlertType.STRONG_SUN)))
    }
}

class AlertCooldownTest {
    private val hour = 60 * 60 * 1000L
    private val now = 100 * hour

    @Test
    fun `an alert that was never sent is allowed`() =
        assertEquals(listOf(AlertType.HEAT), AlertCooldown.filter(listOf(AlertType.HEAT), emptyMap(), now))

    @Test
    fun `an alert sent one hour ago is blocked`() =
        assertTrue(AlertCooldown.filter(listOf(AlertType.HEAT), mapOf(AlertType.HEAT to now - hour), now).isEmpty())

    @Test
    fun `an alert sent just under 3 hours ago is still blocked`() =
        assertTrue(AlertCooldown.filter(listOf(AlertType.HEAT), mapOf(AlertType.HEAT to now - 3 * hour + 1), now).isEmpty())

    @Test
    fun `an alert sent exactly 3 hours ago is allowed again`() =
        assertEquals(
            listOf(AlertType.HEAT),
            AlertCooldown.filter(listOf(AlertType.HEAT), mapOf(AlertType.HEAT to now - 3 * hour), now),
        )

    @Test
    fun `each type has its own cooldown`() {
        val result = AlertCooldown.filter(
            due = listOf(AlertType.HEAT, AlertType.STRONG_SUN),
            lastSent = mapOf(AlertType.HEAT to now - hour),
            now = now,
        )
        assertEquals(listOf(AlertType.STRONG_SUN), result)
    }

    @Test
    fun `nothing due gives nothing`() = assertTrue(AlertCooldown.filter(emptyList(), emptyMap(), now).isEmpty())
}

class BatteryRulesTest {
    @Test fun `below threshold and not charging skips`() = assertTrue(BatteryRules.shouldSkip(10, false, 15))
    @Test fun `below threshold but charging does not skip`() = assertFalse(BatteryRules.shouldSkip(10, true, 15))
    @Test fun `exactly at the threshold does not skip`() = assertFalse(BatteryRules.shouldSkip(15, false, 15))
    @Test fun `above the threshold does not skip`() = assertFalse(BatteryRules.shouldSkip(80, false, 15))
    @Test fun `unknown level never skips`() = assertFalse(BatteryRules.shouldSkip(null, false, 15))
    @Test fun `a higher threshold skips sooner`() = assertTrue(BatteryRules.shouldSkip(25, false, 30))
}
