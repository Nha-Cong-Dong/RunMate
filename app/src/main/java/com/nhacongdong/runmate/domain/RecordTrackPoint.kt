package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.TrackPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Nơi lưu điểm GPS của buổi chạy. SessionRepository cài đặt interface này
interface TrackPointStore {
    suspend fun lastPoint(sessionId: Long): TrackPoint?
    suspend fun addPoint(point: TrackPoint)
}

// Đếm số điểm GPS bị bỏ liên tiếp (BR-30), để S6 đổi chỉ báo GPS khi tín hiệu yếu (DESIGN 9 S6).
// Điểm được giữ thì đếm về 0
class GpsDropCounter {
    private val _consecutive = MutableStateFlow(0)
    val consecutive: StateFlow<Int> = _consecutive.asStateFlow()

    fun onKept() {
        _consecutive.value = 0
    }

    fun onDropped() {
        _consecutive.update { it + 1 }
    }

    companion object {
        // Từ 3 điểm liên tiếp bị bỏ thì coi là GPS yếu
        const val WEAK_STREAK = 3

        fun isWeak(consecutive: Int): Boolean = consecutive >= WEAK_STREAK
    }
}

// Ghi một điểm GPS thô của buổi chạy (SPEC 5.3, BR-30, BR-31, BR-39).
// Chỉ điểm được giữ mới vào database. Điểm bị bỏ thì không ghi gì, chỉ tăng bộ đếm
class RecordTrackPoint(
    private val store: TrackPointStore,
    private val drops: GpsDropCounter = GpsDropCounter(),
) {

    // Trả về true nếu điểm được giữ
    suspend operator fun invoke(
        sessionId: Long,
        timestamp: Long,
        latitude: Double,
        longitude: Double,
        accuracyM: Float,
    ): Boolean {
        val last = store.lastPoint(sessionId)
        val candidate = TrackPoint(
            sessionId = sessionId,
            segmentIndex = 0,
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            accuracyM = accuracyM,
        )
        if (!TrackRules.shouldKeep(last, candidate)) {
            drops.onDropped()
            return false
        }
        drops.onKept()
        store.addPoint(candidate.copy(segmentIndex = TrackRules.segmentFor(last, candidate)))
        return true
    }
}
