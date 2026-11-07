package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.SessionRepository
import com.nhacongdong.runmate.data.local.RunSession
import com.nhacongdong.runmate.data.local.WeatherSnapshot
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.SessionStatus
import kotlinx.coroutines.flow.first

// Kết quả khi kết thúc buổi chạy. saved = false thì buổi chạy đã bị bỏ (BR-36)
data class FinishResult(val saved: Boolean, val distanceM: Double, val durationMs: Long)

// Các use case bắt đầu, tạm dừng, tiếp tục, kết thúc buổi chạy (SPEC 6.3). Thời gian lấy từ clock để test được

// Bắt đầu buổi chạy theo kế hoạch. Trả về id buổi chạy, hoặc null nếu không bắt đầu được
class StartPlanRun(
    private val sessions: SessionRepository,
    private val plans: PlanRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(planId: Long): Long? {
        val plan = plans.getPlan(planId) ?: return null
        val planStatus = RunRules.planAfterStart(plan.status) ?: return null
        val session = RunSession(
            planId = planId,
            status = SessionStatus.RECORDING,
            startedAt = clock(),
            locationName = plan.locationName,
            latitude = plan.latitude,
            longitude = plan.longitude,
            goalType = plan.goalType,
            goalValue = plan.goalValue,
            weather = plan.weather,
        )
        return sessions.startSession(session, planId, planStatus)
    }
}

// Bắt đầu chạy tự do, không gắn kế hoạch (BR-43). Trả về id buổi chạy, hoặc null nếu đang có buổi chạy dở
class StartFreeRun(
    private val sessions: SessionRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(
        locationName: String,
        latitude: Double,
        longitude: Double,
        goalType: GoalType,
        goalValue: Int,
        weather: WeatherSnapshot? = null,
    ): Long? {
        val session = RunSession(
            planId = null,
            status = SessionStatus.RECORDING,
            startedAt = clock(),
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            goalType = goalType,
            goalValue = goalValue,
            weather = weather,
        )
        return sessions.startSession(session, planId = null, planStatus = null)
    }
}

// Tạm dừng buổi chạy đang ghi. Trả về false nếu không tạm dừng được
class PauseRun(private val sessions: SessionRepository) {
    suspend operator fun invoke(sessionId: Long): Boolean {
        val session = sessions.observeSession(sessionId).first() ?: return false
        val next = RunRules.sessionAfterPause(session.status) ?: return false
        sessions.updateSession(session.copy(status = next))
        return true
    }
}

// Tiếp tục buổi chạy đang tạm dừng. Trả về false nếu không tiếp tục được
class ResumeRun(private val sessions: SessionRepository) {
    suspend operator fun invoke(sessionId: Long): Boolean {
        val session = sessions.observeSession(sessionId).first() ?: return false
        val next = RunRules.sessionAfterResume(session.status) ?: return false
        sessions.updateSession(session.copy(status = next))
        return true
    }
}

// Kết thúc buổi chạy. Tính quãng đường và thời gian từ các điểm GPS (BR-33, BR-34).
// Đủ điều kiện lưu (BR-36) thì lưu, kế hoạch thành COMPLETED. Không đủ thì xoá buổi chạy, kế hoạch về PLANNED hoặc MISSED
class FinishRun(
    private val sessions: SessionRepository,
    private val plans: PlanRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    // endedAt: giờ kết thúc khi khôi phục buổi bị gián đoạn (SPEC 10.1). Không truyền thì là lúc bấm Kết thúc
    suspend operator fun invoke(sessionId: Long, endedAt: Long? = null): FinishResult? {
        val session = sessions.observeSession(sessionId).first() ?: return null
        if (session.status == SessionStatus.FINISHED) return null

        val points = sessions.getTrackPoints(sessionId)
        val distanceM = TrackRules.totalDistanceM(points)
        val durationMs = TrackRules.activeDurationMs(points)
        val saved = TrackRules.isSavable(distanceM, durationMs)
        val now = clock()
        val planStartTime = session.planId?.let { plans.getPlan(it)?.startTime }

        if (saved) {
            val planStatus = planStartTime?.let { RunRules.planAfterFinish(saved = true, startTime = it, now = now) }
            sessions.completeSession(
                session.copy(
                    status = SessionStatus.FINISHED,
                    endedAt = endedAt ?: now,
                    distanceMeters = distanceM,
                    activeDurationSec = durationMs / 1_000,
                ),
                planStatus,
            )
        } else {
            val planStatusAfter = planStartTime?.let { RunRules.planAfterFinish(saved = false, startTime = it, now = now) }
            sessions.deleteSession(session, planStatusAfter)
        }
        return FinishResult(saved, distanceM, durationMs)
    }
}
