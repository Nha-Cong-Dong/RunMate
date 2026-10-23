package com.nhacongdong.runmate.spike

// Code chạy thử 3.3a: gọi Open-Meteo Forecast và Geocoding.
// Chỉ nằm ở nhánh spike/api, không gộp vào main.

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.io.InterruptedIOException
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val FORECAST_BASE_URL = "https://api.open-meteo.com/"
private const val GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/"
private const val HANOI_LAT = 21.0285
private const val HANOI_LON = 105.8542

// ----- Dữ liệu trả về (chỉ lấy các trường cần dùng) -----

@Serializable
data class ForecastResponse(val hourly: Hourly)

@Serializable
data class Hourly(
    val time: List<Long>,
    @SerialName("temperature_2m") val temperature: List<Double?>,
    @SerialName("precipitation_probability") val precipitationProb: List<Int?>,
    @SerialName("wind_speed_10m") val windSpeed: List<Double?>,
    @SerialName("uv_index") val uvIndex: List<Double?>,
)

@Serializable
data class GeocodingResponse(val results: List<Place> = emptyList())

@Serializable
data class Place(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
)

// ----- Khai báo API cho Retrofit -----

interface ForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,wind_speed_10m,uv_index",
        @Query("forecast_days") forecastDays: Int = 16,
        @Query("timeformat") timeFormat: String = "unixtime",
    ): ForecastResponse
}

interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("language") language: String = "vi",
        @Query("count") count: Int = 5,
    ): GeocodingResponse
}

private val json = Json { ignoreUnknownKeys = true }

private inline fun <reified T> createApi(baseUrl: String, timeoutMillis: Long = 10_000): T {
    val client = OkHttpClient.Builder()
        .callTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
        .build()
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(T::class.java)
}

// ----- Các lần gọi thử -----

private suspend fun callForecast(api: ForecastApi): String {
    val hourly = api.forecast(HANOI_LAT, HANOI_LON).hourly
    // Giờ hiện tại làm tròn xuống giờ chẵn (mục 5.2), tính bằng giây unix
    val hourStart = System.currentTimeMillis() / 1000 / 3600 * 3600
    val i = hourly.time.indexOf(hourStart)
    if (i < 0) return "Không thấy mốc $hourStart trong ${hourly.time.size} mốc giờ"
    val localTime = Instant.ofEpochSecond(hourStart).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return """
        Mốc giờ: $localTime (unix $hourStart)
        Nhiệt độ: ${hourly.temperature[i]} °C
        Khả năng mưa: ${hourly.precipitationProb[i]} %
        Gió: ${hourly.windSpeed[i]} km/h
        UV: ${hourly.uvIndex[i]}
        Tổng số mốc giờ nhận được: ${hourly.time.size}
    """.trimIndent()
}

private suspend fun callGeocoding(api: GeocodingApi, name: String): String {
    val places = api.search(name).results
    if (places.isEmpty()) return "Không có kết quả cho \"$name\""
    return places.joinToString("\n") { p ->
        "${p.name}, ${p.admin1 ?: "?"}, ${p.country ?: "?"} (${p.latitude}, ${p.longitude})"
    }
}

// Bỏ dấu tiếng Việt: Open-Meteo Geocoding không tìm được khi tên có dấu
fun removeVietnameseAccents(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd')
        .replace('Đ', 'D')

private val SAMPLE_NAMES = listOf("Hồ Gươm", "Hà Nội", "Hoàn Kiếm", "Đà Nẵng", "Huế")

private suspend fun callGeocodingSamples(api: GeocodingApi): String = buildString {
    for (name in SAMPLE_NAMES) {
        val query = removeVietnameseAccents(name)
        append("\"$name\" → gửi \"$query\"\n")
        append(callGeocoding(api, query))
        append("\n\n")
    }
}

private val POI_NAMES = listOf("Hồ Gươm", "Hồ Tây", "Sân vận động Mỹ Đình", "Công viên Thống Nhất", "Hà Nội")

// Android 13 (API 33) trở lên có cách gọi bất đồng bộ.
// Máy cũ hơn dùng cách gọi chặn luồng, nên phải chạy trên luồng IO.
private suspend fun androidGeocode(context: Context, name: String): List<Address> {
    val geocoder = Geocoder(context, Locale.forLanguageTag("vi-VN"))
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocationName(name, 5, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses)
                override fun onError(errorMessage: String?) =
                    cont.resumeWithException(IOException(errorMessage ?: "Geocoder lỗi"))
            })
        }
    } else {
        withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(name, 5).orEmpty()
        }
    }
}

private suspend fun callAndroidGeocoder(context: Context): String = buildString {
    // Geocoder cần dịch vụ phía sau không có sẵn trong Android, có máy không dùng được
    append("Geocoder.isPresent() = ${Geocoder.isPresent()}\n\n")
    for (name in POI_NAMES) {
        append("\"$name\"\n")
        val results = androidGeocode(context, name)
        if (results.isEmpty()) {
            append("Không có kết quả\n")
        } else {
            for (a in results) {
                append("• ${a.getAddressLine(0)} (${a.latitude}, ${a.longitude})\n")
            }
        }
        append("\n")
    }
}

// Bắt mọi lỗi và đổi thành chữ, để app không crash
private suspend fun safeCall(block: suspend () -> String): String = try {
    block()
} catch (e: InterruptedIOException) {
    "Lỗi: hết thời gian chờ (${e.javaClass.simpleName}: ${e.message})"
} catch (e: IOException) {
    "Lỗi mạng: ${e.javaClass.simpleName}: ${e.message}"
} catch (e: HttpException) {
    "Lỗi HTTP ${e.code()}"
} catch (e: SerializationException) {
    "Lỗi đọc JSON: ${e.message}"
}

// ----- Màn hình -----

@Composable
fun ApiSpikeScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var result by remember { mutableStateOf("Bấm một nút để thử") }
    var loading by remember { mutableStateOf(false) }

    fun run(label: String, block: suspend () -> String) {
        loading = true
        result = "$label: đang gọi…"
        scope.launch {
            val startedAt = System.currentTimeMillis()
            val text = safeCall(block)
            result = "$label (${System.currentTimeMillis() - startedAt} ms)\n\n$text"
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(enabled = !loading, onClick = {
            run("Forecast Hà Nội") { callForecast(createApi(FORECAST_BASE_URL)) }
        }) { Text("Gọi Forecast (Hà Nội, giờ hiện tại)") }

        Button(enabled = !loading, onClick = {
            run("Geocoding (bỏ dấu)") { callGeocodingSamples(createApi(GEOCODING_BASE_URL)) }
        }) { Text("Tìm 5 tên có dấu (bỏ dấu trước khi gửi)") }

        Button(enabled = !loading, onClick = {
            run("Timeout 1 ms") { callForecast(createApi(FORECAST_BASE_URL, timeoutMillis = 1)) }
        }) { Text("Thử timeout (1 ms)") }

        Button(enabled = !loading, onClick = {
            run("Android Geocoder") { callAndroidGeocoder(context) }
        }) { Text("Android Geocoder: tìm địa điểm cụ thể") }

        Text(result)
    }
}
