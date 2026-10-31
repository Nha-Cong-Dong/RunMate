package com.nhacongdong.runmate.data

import com.nhacongdong.runmate.data.local.WeatherSnapshot
import com.nhacongdong.runmate.data.remote.ForecastApi
import com.nhacongdong.runmate.data.remote.ForecastResponse
import com.nhacongdong.runmate.data.remote.HourlyForecast
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId

// Test lấy dự báo cho một giờ (SPEC 5.2, 8.1). API giả, không gọi mạng thật
class WeatherRepositoryTest {
    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    private val now = 1_000L

    // Giờ 06:00 ngày 29/10/2026 theo giây (Open-Meteo dùng giây)
    private val sixSeconds = LocalDateTime.of(2026, 10, 29, 6, 0).atZone(zone).toEpochSecond()
    private val sevenSeconds = sixSeconds + 3_600
    private val startAt6_30 = LocalDateTime.of(2026, 10, 29, 6, 30).atZone(zone).toInstant().toEpochMilli()

    private class FakeForecastApi(private val answer: () -> ForecastResponse) : ForecastApi {
        override suspend fun forecast(
            latitude: Double,
            longitude: Double,
            hourly: String,
            forecastDays: Int,
            timeFormat: String,
        ): ForecastResponse = answer()
    }

    private fun response(
        times: List<Long> = listOf(sixSeconds, sevenSeconds),
        precipitation: List<Int?> = listOf(40, 60),
    ) = ForecastResponse(
        HourlyForecast(
            time = times,
            temperature = listOf(27.5, 28.1),
            precipitationProbability = precipitation,
            windSpeed = listOf(12.3, 15.0),
            uvIndex = listOf(0.0, 3.5),
        )
    )

    private fun repository(api: ForecastApi) = WeatherRepository(api, clock = { now })

    @Test
    fun fetch_picksHourRoundedDown() = runBlocking {
        // 06:30 lấy dữ liệu của 06:00
        val result = repository(FakeForecastApi { response() }).fetch(21.0, 105.8, startAt6_30, zone)

        assertEquals(
            ForecastResult.Success(
                WeatherSnapshot(weatherFetchedAt = now, temperatureC = 27.5, precipitationProb = 40, windSpeedKmh = 12.3, uvIndex = 0.0),
            ),
            result,
        )
    }

    @Test
    fun fetch_hourMissingFromForecast_isFailed() = runBlocking {
        val result = repository(FakeForecastApi { response(times = listOf(sevenSeconds, sevenSeconds + 3_600)) })
            .fetch(21.0, 105.8, startAt6_30, zone)

        assertEquals(ForecastResult.Failed, result)
    }

    @Test
    fun fetch_missingValue_isFailed() = runBlocking {
        // Không lưu nửa vời: thiếu một số liệu của giờ đó thì coi như lỗi
        val result = repository(FakeForecastApi { response(precipitation = listOf(null, 60)) })
            .fetch(21.0, 105.8, startAt6_30, zone)

        assertEquals(ForecastResult.Failed, result)
    }

    @Test
    fun fetch_networkError_isOffline() = runBlocking {
        val result = repository(FakeForecastApi { throw IOException("mất mạng") })
            .fetch(21.0, 105.8, startAt6_30, zone)

        assertEquals(ForecastResult.Offline, result)
    }
}
