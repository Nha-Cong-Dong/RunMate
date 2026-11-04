package com.nhacongdong.runmate.ui.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nhacongdong.runmate.RecentlyFinishedRun
import com.nhacongdong.runmate.RunMateApplication
import com.nhacongdong.runmate.data.SessionRepository
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.domain.FinishRun
import com.nhacongdong.runmate.domain.GpsDropCounter
import com.nhacongdong.runmate.domain.PauseRun
import com.nhacongdong.runmate.domain.ResumeRun
import com.nhacongdong.runmate.domain.RunSummary
import com.nhacongdong.runmate.domain.RunSummaryRules
import com.nhacongdong.runmate.domain.TrackRules
import com.nhacongdong.runmate.service.RunServiceControl
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Trạng thái của S6 Đang chạy (SPEC mục 9 S6, DESIGN 9 S6)
data class RunUiState(
    val sessionId: Long? = null,
    val status: SessionStatus? = null,   // RECORDING hoặc PAUSED
    val summary: RunSummary? = null,     // số liệu sống (D6-1)
    val goalType: GoalType = GoalType.DISTANCE,
    val goalValue: Int = 0,
    val locationName: String = "",
    val gpsWeak: Boolean = false,        // 3 điểm liên tiếp bị bỏ (BR-30), đổi chỉ báo GPS sang cảnh báo
)

// Việc chỉ xảy ra một lần: báo đạt mục tiêu, kết thúc đã lưu, buổi quá ngắn, không có buổi chạy nào
sealed interface RunEvent {
    data object GoalReached : RunEvent                 // BR-38
    data class Saved(val sessionId: Long) : RunEvent   // SPEC 9.3: sang S7
    data object TooShort : RunEvent                    // BR-36: báo rồi quay về màn trước
    data object NoActiveRun : RunEvent                 // không có buổi dở để hiện
}

// S6: theo dõi buổi chạy đang dở, tạm dừng, tiếp tục, kết thúc. Gọi service qua các hàm onPause, onResume, onStop
class RunViewModel(
    private val sessions: SessionRepository,
    private val gpsDrops: GpsDropCounter,
    private val recentlyFinished: RecentlyFinishedRun,
    private val pauseRun: PauseRun,
    private val resumeRun: ResumeRun,
    private val finishRun: FinishRun,
    private val onPause: () -> Unit,
    private val onResume: (Long) -> Unit,
    private val onStop: () -> Unit,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunUiState())
    val uiState: StateFlow<RunUiState> = _uiState.asStateFlow()

    private val _events = Channel<RunEvent>(Channel.BUFFERED)
    val events: Flow<RunEvent> = _events.receiveAsFlow()

    private var goalReachedBefore = false
    private var binding: Job? = null

    init {
        viewModelScope.launch {
            val active = sessions.getActiveSession()
            if (active == null) _events.trySend(RunEvent.NoActiveRun) else bind(active.id)
        }
    }

    // Theo dõi trạng thái buổi chạy và điểm GPS, rồi tính số liệu sống mỗi khi có thay đổi
    private fun bind(sessionId: Long) {
        _uiState.update { it.copy(sessionId = sessionId) }
        binding?.cancel()
        binding = viewModelScope.launch {
            combine(
                sessions.observeSession(sessionId),
                sessions.observeTrackPoints(sessionId),
                gpsDrops.consecutive,
            ) { session, points, drops ->
                Triple(session, points, drops)
            }.collect { (session, points, drops) ->
                if (session == null) return@collect   // buổi chạy đã bị xoá khi kết thúc
                val summary = RunSummaryRules.summarize(points, session.goalType, session.goalValue)
                if (TrackRules.justReached(goalReachedBefore, summary.goalReached)) {
                    _events.trySend(RunEvent.GoalReached)
                }
                goalReachedBefore = summary.goalReached
                _uiState.update {
                    it.copy(
                        status = session.status,
                        summary = summary,
                        goalType = session.goalType,
                        goalValue = session.goalValue,
                        locationName = session.locationName,
                        gpsWeak = GpsDropCounter.isWeak(drops),
                    )
                }
            }
        }
    }

    fun pause() {
        val id = _uiState.value.sessionId ?: return
        viewModelScope.launch { if (pauseRun(id)) onPause() }
    }

    fun resume() {
        val id = _uiState.value.sessionId ?: return
        viewModelScope.launch { if (resumeRun(id)) onResume(id) }
    }

    // Kết thúc sau khi người dùng xác nhận. Dừng service rồi báo kết quả
    fun finish() {
        val id = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            // Dừng GPS trước, rồi mới lưu hoặc xoá buổi chạy, để không có điểm nào được ghi vào buổi đã mất
            onStop()
            val result = finishRun(id) ?: return@launch
            // Buổi đã lưu thì S1 sẽ vẽ hiệu ứng vòng một lần (DESIGN 3.4)
            if (result.saved) recentlyFinished.mark(id)
            _events.trySend(if (result.saved) RunEvent.Saved(id) else RunEvent.TooShort)
        }
    }

    companion object {
        fun factory() = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunMateApplication
                val container = app.container
                RunViewModel(
                    sessions = container.sessionRepository,
                    gpsDrops = container.gpsDrops,
                    recentlyFinished = container.recentlyFinished,
                    pauseRun = container.pauseRun,
                    resumeRun = container.resumeRun,
                    finishRun = container.finishRun,
                    onPause = { RunServiceControl.pause(app) },
                    onResume = { id -> RunServiceControl.resume(app, id) },
                    onStop = { RunServiceControl.stop(app) },
                )
            }
        }
    }
}
