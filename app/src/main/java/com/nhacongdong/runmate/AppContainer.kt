package com.nhacongdong.runmate

import android.content.Context
import android.location.Geocoder
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.SessionRepository
import com.nhacongdong.runmate.data.local.RunMateDatabase
import com.nhacongdong.runmate.data.location.LocationProvider
import com.nhacongdong.runmate.data.place.GeocoderPlaceSearch
import com.nhacongdong.runmate.data.place.OpenMeteoPlaceSearch
import com.nhacongdong.runmate.data.place.PlaceRepository
import com.nhacongdong.runmate.data.WeatherRepository
import com.nhacongdong.runmate.data.remote.ForecastApi
import com.nhacongdong.runmate.data.remote.GeocodingApi
import com.nhacongdong.runmate.domain.FinishRun
import com.nhacongdong.runmate.domain.GpsDropCounter
import com.nhacongdong.runmate.domain.PauseRun
import com.nhacongdong.runmate.domain.RecordTrackPoint
import com.nhacongdong.runmate.domain.ResumeRun
import com.nhacongdong.runmate.domain.StartFreeRun
import com.nhacongdong.runmate.domain.StartPlanRun
import com.nhacongdong.runmate.domain.RefreshMissedPlans
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit

// Buổi vừa lưu trong phiên đang mở, để S1 vẽ hiệu ứng vòng một lần (DESIGN 3.4). Chỉ nhớ trong bộ nhớ, không lưu database
class RecentlyFinishedRun {
    private val _sessionId = MutableStateFlow<Long?>(null)
    val sessionId: StateFlow<Long?> = _sessionId.asStateFlow()

    fun mark(id: Long) {
        _sessionId.value = id
    }

    fun consume() {
        _sessionId.value = null
    }
}

// Nơi duy nhất tạo các phụ thuộc dùng chung (SPEC 8.4: tự tạo thủ công).
// Database, API và Repository sẽ được thêm vào đây ở các giai đoạn A–C.
class AppContainer(private val context: Context) {

    // Chỉ mở database khi có màn hình cần tới (SPEC 7, 8.4)
    val database: RunMateDatabase by lazy {
        Room.databaseBuilder<RunMateDatabase>(context, "runmate.db")
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    val planRepository by lazy { PlanRepository(database.planDao()) }

    val sessionRepository by lazy {
        SessionRepository(database, database.sessionDao(), database.trackPointDao(), database.planDao())
    }

    // Ghi một điểm GPS của buổi chạy, dùng trong RunTrackingService (SPEC 10.2)
    // Bộ đếm điểm bị bỏ dùng chung giữa service (ghi) và S6 (đọc), nên chỉ có một bản
    val gpsDrops = GpsDropCounter()

    // Buổi vừa lưu, dùng cho hiệu ứng ở S1 (DESIGN 3.4)
    val recentlyFinished = RecentlyFinishedRun()
    val recordTrackPoint by lazy { RecordTrackPoint(sessionRepository, gpsDrops) }

    // Bắt đầu, tạm dừng, tiếp tục, kết thúc buổi chạy (SPEC 6.3). Màn S4, S5, S6 gọi các use case này
    val startPlanRun by lazy { StartPlanRun(sessionRepository, planRepository) }
    val startFreeRun by lazy { StartFreeRun(sessionRepository) }
    val pauseRun by lazy { PauseRun(sessionRepository) }
    val resumeRun by lazy { ResumeRun(sessionRepository) }
    val finishRun by lazy { FinishRun(sessionRepository, planRepository) }

    val refreshMissedPlans by lazy { RefreshMissedPlans(planRepository) }

    private val json = Json { ignoreUnknownKeys = true }

    // Open-Meteo Geocoding, dùng khi Geocoder của máy không có sẵn hoặc lỗi (SPEC 8.2)
    val geocodingApi: GeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeocodingApi::class.java)
    }

    // Open-Meteo Forecast, timeout 10 giây (SPEC 8.1). Mất mạng hoặc timeout thì WeatherRepository trả về null
    private val forecastClient by lazy {
        OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build()
    }

    val forecastApi: ForecastApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(forecastClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ForecastApi::class.java)
    }

    val weatherRepository by lazy { WeatherRepository(forecastApi) }

    // Vị trí hiện tại cho S3 (SPEC 8.2)
    val locationProvider by lazy { LocationProvider(context.applicationContext) }

    val placeRepository by lazy {
        val appContext = context.applicationContext
        PlaceRepository(
            geocoder = if (Geocoder.isPresent()) {
                GeocoderPlaceSearch(Geocoder(appContext, Locale.forLanguageTag("vi-VN")))
            } else {
                null
            },
            fallback = OpenMeteoPlaceSearch(geocodingApi)
        )
    }
}
