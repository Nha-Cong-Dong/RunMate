package com.nhacongdong.runmate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nhacongdong.runmate.RunMateApplication
import com.nhacongdong.runmate.data.SessionRepository
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.domain.FinishRun
import com.nhacongdong.runmate.domain.RunRules
import com.nhacongdong.runmate.domain.TrackRules
import com.nhacongdong.runmate.service.RunServiceControl
import com.nhacongdong.runmate.service.RunTrackingService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Buổi chạy dở đã mất service, để hỏi người dùng Tiếp tục hay Kết thúc (SPEC 10.1, BR-60)
data class InterruptedRun(
    val sessionId: Long,
    val status: SessionStatus,   // RECORDING hoặc PAUSED
    val startedAt: Long,
    val distanceM: Double,
    val durationMs: Long,
    val canResume: Boolean,      // false khi quá 1 giờ từ điểm GPS cuối (BR-60)
    val endedAt: Long,           // giờ kết thúc nếu chọn Kết thúc: điểm GPS cuối (SPEC 10.1)
)

// Trạng thái dùng chung cho khung chính: có buổi chạy dở hay không (BR-08, DESIGN 8.1), và buổi bị gián đoạn cần hỏi (SPEC 10.1)
class RunMateAppViewModel(
    private val sessions: SessionRepository,
    private val finishRun: FinishRun,
    private val startService: (Long) -> Unit,
    private val pauseService: () -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val hasActiveRun: StateFlow<Boolean> = sessions.observeActiveSession()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _interrupted = MutableStateFlow<InterruptedRun?>(null)
    val interrupted: StateFlow<InterruptedRun?> = _interrupted.asStateFlow()

    // Yêu cầu mở S6 sau khi chọn Tiếp tục
    private val _openRun = Channel<Unit>(Channel.BUFFERED)
    val openRun: Flow<Unit> = _openRun.receiveAsFlow()

    // Gọi mỗi khi app hiện lên. Có buổi dở mà service không chạy thì chuẩn bị hộp thoại (SPEC 10.1)
    fun refresh() {
        viewModelScope.launch {
            val active = sessions.getActiveSession()
            if (active == null || RunTrackingService.isRunning) {
                _interrupted.value = null
                return@launch
            }
            val points = sessions.getTrackPoints(active.id)
            val lastPointTime = sessions.lastPoint(active.id)?.timestamp
            _interrupted.value = InterruptedRun(
                sessionId = active.id,
                status = active.status,
                startedAt = active.startedAt,
                distanceM = TrackRules.totalDistanceM(points),
                durationMs = TrackRules.activeDurationMs(points),
                canResume = RunRules.canResumeInterrupted(lastPointTime, active.startedAt, clock()),
                endedAt = RunRules.endedAtAfterInterruption(lastPointTime, active.startedAt),
            )
        }
    }

    // Tiếp tục: bật lại service với đúng buổi chạy, mở S6. Buổi đang tạm dừng thì vẫn tạm dừng
    fun resume() {
        val run = _interrupted.value ?: return
        _interrupted.value = null
        startService(run.sessionId)
        if (run.status == SessionStatus.PAUSED) pauseService()
        _openRun.trySend(Unit)
    }

    // Kết thúc: lưu hoặc xoá theo BR-36, giờ kết thúc là điểm GPS cuối
    fun finish() {
        val run = _interrupted.value ?: return
        _interrupted.value = null
        viewModelScope.launch {
            finishRun(run.sessionId, run.endedAt)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunMateApplication
                val container = app.container
                RunMateAppViewModel(
                    sessions = container.sessionRepository,
                    finishRun = container.finishRun,
                    startService = { id -> RunServiceControl.start(app, id) },
                    pauseService = { RunServiceControl.pause(app) },
                )
            }
        }
    }
}
