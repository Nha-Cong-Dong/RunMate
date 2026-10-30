package com.nhacongdong.runmate.data.place

import com.nhacongdong.runmate.data.remote.GeocodingResponse
import com.nhacongdong.runmate.domain.model.Place
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

// Test đọc JSON của Open-Meteo Geocoding (SPEC 8.2)
class OpenMeteoPlaceSearchTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decode_mapsResultsToPlaces() {
        val body = """{"results":[{"id":1,"name":"Hanoi","latitude":21.0285,"longitude":105.8542,"admin1":"Hanoi","country":"Vietnam"}],"generationtime_ms":0.4}"""

        val places = json.decodeFromString<GeocodingResponse>(body).toPlaces()

        assertEquals(listOf(Place("Hanoi", "Hanoi, Vietnam", 21.0285, 105.8542)), places)
    }

    @Test
    fun decode_withoutResultsKey_givesEmpty() {
        val body = """{"generationtime_ms":0.3}"""

        val places = json.decodeFromString<GeocodingResponse>(body).toPlaces()

        assertEquals(emptyList<Place>(), places)
    }
}
