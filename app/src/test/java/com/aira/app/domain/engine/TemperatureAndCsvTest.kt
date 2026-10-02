package com.aira.app.domain.engine

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperatureTest {

    private val c = TemperatureUnit.CELSIUS
    private val f = TemperatureUnit.FAHRENHEIT

    @Test fun `freezing point`() = assertEquals(32.0, Temperature.toFahrenheit(0.0), 0.0)
    @Test fun `boiling point`() = assertEquals(212.0, Temperature.toFahrenheit(100.0), 0.0)
    @Test fun `minus 40 is the same in both`() = assertEquals(-40.0, Temperature.toFahrenheit(-40.0), 0.0)
    @Test fun `value is rounded`() = assertEquals(98, Temperature.value(36.6, f))
    @Test fun `celsius value is just rounded`() = assertEquals(37, Temperature.value(36.6, c))
    @Test fun `format celsius`() = assertEquals("34°C", Temperature.format(34.0, c))
    @Test fun `format fahrenheit`() = assertEquals("93°F", Temperature.format(34.0, f))

    @Test
    fun `text in celsius mode is unchanged`() {
        val text = "Outdoors in strong sun, 1 h, 34 °C"
        assertEquals(text, Temperature.convertText(text, c))
    }

    @Test
    fun `text in fahrenheit mode has its temperatures converted`() =
        assertEquals("Outdoors in strong sun, 1 h, 93 °F", Temperature.convertText("Outdoors in strong sun, 1 h, 34 °C", f))

    @Test
    fun `every temperature in a sentence is converted`() =
        assertEquals(
            "Feels like 100 °F, hotter than 95 °F.",
            Temperature.convertText("Feels like 38 °C, hotter than 35 °C.", f),
        )

    @Test
    fun `negative temperatures are converted`() = assertEquals("23 °F", Temperature.convertText("-5 °C", f))

    @Test
    fun `text without temperatures is unchanged`() =
        assertEquals("Indoors, 2 h", Temperature.convertText("Indoors, 2 h", f))

    @Test
    fun `hours and percentages are not touched`() =
        assertEquals("You got 40% more sun.", Temperature.convertText("You got 40% more sun.", f))
}

class CsvExporterTest {

    private val utc = ZoneOffset.UTC

    private fun csv(vararg snapshots: com.aira.app.domain.model.Snapshot): Pair<Int, List<String>> {
        val out = StringBuilder()
        val count = CsvExporter.write(snapshots.toList(), utc, out)
        return count to out.toString().trimEnd('\n').split('\n')
    }

    @Test
    fun `first line is the header`() {
        val (_, lines) = csv()
        assertEquals(CsvExporter.HEADER.joinToString(","), lines.first())
        assertEquals(19, CsvExporter.HEADER.size)
    }

    @Test
    fun `no snapshots gives only the header and a count of zero`() {
        val (count, lines) = csv()
        assertEquals(0, count)
        assertEquals(1, lines.size)
    }

    @Test
    fun `a snapshot becomes one line with every column`() {
        val (count, lines) = csv(snap(0))
        assertEquals(1, count)
        assertEquals("0,0,1970-01-01T00:00:00Z,22.57,88.36,30.0,30.0,60,0.0,5.0,8.0,200.0,0,,,false,INDOOR,STILL,false", lines[1])
    }

    @Test
    fun `every line has the same number of columns as the header`() {
        val (_, lines) = csv(snap(0), snap(30, pressure = 1010f), snap(60, lux = null, steps = null))
        assertTrue(lines.all { it.split(',').size == CsvExporter.HEADER.size })
    }

    @Test
    fun `missing sensor values are empty fields`() {
        val (_, lines) = csv(snap(0, lux = null, steps = null, pressure = null))
        val fields = lines[1].split(',')
        assertEquals("", fields[CsvExporter.HEADER.indexOf("lux")])
        assertEquals("", fields[CsvExporter.HEADER.indexOf("steps_delta")])
        assertEquals("", fields[CsvExporter.HEADER.indexOf("pressure_hpa")])
    }

    @Test
    fun `snapshots are written oldest first`() {
        val (_, lines) = csv(snap(60), snap(0), snap(30))
        assertEquals(listOf("0", "1800000", "3600000"), lines.drop(1).map { it.split(',')[1] })
    }

    @Test
    fun `weather pending is exported so partial rows can be spotted`() {
        val (_, lines) = csv(snap(0, pending = true))
        assertTrue(lines[1].endsWith(",true"))
    }

    @Test
    fun `fields with commas or quotes are escaped`() {
        assertEquals("plain", CsvExporter.escape("plain"))
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""))
        assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"))
    }
}
