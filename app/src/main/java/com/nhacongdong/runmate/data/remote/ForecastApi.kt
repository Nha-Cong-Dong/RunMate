package com.nhacongdong.runmate.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

// Open-Meteo Forecast API (SPEC 8.1). Không cần API key.
// Các tham số gửi cùng lúc, không dùng giá trị mặc định (Retrofit không đọc được giá trị mặc định của Kotlin)
interface ForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String,
        @Query("forecast_days") forecastDays: Int,
        @Query("timeformat") timeFormat: String,
    ): ForecastResponse
}

// Chỉ đọc các trường cần dùng. Mỗi mảng dài bằng số giờ dự báo, các phần tử cùng vị trí thuộc cùng một giờ
@Serializable
data class ForecastResponse(val hourly: HourlyForecast)

@Serializable
data class HourlyForecast(
    val time: List<Long>,
    @SerialName("temperature_2m") val temperature: List<Double?>,
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?>,
    @SerialName("wind_speed_10m") val windSpeed: List<Double?>,
    @SerialName("uv_index") val uvIndex: List<Double?>,
)
