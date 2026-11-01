package com.nhacongdong.runmate.domain

// Số liệu tổng hợp của một kỳ thống kê (SPEC 5.5, BR-55, BR-56, DESIGN 9 S8).
data class PeriodSummary(
    val sessionCount: Int,         // Số buổi chạy đã hoàn thành trong kỳ (BR-56)
    val totalDistanceM: Double,    // Tổng quãng đường chạy (mét)
    val totalDurationSec: Long,    // Tổng thời gian chạy (giây, format qua RunFormat.formatDuration)
    val avgPaceSecPerKm: Double?,  // Pace TB tính theo giây/km (định dạng phút/km qua RunFormat.formatDuration; null khi chưa chạy)
    val completedPlans: Int,       // Số kế hoạch COMPLETED trong kỳ (BR-55)
    val missedPlans: Int,          // Số kế hoạch MISSED trong kỳ (BR-55)
    val planCompletionRate: Int?,  // Tỷ lệ % hoàn thành kế hoạch (làm tròn xuống; null khi không có kế hoạch nào -> hiện "--")
)
