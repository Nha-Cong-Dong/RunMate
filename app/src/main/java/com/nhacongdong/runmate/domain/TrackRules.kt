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
    const val MAX_GAP_MS = 30_000L          // BR-39: khoảng trống GPS trên 30 giây thì mở đoạn mới
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

    // BR-39: khoảng cách thời gian giữa hai điểm vượt 30 giây thì mở đoạn mới
    fun isNewSegment(last: TrackPoint, point: TrackPoint): Boolean =
        point.timestamp - last.timestamp > MAX_GAP_MS

    // Số đoạn của điểm mới: điểm đầu tiên là đoạn 0, khoảng trống lớn thì tăng số đoạn
    fun segmentFor(last: TrackPoint?, point: TrackPoint): Int = when {
        last == null -> 0
        isNewSegment(last, point) -> last.segmentIndex + 1
        else -> last.segmentIndex
    }

    // Giữ điểm đủ chính xác (BR-30). Sau khoảng trống lớn thì mở đoạn mới và không kiểm tra tốc độ (BR-39).
    // Trong các trường hợp còn lại thì phải đạt BR-31 so với điểm trước
    fun shouldKeep(last: TrackPoint?, point: TrackPoint): Boolean {
        if (!isAccurate(point.accuracyM)) return false
        if (last == null || isNewSegment(last, point)) return true
        return isPlausibleStep(last, point)
    }

    // Giữ điểm hợp lệ theo shouldKeep, so với điểm được giữ gần nhất
    fun filterValid(points: List<TrackPoint>): List<TrackPoint> {
        val valid = mutableListOf<TrackPoint>()
        for (point in points) {
            val last = valid.lastOrNull()
            if (shouldKeep(last, point)) valid += point.copy(segmentIndex = segmentFor(last, point))
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
