package com.aira.app.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingMapperTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun `results become places with region and country`() {
        val text = """
            {"results":[
              {"id":1,"name":"Howrah","latitude":22.5958,"longitude":88.2636,"country":"India","admin1":"West Bengal"},
              {"id":2,"name":"Singapore","latitude":1.2897,"longitude":103.8501,"country":"Singapore","admin1":"Singapore"},
              {"id":3,"name":"Nowhere","latitude":1.0,"longitude":2.0}
            ]}
        """.trimIndent()
        val places = json.decodeFromString<GeocodingResponse>(text).toPlaces()
        assertEquals(3, places.size)
        assertEquals("Howrah", places[0].name)
        assertEquals("West Bengal, India", places[0].region)
        assertEquals(22.5958, places[0].latitude, 0.0)
        assertEquals("Singapore", places[1].region) // the same word twice is shown once
        assertEquals("", places[2].region)
    }

    @Test
    fun `no match gives an empty list`() {
        assertTrue(json.decodeFromString<GeocodingResponse>("""{"generationtime_ms":0.5}""").toPlaces().isEmpty())
    }
}
