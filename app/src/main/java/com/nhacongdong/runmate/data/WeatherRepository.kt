package com.nhacongdong.runmate.data

import com.nhacongdong.runmate.data.local.WeatherSnapshot
import com.nhacongdong.runmate.data.remote.ForecastApi
import com.nhacongdong.runmate.domain.WeatherRules
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.time.ZoneId

// Kết quả lấy dự báo. Offline tách riêng để S4 ghi "Không có mạng", còn lỗi khác thì báo hộp thoại (DESIGN 10)
sealed interface ForecastResult {
    data class Success(val snapshot: WeatherSnapshot) : ForecastResult
    data object Offline : ForecastResult   // mất mạng hoặc quá thời gian chờ
    data object Failed : ForecastResult    // mã HTTP lỗi, dữ liệu sai cấu trúc, thiếu số liệu hoặc không có giờ đó
}

// Lấy dự báo thời tiết cho một giờ (SPEC 5.2, 8.1). Không ném lỗi ra ngoài, mọi lỗi đều thành ForecastResult
class WeatherRepository(
    private val api: ForecastApi,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun fetch(
        latitude: Double,
        longitude: Double,
        startTime: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ForecastResult {
        // Open-Meteo trả giờ dạng giây (timeformat=unixtime), còn app dùng mili giây
        val hourSeconds = WeatherRules.forecastHour(startTime, zone) / 1_000
        return try {
            val hourly = api.forecast(
                latitude = latitude,
                longitude = longitude,
                hourly = HOURLY,
                forecastDays = FORECAST_DAYS,
                timeFormat = "unixtime",
            ).hourly
            val index = hourly.time.indexOf(hourSeconds)
            if (index < 0) return ForecastResult.Failed
            ForecastResult.Success(
                WeatherSnapshot(
                    weatherFetchedAt = clock(),
                    temperatureC = hourly.temperature.getOrNull(index) ?: return ForecastResult.Failed,
                    precipitationProb = hourly.precipitationProbability.getOrNull(index) ?: return ForecastResult.Failed,
                    windSpeedKmh = hourly.windSpeed.getOrNull(index) ?: return ForecastResult.Failed,
                    uvIndex = hourly.uvIndex.getOrNull(index) ?: return ForecastResult.Failed,
                )
            )
        } catch (e: IOException) {
            ForecastResult.Offline   // mất mạng hoặc timeout (SocketTimeoutException cũng là IOException)
        } catch (e: HttpException) {
            ForecastResult.Failed    // mã HTTP không phải 2xx
        } catch (e: SerializationException) {
            ForecastResult.Failed    // dữ liệu trả về không đúng cấu trúc
        }
    }

    private companion object {
        const val HOURLY = "temperature_2m,precipitation_probability,wind_speed_10m,uv_index"
        const val FORECAST_DAYS = 16
    }
}
