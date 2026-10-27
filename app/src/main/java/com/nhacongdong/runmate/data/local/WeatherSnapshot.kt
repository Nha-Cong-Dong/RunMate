package com.nhacongdong.runmate.data.local

// Nhóm cột thời tiết gộp vào run_plan và run_session bằng @Embedded (SPEC 7.5).
// Cả nhóm null khi chưa lấy được thời tiết.
data class WeatherSnapshot(
    val weatherFetchedAt: Long,
    val temperatureC: Double,
    val precipitationProb: Int,
    val windSpeedKmh: Double,
    val uvIndex: Double
)
