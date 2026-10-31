package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus

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
}
