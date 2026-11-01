package com.nhacongdong.runmate.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nhacongdong.runmate.RunMateApplication
import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.SessionRepository
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.local.RunSession
import com.nhacongdong.runmate.data.local.TrackPoint
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.domain.PlanRules
import com.nhacongdong.runmate.domain.RunSummary
import com.nhacongdong.runmate.domain.RunSummaryRules
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Trạng thái của S7: buổi chạy, các điểm GPS, số liệu tính từ chúng, và kế hoạch gắn với buổi (SPEC 9.1, DESIGN 9 S7)
data class SessionDetailUiState(
    val session: RunSession? = null,
    val points: List<TrackPoint> = emptyList(),
    val summary: RunSummary? = null,
    val plan: RunPlan? = null,
)

// Việc chỉ xảy ra một lần: đã xoá buổi chạy hoặc kế hoạch thì về màn trước (SPEC 9.3)
sealed interface SessionDetailEvent {
    data object Deleted : SessionDetailEvent
}

// Đọc một buổi chạy để S7 hiển thị và xoá (SPEC UC6). Số liệu tính từ điểm GPS, giống S6
class SessionDetailViewModel(
    private val sessions: SessionRepository,
    private val planRepository: PlanRepository,
    sessionId: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val uiState: StateFlow<SessionDetailUiState> = combine(
        sessions.observeSession(sessionId),
        sessions.observeTrackPoints(sessionId),
        planRepository.observePlans(),
    ) { session, points, plans ->
        SessionDetailUiState(
            session = session,
            points = points,
            summary = session?.let { RunSummaryRules.summarize(points, it.goalType, it.goalValue) },
            plan = session?.planId?.let { id -> plans.firstOrNull { it.id == id } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())

    private val _events = Channel<SessionDetailEvent>(Channel.BUFFERED)
    val events: Flow<SessionDetailEvent> = _events.receiveAsFlow()

    // Xoá buổi chạy (BR-54). Nếu thuộc kế hoạch thì kế hoạch về PLANNED hoặc MISSED (BR-53)
    fun deleteSession() {
        val state = uiState.value
        val session = state.session ?: return
        val plan = state.plan
        val statusAfter: PlanStatus? = plan?.let { PlanRules.statusAfterSessionRemoved(it.startTime, clock()) }
        viewModelScope.launch {
            sessions.deleteSession(session, statusAfter)
            _events.trySend(SessionDetailEvent.Deleted)
        }
    }

    // Xoá kế hoạch đã hoàn thành (BR-52): buổi chạy được giữ lại thành chạy tự do
    fun deletePlan() {
        val plan = uiState.value.plan ?: return
        if (plan.status != PlanStatus.COMPLETED) return
        viewModelScope.launch {
            planRepository.deletePlan(plan)
            _events.trySend(SessionDetailEvent.Deleted)
        }
    }

    companion object {
        fun factory(sessionId: Long) = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RunMateApplication).container
                SessionDetailViewModel(container.sessionRepository, container.planRepository, sessionId)
            }
        }
    }
}
