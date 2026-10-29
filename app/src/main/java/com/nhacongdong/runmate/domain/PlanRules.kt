package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.domain.model.PlanError

// Quy tắc kế hoạch (SPEC 5.1, 6.3, 6.4). Hàm thuần: giờ hiện tại luôn truyền vào qua `now` (SPEC 10.7).
object PlanRules {
    const val GRACE_PERIOD_MS = 30 * 60_000L          // BR-09
    const val MAX_DAYS_AHEAD = 16                      // BR-02
    private const val MINUTE_MS = 60_000L
    private const val DAY_MS = 24 * 60 * MINUTE_MS

    private val DISTANCE_RANGE_M = 500..100_000        // BR-04: 0,5–100 km
    private const val DISTANCE_STEP_M = 100            // bước 0,1 km
    private val DURATION_RANGE_S = 300..43_200         // BR-05: 5 phút–12 giờ
    private const val DURATION_STEP_S = 60             // bước 1 phút

    // BR-01..05. Trả về danh sách rỗng nghĩa là hợp lệ
    fun validate(startTime: Long, goalType: GoalType, goalValue: Int, now: Long): List<PlanError> {
        val errors = mutableListOf<PlanError>()
        // BR-01 so theo phút: chọn 06:00 rồi bấm Lưu lúc 06:00:20 vẫn hợp lệ
        if (startTime < now - now % MINUTE_MS) errors += PlanError.START_IN_PAST
        if (startTime > now + MAX_DAYS_AHEAD * DAY_MS) errors += PlanError.START_TOO_FAR
        when (goalType) {
            GoalType.DISTANCE ->
                if (goalValue !in DISTANCE_RANGE_M || goalValue % DISTANCE_STEP_M != 0) {
                    errors += PlanError.DISTANCE_OUT_OF_RANGE
                }
            GoalType.DURATION ->
                if (goalValue !in DURATION_RANGE_S || goalValue % DURATION_STEP_S != 0) {
                    errors += PlanError.DURATION_OUT_OF_RANGE
                }
        }
        return errors
    }

    // BR-09: quá giờ khi now > startTime + 30 phút. Đúng mốc 30 phút thì chưa quá giờ
    fun isOverdue(startTime: Long, now: Long): Boolean = now > startTime + GRACE_PERIOD_MS

    // SPEC 6.3: PLANNED quá giờ thì thành MISSED, trạng thái khác giữ nguyên
    fun refreshStatus(status: PlanStatus, startTime: Long, now: Long): PlanStatus =
        if (status == PlanStatus.PLANNED && isOverdue(startTime, now)) PlanStatus.MISSED else status

    // SPEC 6.3: bỏ buổi chạy quá ngắn (BR-36) hoặc xoá buổi chạy (BR-53)
    fun statusAfterSessionRemoved(startTime: Long, now: Long): PlanStatus =
        if (isOverdue(startTime, now)) PlanStatus.MISSED else PlanStatus.PLANNED

    // SPEC 6.4
    fun canEdit(status: PlanStatus): Boolean =
        status == PlanStatus.PLANNED || status == PlanStatus.MISSED || status == PlanStatus.CANCELLED

    fun canDelete(status: PlanStatus): Boolean = status != PlanStatus.IN_PROGRESS

    fun canCancel(status: PlanStatus): Boolean =
        status == PlanStatus.PLANNED || status == PlanStatus.MISSED

    // BR-08 (chỉ một buổi chạy cùng lúc) cần database nên kiểm tra riêng ở use case D4
    fun canStart(status: PlanStatus): Boolean =
        status == PlanStatus.PLANNED || status == PlanStatus.MISSED
}
