package com.nhacongdong.runmate.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nhacongdong.runmate.RunMateApplication
import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.domain.PlanRules
import com.nhacongdong.runmate.domain.RefreshMissedPlans
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PlanDetailEvent {
    data object Deleted : PlanDetailEvent
}

// S4 Chi tiết kế hoạch (SPEC 6.3, 6.4). Bật tắt mục menu lấy từ PlanRules
class PlanDetailViewModel(
    private val planId: Long,
    private val planRepository: PlanRepository,
    private val refreshMissedPlans: RefreshMissedPlans,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    // Null khi kế hoạch không còn trong database
    val plan: StateFlow<RunPlan?> = planRepository.observePlan(planId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = Channel<PlanDetailEvent>(Channel.BUFFERED)
    val events: Flow<PlanDetailEvent> = _events.receiveAsFlow()

    // Mỗi lần S4 hiện lên: đổi PLANNED quá giờ thành MISSED (SPEC 6.3)
    fun refreshStatus() {
        viewModelScope.launch { refreshMissedPlans(clock()) }
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
                PlanDetailViewModel(planId, container.planRepository, container.refreshMissedPlans)
            }
        }
    }
}
