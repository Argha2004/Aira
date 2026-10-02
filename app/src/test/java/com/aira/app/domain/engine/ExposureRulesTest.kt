package com.aira.app.domain.engine

import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExposureRulesTest {

    // ---- sun ----

    @Test
    fun `outdoor above 30000 lux is sun exposure`() =
        assertTrue(ExposureRules.isSunExposure(Place.OUTDOOR, 30_001f))

    @Test
    fun `exactly 30000 lux is not sun exposure`() =
        assertFalse(ExposureRules.isSunExposure(Place.OUTDOOR, 30_000f))

    @Test
    fun `bright light indoors is not sun exposure`() =
        assertFalse(ExposureRules.isSunExposure(Place.INDOOR, 50_000f))

    @Test
    fun `missing light reading is not sun exposure`() =
        assertFalse(ExposureRules.isSunExposure(Place.OUTDOOR, null))

    // ---- heat ----

    @Test
    fun `outdoor and feels like above 33 is heat exposure`() =
        assertTrue(ExposureRules.isHeatExposure(Place.OUTDOOR, Movement.STILL, 33.1))

    @Test
    fun `exactly 33 is not heat exposure`() =
        assertFalse(ExposureRules.isHeatExposure(Place.OUTDOOR, Movement.STILL, 33.0))

    @Test
    fun `walking counts as exposed even if place is unknown`() =
        assertTrue(ExposureRules.isHeatExposure(Place.UNKNOWN, Movement.WALKING, 36.0))

    @Test
    fun `sitting indoors is not heat exposure`() =
        assertFalse(ExposureRules.isHeatExposure(Place.INDOOR, Movement.STILL, 40.0))

    // ---- rain ----

    @Test
    fun `outdoor in more than 0_2 mm rain is a rain encounter`() =
        assertTrue(ExposureRules.isRainEncounter(Place.OUTDOOR, Movement.STILL, 0.3))

    @Test
    fun `exactly 0_2 mm is not a rain encounter`() =
        assertFalse(ExposureRules.isRainEncounter(Place.OUTDOOR, Movement.STILL, 0.2))

    @Test
    fun `walking in rain is a rain encounter`() =
        assertTrue(ExposureRules.isRainEncounter(Place.UNKNOWN, Movement.WALKING, 1.0))

    @Test
    fun `staying indoors in rain is not a rain encounter`() =
        assertFalse(ExposureRules.isRainEncounter(Place.INDOOR, Movement.STILL, 5.0))

    @Test
    fun `riding in a vehicle is not a rain encounter`() =
        assertFalse(ExposureRules.isRainEncounter(Place.UNKNOWN, Movement.VEHICLE, 5.0))

    // ---- UV dose ----

    @Test
    fun `uv dose is index times minutes`() = assertEquals(315.0, ExposureRules.uvDose(7.0, 45.0), 0.0)

    @Test
    fun `dose below 100 is low`() = assertEquals(UvBand.LOW, ExposureRules.uvDoseBand(99.9))

    @Test
    fun `dose of exactly 100 is moderate`() = assertEquals(UvBand.MODERATE, ExposureRules.uvDoseBand(100.0))

    @Test
    fun `dose just below 300 is moderate`() = assertEquals(UvBand.MODERATE, ExposureRules.uvDoseBand(299.9))

    @Test
    fun `dose of exactly 300 is high`() = assertEquals(UvBand.HIGH, ExposureRules.uvDoseBand(300.0))

    @Test
    fun `zero dose is low`() = assertEquals(UvBand.LOW, ExposureRules.uvDoseBand(0.0))
}
