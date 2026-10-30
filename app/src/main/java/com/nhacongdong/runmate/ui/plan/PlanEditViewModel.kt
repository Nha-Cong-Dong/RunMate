package com.nhacongdong.runmate.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nhacongdong.runmate.RunMateApplication
import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.domain.PlanRules
import com.nhacongdong.runmate.domain.model.Place
import com.nhacongdong.runmate.domain.model.PlanError
import com.nhacongdong.runmate.ui.components.DEFAULT_DISTANCE_M
import com.nhacongdong.runmate.ui.components.DEFAULT_DURATION_S
import kotlinx.coroutines.channels.Channel
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
)

sealed interface PlanEditEvent {
    data object Saved : PlanEditEvent
}

class PlanEditViewModel(
    private val planRepository: PlanRepository,
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
                        weather = null,
                    )
                )
            } else {
                // Đổi giờ hoặc địa điểm thì thời tiết đã lưu không còn đúng. Thời tiết lấy lại ở C3
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
                        weather = if (current.startTime == startTime && sameWhere) current.weather else null,
                    )
                )
            }
            _events.trySend(PlanEditEvent.Saved)
        }
    }

    private fun changeStart(change: (LocalDateTime) -> LocalDateTime) {
        _uiState.update { it.copy(start = change(it.start)) }
        revalidate()
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
        fun factory(planId: Long?, initialDay: LocalDate?) = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RunMateApplication).container
                PlanEditViewModel(container.planRepository, planId, initialDay)
            }
        }
    }
}
