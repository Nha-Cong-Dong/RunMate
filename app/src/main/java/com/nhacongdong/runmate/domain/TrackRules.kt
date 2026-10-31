package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.TrackPoint
import com.nhacongdong.runmate.data.model.GoalType
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// Quy tắc ghi buổi chạy (SPEC 5.3, BR-30 đến BR-38). Hàm thuần: nhận điểm GPS và thời gian, không đọc đồng hồ hay GPS.
// Thời gian tính bằng mili giây, quãng đường bằng mét, goalValue theo đơn vị của domain (mét hoặc giây)
object TrackRules {
    const val MAX_ACCURACY_M = 30f          // BR-30
    const val MAX_SPEED_MPS = 12.0          // BR-31: khoảng 43 km/h
    const val MIN_DISTANCE_M = 100.0        // BR-35, BR-36
    const val MIN_DURATION_MS = 60_000L     // BR-36
    private const val EARTH_RADIUS_M = 6_371_000.0

    // BR-30: bỏ điểm có sai số lớn hơn 30 m
    fun isAccurate(accuracyM: Float): Boolean = accuracyM <= MAX_ACCURACY_M

    // BR-31: bước giữa hai điểm không được vượt 12 m/s. Hai điểm cùng thời điểm thì không hợp lệ
    fun isPlausibleStep(previous: TrackPoint, next: TrackPoint): Boolean {
        val seconds = (next.timestamp - previous.timestamp) / 1_000.0
        if (seconds <= 0.0) return false
        val speed = distanceM(previous, next) / seconds
        return speed <= MAX_SPEED_MPS
    }

    // Giữ điểm đủ chính xác, và bỏ điểm nhảy so với điểm hợp lệ gần nhất trước đó
    fun filterValid(points: List<TrackPoint>): List<TrackPoint> {
        val valid = mutableListOf<TrackPoint>()
        for (point in points) {
            if (!isAccurate(point.accuracyM)) continue
            val last = valid.lastOrNull()
            if (last == null || isPlausibleStep(last, point)) valid += point
        }
        return valid
    }

    // BR-33: cộng khoảng cách giữa các điểm liên tiếp trong cùng một đoạn. Không nối qua lúc tạm dừng
    fun totalDistanceM(points: List<TrackPoint>): Double =
        points.groupBy { it.segmentIndex }.values.sumOf { segment ->
            segment.zipWithNext { a, b -> distanceM(a, b) }.sum()
        }

    // BR-34: thời gian chạy là tổng thời gian của từng đoạn, không tính lúc tạm dừng
    fun activeDurationMs(points: List<TrackPoint>): Long =
        points.groupBy { it.segmentIndex }.values.sumOf { segment ->
            segment.last().timestamp - segment.first().timestamp
        }

    // BR-35: phút mỗi km tính bằng giây. Chưa đủ 100 m thì null (hiện "--")
    fun paceSecPerKm(distanceM: Double, durationMs: Long): Double? {
        if (distanceM < MIN_DISTANCE_M) return null
        return (durationMs / 1_000.0) / (distanceM / 1_000.0)
    }

    // BR-36: buổi chạy phải đủ 100 m và 1 phút mới được lưu
    fun isSavable(distanceM: Double, durationMs: Long): Boolean =
        distanceM >= MIN_DISTANCE_M && durationMs >= MIN_DURATION_MS

    // BR-37: quãng đường hoặc thời gian chạy đạt mục tiêu
    fun reachedGoal(goalType: GoalType, goalValue: Int, distanceM: Double, durationMs: Long): Boolean =
        when (goalType) {
            GoalType.DISTANCE -> distanceM >= goalValue
            GoalType.DURATION -> durationMs >= goalValue * 1_000L
        }

    // BR-38: báo một lần, chỉ khi lần này vừa đạt mục tiêu
    fun justReached(wasReached: Boolean, isReached: Boolean): Boolean = !wasReached && isReached

    // Khoảng cách trên mặt đất giữa hai điểm (công thức haversine), đơn vị mét
    private fun distanceM(a: TrackPoint, b: TrackPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }
}
