package com.nhacongdong.runmate.ui.run

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nhacongdong.runmate.RunMateApplication
import com.nhacongdong.runmate.data.ForecastResult
import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.SessionRepository
import com.nhacongdong.runmate.data.WeatherRepository
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.local.WeatherSnapshot
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.domain.PlanRules
import com.nhacongdong.runmate.domain.RunRules
import com.nhacongdong.runmate.domain.StartFreeRun
import com.nhacongdong.runmate.domain.WeatherRules
import com.nhacongdong.runmate.domain.model.Place
import com.nhacongdong.runmate.ui.components.DEFAULT_DISTANCE_M
import com.nhacongdong.runmate.ui.components.DEFAULT_DURATION_S
import com.nhacongdong.runmate.ui.place.PlacePickerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

// Trạng thái của S5 Chuẩn bị chạy tự do (SPEC mục 5.4, S5; DESIGN 9 S5)
data class FreeRunUiState(
    val location: Place? = null,          // mặc định là vị trí hiện tại (BR-41). Null thì hiện "Chọn vị trí"
    val locationFailed: Boolean = false,  // lấy vị trí hiện tại không được
    val goalType: GoalType = GoalType.DISTANCE,
    val goalValue: Int = DEFAULT_DISTANCE_M,
    val weather: WeatherSnapshot? = null,
    val weatherLoading: Boolean = false,
    val weatherOffline: Boolean = false,
    val weatherFailed: Boolean = false,   // lỗi khác, hiện hộp thoại (DESIGN 10)
    val suggestion: RunPlan? = null,      // kế hoạch gợi ý trong khoảng BR-44
    val hasActiveRun: Boolean = false,    // đang có buổi chạy dở (BR-08): nút Bắt đầu chạy tắt
)

// Chạy tự do (S5). Thời tiết lấy cho giờ hiện tại. Gợi ý kế hoạch chỉ hiện một lần trong lần mở màn hình
class FreeRunViewModel(
    private val sessions: SessionRepository,
    private val plans: PlanRepository,
    private val weatherRepository: WeatherRepository,
    private val startFreeRun: StartFreeRun,
    private val locate: suspend () -> Location?,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreeRunUiState())
    val uiState: StateFlow<FreeRunUiState> = _uiState.asStateFlow()

    // Bỏ qua gợi ý thì không hỏi lại trong cùng lần mở màn hình (đã chốt)
    private var suggestionDismissed = false
    private var weatherJob: Job? = null
    private var weatherKey: Pair<Long, Pair<Double, Double>>? = null

    init {
        viewModelScope.launch {
            plans.observePlans().collect { all ->
                val suggestion = if (suggestionDismissed) null else RunRules.suggestedPlan(all, clock())
                _uiState.update { it.copy(suggestion = suggestion) }
            }
        }
        refreshActiveRun()
    }

    // Gọi khi màn hình hiện lên (kể cả quay lại): kiểm tra buổi chạy dở để tắt hoặc bật nút Bắt đầu chạy
    fun refreshActiveRun() {
        viewModelScope.launch {
            val active = sessions.getActiveSession() != null
            _uiState.update { it.copy(hasActiveRun = active) }
        }
    }

    // Dùng vị trí hiện tại. Gọi sau khi đã có quyền vị trí và GPS bật
    fun useCurrentLocation() {
        viewModelScope.launch {
            val location = try {
                locate()
            } catch (e: SecurityException) {
                null
            }
            if (location == null) {
                _uiState.update { it.copy(locationFailed = true) }
            } else {
                onLocationChanged(Place(PlacePickerViewModel.CURRENT_LOCATION_NAME, "", location.latitude, location.longitude))
            }
        }
    }

    // Chọn vị trí khác ở S3
    fun onLocationPicked(place: Place) = onLocationChanged(place)

    fun onGoalTypeChange(type: GoalType) {
        _uiState.update {
            it.copy(goalType = type, goalValue = if (type == GoalType.DISTANCE) DEFAULT_DISTANCE_M else DEFAULT_DURATION_S)
        }
    }

    fun onGoalValueChange(value: Int) = _uiState.update { it.copy(goalValue = value) }

    // Bấm Bỏ qua gợi ý
    fun dismissSuggestion() {
        suggestionDismissed = true
        _uiState.update { it.copy(suggestion = null) }
    }

    fun retryWeather() = fetchWeather(manual = true)

    fun dismissWeatherError() = _uiState.update { it.copy(weatherFailed = false) }

    // Bắt đầu chạy tự do. Trả về id buổi chạy, hoặc null nếu không bắt đầu được (thiếu vị trí, có buổi dở)
    suspend fun startRun(): Long? {
        val state = _uiState.value
        val location = state.location ?: return null
        val goalErrors = PlanRules.validate(clock(), state.goalType, state.goalValue, clock())
        if (goalErrors.isNotEmpty()) return null
        return startFreeRun(
            locationName = location.name,
            latitude = location.latitude,
            longitude = location.longitude,
            goalType = state.goalType,
            goalValue = state.goalValue,
        )
    }

    private fun onLocationChanged(place: Place) {
        _uiState.update { it.copy(location = place, locationFailed = false) }
        fetchWeather()
    }

    // Thời tiết cho giờ hiện tại và vị trí đang chọn (SPEC 5.4). Tự lấy khi vị trí đổi, chờ 400 ms như S2.
    // Bấm thử lại thì lấy ngay và giữ dữ liệu cũ nếu lỗi
    private fun fetchWeather(manual: Boolean = false) {
        val location = _uiState.value.location ?: return
        val now = clock()
        val hour = WeatherRules.forecastHour(now, zone)
        val key = hour to (location.latitude to location.longitude)
        if (!manual && key == weatherKey) return
        weatherKey = key
        weatherJob?.cancel()

        _uiState.update {
            if (manual) it.copy(weatherLoading = true, weatherFailed = false)
            else it.copy(weather = null, weatherLoading = true, weatherOffline = false, weatherFailed = false)
        }
        weatherJob = viewModelScope.launch {
            if (!manual) delay(WEATHER_DELAY_MS)
            val result = weatherRepository.fetch(location.latitude, location.longitude, now, zone)
            _uiState.update {
                when (result) {
                    is ForecastResult.Success -> it.copy(
                        weather = result.snapshot, weatherLoading = false, weatherOffline = false,
                    )
                    ForecastResult.Offline -> it.copy(weatherLoading = false, weatherOffline = true)
                    ForecastResult.Failed -> it.copy(weatherLoading = false, weatherOffline = false, weatherFailed = true)
                }
            }
        }
    }

    companion object {
        private const val WEATHER_DELAY_MS = 400L

        fun factory() = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RunMateApplication).container
                FreeRunViewModel(
                    sessions = container.sessionRepository,
                    plans = container.planRepository,
                    weatherRepository = container.weatherRepository,
                    startFreeRun = container.startFreeRun,
                    locate = container.locationProvider::currentLocation,
                )
            }
        }
    }
}
