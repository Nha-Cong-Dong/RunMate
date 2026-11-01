package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.model.SessionStatus
import kotlin.math.abs

// Quy tắc chuyển trạng thái khi bắt đầu, tạm dừng, tiếp tục, kết thúc buổi chạy (SPEC 6.3, BR-08, BR-36).
// Hàm thuần. Trả về null khi thao tác không được phép ở trạng thái hiện tại
object RunRules {

    // BR-08: chỉ được bắt đầu khi không có buổi chạy nào đang RECORDING hoặc PAUSED
    fun canStartRun(hasActiveSession: Boolean): Boolean = !hasActiveSession

    // Bắt đầu theo kế hoạch: PLANNED hoặc MISSED thành IN_PROGRESS (SPEC 6.3)
    fun planAfterStart(status: PlanStatus): PlanStatus? = when (status) {
        PlanStatus.PLANNED, PlanStatus.MISSED -> PlanStatus.IN_PROGRESS
        else -> null
    }

    // Tạm dừng: RECORDING thành PAUSED
    fun sessionAfterPause(status: SessionStatus): SessionStatus? = when (status) {
        SessionStatus.RECORDING -> SessionStatus.PAUSED
        else -> null
    }

    // Tiếp tục: PAUSED thành RECORDING
    fun sessionAfterResume(status: SessionStatus): SessionStatus? = when (status) {
        SessionStatus.PAUSED -> SessionStatus.RECORDING
        else -> null
    }

    // Kết thúc: buổi chạy đủ điều kiện (BR-36) thì kế hoạch thành COMPLETED.
    // Không đủ thì không lưu, kế hoạch về PLANNED nếu chưa quá giờ, MISSED nếu đã quá giờ (SPEC 6.3)
    fun planAfterFinish(saved: Boolean, startTime: Long, now: Long): PlanStatus =
        if (saved) PlanStatus.COMPLETED else PlanRules.statusAfterSessionRemoved(startTime, now)

    // BR-44: gợi ý chạy theo kế hoạch trước giờ bắt đầu 30 phút (đề xuất trong SPEC)
    const val SUGGEST_BEFORE_MS = 30 * 60_000L

    // BR-44: chọn kế hoạch PLANNED có giờ nằm trong khoảng gợi ý, từ 30 phút trước đến ân hạn BR-09 sau giờ bắt đầu.
    // Có nhiều kế hoạch khớp thì lấy kế hoạch có giờ gần bây giờ nhất
    fun suggestedPlan(plans: List<RunPlan>, now: Long): RunPlan? =
        plans
            .filter { it.status == PlanStatus.PLANNED }
            .filter { now in (it.startTime - SUGGEST_BEFORE_MS)..(it.startTime + PlanRules.GRACE_PERIOD_MS) }
            .minByOrNull { abs(it.startTime - now) }

    // BR-60: buổi bị gián đoạn quá 1 giờ kể từ điểm GPS cuối thì chỉ còn Kết thúc (SPEC 10.1)
    const val RESUME_LIMIT_MS = 60 * 60_000L

    fun canResumeInterrupted(lastPointTime: Long?, startedAt: Long, now: Long): Boolean =
        now - (lastPointTime ?: startedAt) <= RESUME_LIMIT_MS

    // SPEC 10.1: giờ kết thúc của buổi bị gián đoạn là điểm GPS cuối, không phải lúc mở app
    fun endedAtAfterInterruption(lastPointTime: Long?, startedAt: Long): Long = lastPointTime ?: startedAt
}
