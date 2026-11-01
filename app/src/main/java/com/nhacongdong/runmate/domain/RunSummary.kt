package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.TrackPoint
import com.nhacongdong.runmate.data.model.GoalType

// Số liệu sống của buổi chạy đang ghi (SPEC 5.3, BR-33 đến BR-38). Hàm thuần
data class RunSummary(
    val distanceM: Double,
    val durationMs: Long,
    val paceSecPerKm: Double?,   // null khi chưa đủ 100 m (BR-35)
    val progress: Double,        // tỷ lệ đạt mục tiêu (BR-37), có thể trên 1
    val goalReached: Boolean,
)

object RunSummaryRules {

    // Tính số liệu từ các điểm GPS đã lưu. Điểm đã được lọc khi ghi (BR-30, BR-31, BR-39)
    fun summarize(points: List<TrackPoint>, goalType: GoalType, goalValue: Int): RunSummary {
        val distanceM = TrackRules.totalDistanceM(points)
        val durationMs = TrackRules.activeDurationMs(points)
        val progress = when (goalType) {
            GoalType.DISTANCE -> distanceM / goalValue
            GoalType.DURATION -> durationMs.toDouble() / (goalValue * 1_000.0)
        }
        return RunSummary(
            distanceM = distanceM,
            durationMs = durationMs,
            paceSecPerKm = TrackRules.paceSecPerKm(distanceM, durationMs),
            progress = progress,
            goalReached = TrackRules.reachedGoal(goalType, goalValue, distanceM, durationMs),
        )
    }
}
