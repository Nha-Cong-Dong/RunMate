package com.nhacongdong.runmate.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Test đọc JSON của Open-Meteo Forecast (SPEC 8.1). Tên trường có dấu gạch dưới map qua @SerialName
class ForecastResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decode_readsHourlyArrays() {
        val body = """
            {"latitude":21.0,"hourly":{
              "time":[1793001600,1793005200],
              "temperature_2m":[27.5,28.1],
              "precipitation_probability":[10,60],
              "wind_speed_10m":[12.3,15.0],
              "uv_index":[0.0,3.5]
            }}
        """.trimIndent()

        val hourly = json.decodeFromString<ForecastResponse>(body).hourly

        assertEquals(listOf(1793001600L, 1793005200L), hourly.time)
        assertEquals(listOf(27.5, 28.1), hourly.temperature)
        assertEquals(listOf(10, 60), hourly.precipitationProbability)
        assertEquals(listOf(12.3, 15.0), hourly.windSpeed)
        assertEquals(listOf(0.0, 3.5), hourly.uvIndex)
    }

    @Test
    fun decode_nullValueStaysNull() {
        val body = """
            {"hourly":{"time":[1793001600],"temperature_2m":[27.5],
            "precipitation_probability":[null],"wind_speed_10m":[12.3],"uv_index":[0.0]}}
        """.trimIndent()

        val hourly = json.decodeFromString<ForecastResponse>(body).hourly

        assertNull(hourly.precipitationProbability.single())
    }
}
