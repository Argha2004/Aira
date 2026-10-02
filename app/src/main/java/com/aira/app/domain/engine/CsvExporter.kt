package com.aira.app.domain.engine

import com.aira.app.domain.model.Snapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Writes snapshots as CSV (opens in Excel or Google Sheets). Temperatures are in °C. Pure Kotlin. */
object CsvExporter {

    val HEADER = listOf(
        "id", "timestamp_millis", "datetime", "latitude", "longitude", "temperature_c", "feels_like_c",
        "humidity_percent", "rain_mm", "uv_index", "wind_kmh", "lux", "steps_delta", "total_steps",
        "pressure_hpa", "in_pocket", "place", "movement", "weather_pending",
    )

    /** Writes the header and one line per snapshot, oldest first. Returns the number of snapshots written. */
    fun write(snapshots: List<Snapshot>, zone: ZoneId, out: Appendable): Int {
        out.append(HEADER.joinToString(",")).append('\n')
        snapshots.sortedBy { it.timestamp }.forEach { out.append(line(it, zone)).append('\n') }
        return snapshots.size
    }

    fun line(s: Snapshot, zone: ZoneId): String = listOf(
        s.id.toString(),
        s.timestamp.toString(),
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.ofEpochMilli(s.timestamp).atZone(zone)),
        s.latitude.toString(),
        s.longitude.toString(),
        s.temperature.toString(),
        s.feelsLike.toString(),
        s.humidity.toString(),
        s.rainMm.toString(),
        s.uvIndex.toString(),
        s.windSpeed.toString(),
        s.lux?.toString().orEmpty(),
        s.stepsDelta?.toString().orEmpty(),
        s.totalSteps?.toString().orEmpty(),
        s.pressure?.toString().orEmpty(),
        s.inPocket?.toString().orEmpty(),
        s.place.name,
        s.movement.name,
        s.weatherPending.toString(),
    ).joinToString(",") { escape(it) }

    /** A field with a comma, quote or line break is wrapped in quotes, and quotes inside are doubled. */
    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
