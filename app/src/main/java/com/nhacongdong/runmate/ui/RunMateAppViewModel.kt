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
import com.nhacongdong.runmate.domain.PauseRun
import com.nhacongdong.runmate.service.RunAlerts
import com.nhacongdong.runmate.service.RunServiceControl
import com.nhacongdong.runmate.ui.location.hasLocationPermission
import com.nhacongdong.runmate.ui.location.isLocationOn
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
    private val pauseRun: PauseRun,
    private val onAutoPaused: () -> Unit,
    private val isLocationReady: () -> Boolean,
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

    // Gọi mỗi khi app hiện lên (SPEC 10.1, 10.2a). Buổi đang ghi mà thiếu quyền vị trí hoặc công tắc Vị trí tắt,
    // hoặc service đã mất, thì tự chuyển sang tạm dừng rồi báo người dùng. Buổi đang ghi bình thường thì không làm gì
    fun refresh() {
        viewModelScope.launch {
            val found = sessions.getActiveSession() ?: run {
                _interrupted.value = null
                return@launch
            }
            val serviceRunning = RunTrackingService.isRunning
            val needsAutoPause = RunRules.needsAutoPause(found.status, serviceRunning, isLocationReady())
            if (serviceRunning && !needsAutoPause) {
                _interrupted.value = null
                return@launch
            }
            val active = if (needsAutoPause) {
                if (serviceRunning) pauseService()
                if (pauseRun(found.id)) {
                    onAutoPaused()
                    found.copy(status = SessionStatus.PAUSED)
                } else {
                    found
                }
            } else {
                found
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
                    pauseRun = container.pauseRun,
                    onAutoPaused = { RunAlerts.pausedAfterInterruption(app) },
                    isLocationReady = { app.hasLocationPermission() && app.isLocationOn() },
                )
            }
        }
    }
}
