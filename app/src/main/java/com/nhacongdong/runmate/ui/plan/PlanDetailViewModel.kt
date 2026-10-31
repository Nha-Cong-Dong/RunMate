package com.nhacongdong.runmate.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nhacongdong.runmate.RunMateApplication
import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.WeatherRepository
import com.nhacongdong.runmate.data.ForecastResult
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.domain.PlanRules
import com.nhacongdong.runmate.domain.RefreshMissedPlans
import com.nhacongdong.runmate.domain.WeatherRules
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

sealed interface PlanDetailEvent {
    data object Deleted : PlanDetailEvent

    // Lỗi thời tiết khác mất mạng: hiện hộp thoại (DESIGN 10). Mất mạng thì chỉ ghi nhãn
    data object WeatherFailed : PlanDetailEvent
}

// S4 Chi tiết kế hoạch (SPEC 5.2, 6.3, 6.4). Bật tắt mục menu lấy từ PlanRules.
// Thời tiết không tự lấy khi mở. Chỉ lấy khi người dùng bấm nút Lấy lại
class PlanDetailViewModel(
    private val planId: Long,
    private val planRepository: PlanRepository,
    private val refreshMissedPlans: RefreshMissedPlans,
    private val weatherRepository: WeatherRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    // Null khi kế hoạch không còn trong database
    val plan: StateFlow<RunPlan?> = planRepository.observePlan(planId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    // Lần bấm Lấy lại gần nhất không có mạng. Đổi lại khi bấm lần sau
    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    private val _events = Channel<PlanDetailEvent>(Channel.BUFFERED)
    val events: Flow<PlanDetailEvent> = _events.receiveAsFlow()

    // Mỗi lần S4 hiện lên: đổi PLANNED quá giờ thành MISSED (SPEC 6.3)
    fun refreshStatus() {
        viewModelScope.launch { refreshMissedPlans(clock()) }
    }

    // Bấm Lấy lại thời tiết (SPEC 5.2). Thành công thì lưu và ô số đổi theo
    fun refreshWeather() {
        val current = plan.value ?: return
        if (!WeatherRules.canRefresh(current.status) || _refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            val result = weatherRepository.fetch(current.latitude, current.longitude, current.startTime, zone)
            _refreshing.value = false
            when (result) {
                is ForecastResult.Success -> {
                    _offline.value = false
                    val latest = plan.value ?: return@launch
                    planRepository.updatePlan(latest.copy(weather = result.snapshot))
                }
                ForecastResult.Offline -> _offline.value = true
                ForecastResult.Failed -> {
                    // Lỗi khác không phải mất mạng: nhãn "Không có mạng" của lần trước không còn đúng
                    _offline.value = false
                    _events.trySend(PlanDetailEvent.WeatherFailed)
                }
            }
        }
    }

    fun cancelPlan() {
        val current = plan.value ?: return
        if (!PlanRules.canCancel(current.status)) return
        viewModelScope.launch { planRepository.updateStatus(planId, PlanStatus.CANCELLED) }
    }

    // Buổi chạy của kế hoạch đã hoàn thành được giữ lại thành chạy tự do nhờ SET_NULL (BR-52)
    fun deletePlan() {
        val current = plan.value ?: return
        if (!PlanRules.canDelete(current.status)) return
        viewModelScope.launch {
            planRepository.deletePlan(current)
            _events.trySend(PlanDetailEvent.Deleted)
        }
    }

    companion object {
        fun factory(planId: Long) = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RunMateApplication).container
                PlanDetailViewModel(
                    planId,
                    container.planRepository,
                    container.refreshMissedPlans,
                    container.weatherRepository,
                )
            }
        }
    }
}
