package com.nhacongdong.runmate.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nhacongdong.runmate.RunMateApplication
import com.nhacongdong.runmate.data.ForecastResult
import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.WeatherRepository
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.local.WeatherSnapshot
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.domain.PlanRules
import com.nhacongdong.runmate.domain.WeatherRules
import com.nhacongdong.runmate.domain.model.Place
import com.nhacongdong.runmate.domain.model.PlanError
import com.nhacongdong.runmate.ui.components.DEFAULT_DISTANCE_M
import com.nhacongdong.runmate.ui.components.DEFAULT_DURATION_S
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// Trạng thái của S2 (DESIGN 9 S2)
data class PlanEditUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val today: LocalDate,
    val start: LocalDateTime,
    val location: Place? = null,
    val goalType: GoalType = GoalType.DISTANCE,
    val goalValue: Int = DEFAULT_DISTANCE_M,
    val errors: List<PlanError> = emptyList(),   // hiện sau lần bấm Lưu đầu tiên
    val locationMissing: Boolean = false,
    // Thời tiết cho giờ và vị trí đang chọn. Null khi đang lấy lại hoặc chưa lấy được
    val weather: WeatherSnapshot? = null,
    val weatherLoading: Boolean = false,
    val weatherOffline: Boolean = false,   // lần lấy gần nhất không có mạng
    val weatherFailed: Boolean = false,    // lỗi khác, hiện hộp thoại (DESIGN 10)
    val refreshAllowed: Boolean = true,    // false khi kế hoạch đã lưu không được lấy lại (SPEC 5.2)
)

sealed interface PlanEditEvent {
    data object Saved : PlanEditEvent
}

// S2 Tạo / Sửa kế hoạch (SPEC 6.4, 5.2). Thời tiết lấy khi đổi giờ hoặc vị trí, và gắn vào kế hoạch khi lưu
class PlanEditViewModel(
    private val planRepository: PlanRepository,
    private val weatherRepository: WeatherRepository,
    private val planId: Long?,
    initialDay: LocalDate?,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val now: LocalDateTime get() = Instant.ofEpochMilli(clock()).atZone(zone).toLocalDateTime()

    // Kế hoạch đang sửa, null khi tạo mới
    private var existing: RunPlan? = null

    // Đã bấm Lưu ít nhất một lần thì kiểm tra lại ngay khi người dùng đổi giá trị
    private var submitted = false

    // Giờ chẵn và toạ độ mà thời tiết đang lấy cho. Đổi cả hai thì mới lấy lại
    private var weatherKey: Triple<Long, Double, Double>? = null
    private var weatherJob: Job? = null

    // Dữ liệu đã lưu của kế hoạch đang sửa, và giờ và vị trí của nó. Quay về đúng giờ và vị trí đó thì dùng lại
    private var savedKey: Triple<Long, Double, Double>? = null
    private var savedWeather: WeatherSnapshot? = null

    private val _uiState = MutableStateFlow(
        PlanEditUiState(
            isEditing = planId != null,
            today = now.toLocalDate(),
            start = PlanRules.defaultStart(initialDay, now),
        )
    )
    val uiState: StateFlow<PlanEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<PlanEditEvent>(Channel.BUFFERED)
    val events: Flow<PlanEditEvent> = _events.receiveAsFlow()

    init {
        if (planId != null) {
            viewModelScope.launch {
                val plan = planRepository.getPlan(planId)
                existing = plan
                if (plan != null) {
                    // Như S4: hiện dữ liệu đã lưu, không tự gọi API khi mở
                    savedKey = Triple(WeatherRules.forecastHour(plan.startTime, zone), plan.latitude, plan.longitude)
                    savedWeather = plan.weather
                    weatherKey = savedKey
                }
                _uiState.update {
                    if (plan == null) {
                        it.copy(isLoading = false)
                    } else {
                        it.copy(
                            isLoading = false,
                            start = Instant.ofEpochMilli(plan.startTime).atZone(zone).toLocalDateTime(),
                            location = Place(plan.locationName, "", plan.latitude, plan.longitude),
                            goalType = plan.goalType,
                            goalValue = plan.goalValue,
                            weather = plan.weather,
                            refreshAllowed = WeatherRules.canRefresh(plan.status),
                        )
                    }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onDateChange(date: LocalDate) = changeStart { it.with(date) }

    fun onTimeChange(time: LocalTime) = changeStart { it.with(time) }

    fun onLocationPicked(place: Place) {
        _uiState.update { it.copy(location = place, locationMissing = false) }
        fetchWeather()
    }

    // Đổi loại mục tiêu thì đặt lại về mặc định của loại đó
    fun onGoalTypeChange(type: GoalType) {
        _uiState.update {
            it.copy(
                goalType = type,
                goalValue = if (type == GoalType.DISTANCE) DEFAULT_DISTANCE_M else DEFAULT_DURATION_S,
            )
        }
        revalidate()
    }

    fun onGoalValueChange(value: Int) {
        _uiState.update { it.copy(goalValue = value) }
        revalidate()
    }

    // Bấm Thử lại trong hộp thoại lỗi thời tiết
    fun retryWeather() = fetchWeather(manual = true)

    // Bấm Lấy lại thời tiết (như S4). Giữ dữ liệu cũ nếu lỗi
    fun refreshWeather() = fetchWeather(manual = true)

    // Bấm Tiếp tục không có thời tiết
    fun dismissWeatherError() {
        _uiState.update { it.copy(weatherFailed = false) }
    }

    fun save() {
        submitted = true
        val state = _uiState.value
        val current = existing
        // Đang sửa mà không có kế hoạch, hoặc kế hoạch không cho sửa (SPEC 6.4): không lưu
        if (planId != null && current == null) return
        if (current != null && !PlanRules.canEdit(current.status)) return

        val location = state.location
        if (location == null) {
            _uiState.update { it.copy(locationMissing = true) }
            return
        }

        val startTime = state.start.atZone(zone).toInstant().toEpochMilli()
        val errors = errorsFor(state, startTime)
        _uiState.update { it.copy(errors = errors, locationMissing = false) }
        if (errors.isNotEmpty()) return

        viewModelScope.launch {
            if (current == null) {
                planRepository.addPlan(
                    RunPlan(
                        startTime = startTime,
                        locationName = location.name,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        goalType = state.goalType,
                        goalValue = state.goalValue,
                        status = PlanStatus.PLANNED,
                        createdAt = clock(),
                        // Gắn thời tiết đã lấy nếu khớp giờ và vị trí đang chọn (SPEC 5.2). Chưa có thì để trống
                        weather = state.weather,
                    )
                )
            } else {
                val sameWhere = current.latitude == location.latitude && current.longitude == location.longitude
                planRepository.updatePlan(
                    current.copy(
                        startTime = startTime,
                        locationName = location.name,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        goalType = state.goalType,
                        goalValue = state.goalValue,
                        status = PlanRules.statusAfterEdit(current.status),
                        // Thời tiết đã lấy cho giờ và vị trí mới được ưu tiên. Không có thì giữ thời tiết cũ nếu không đổi gì
                        weather = state.weather
                            ?: if (current.startTime == startTime && sameWhere) current.weather else null,
                    )
                )
            }
            _events.trySend(PlanEditEvent.Saved)
        }
    }

    private fun changeStart(change: (LocalDateTime) -> LocalDateTime) {
        _uiState.update { it.copy(start = change(it.start)) }
        revalidate()
        fetchWeather()
    }

    // Lấy thời tiết cho giờ chẵn và vị trí đang chọn.
    // Tự động (manual = false): đổi giờ hoặc vị trí thì xoá dữ liệu cũ, chờ 400 ms như ô tìm, rồi lấy lại.
    // Quay về đúng giờ và vị trí đã lưu thì dùng lại dữ liệu đã lưu.
    // Bấm nút (manual = true): lấy lại ngay, giữ dữ liệu cũ nếu lỗi
    private fun fetchWeather(manual: Boolean = false) {
        val state = _uiState.value
        val location = state.location ?: return
        val startMillis = state.start.atZone(zone).toInstant().toEpochMilli()
        val key = Triple(WeatherRules.forecastHour(startMillis, zone), location.latitude, location.longitude)
        if (!manual && key == weatherKey) return
        weatherKey = key
        weatherJob?.cancel()

        val saved = savedWeather
        if (!manual && key == savedKey && saved != null) {
            _uiState.update {
                it.copy(weather = saved, weatherLoading = false, weatherOffline = false, weatherFailed = false)
            }
            return
        }

        _uiState.update {
            if (manual) {
                it.copy(weatherLoading = true, weatherFailed = false)
            } else {
                it.copy(weather = null, weatherLoading = true, weatherOffline = false, weatherFailed = false)
            }
        }
        weatherJob = viewModelScope.launch {
            if (!manual) delay(WEATHER_DELAY_MS)
            val result = weatherRepository.fetch(location.latitude, location.longitude, startMillis, zone)
            _uiState.update {
                when (result) {
                    is ForecastResult.Success -> {
                        // Dữ liệu mới của đúng giờ và vị trí đã lưu thì cập nhật luôn bản đã lưu
                        if (key == savedKey) savedWeather = result.snapshot
                        it.copy(weather = result.snapshot, weatherLoading = false, weatherOffline = false)
                    }
                    ForecastResult.Offline -> it.copy(weatherLoading = false, weatherOffline = true)
                    // Lỗi khác không phải mất mạng: nhãn "Không có mạng" của lần trước không còn đúng
                    ForecastResult.Failed -> it.copy(weatherLoading = false, weatherOffline = false, weatherFailed = true)
                }
            }
        }
    }

    private fun revalidate() {
        if (!submitted) return
        val state = _uiState.value
        val startTime = state.start.atZone(zone).toInstant().toEpochMilli()
        _uiState.update { it.copy(errors = errorsFor(state, startTime)) }
    }

    private fun errorsFor(state: PlanEditUiState, startTime: Long): List<PlanError> =
        PlanRules.validate(startTime, state.goalType, state.goalValue, clock())

    companion object {
        private const val WEATHER_DELAY_MS = 400L

        fun factory(planId: Long?, initialDay: LocalDate?) = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RunMateApplication).container
                PlanEditViewModel(container.planRepository, container.weatherRepository, planId, initialDay)
            }
        }
    }
}
