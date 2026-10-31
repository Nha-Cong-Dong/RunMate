package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.TrackPoint

// Nơi lưu điểm GPS của buổi chạy. SessionRepository cài đặt interface này
interface TrackPointStore {
    suspend fun lastPoint(sessionId: Long): TrackPoint?
    suspend fun addPoint(point: TrackPoint)
}

// Ghi một điểm GPS thô của buổi chạy (SPEC 5.3, BR-30, BR-31, BR-39).
// Chỉ điểm được giữ mới vào database. Điểm bị bỏ thì không ghi gì
class RecordTrackPoint(private val store: TrackPointStore) {

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
        if (!TrackRules.shouldKeep(last, candidate)) return false
        store.addPoint(candidate.copy(segmentIndex = TrackRules.segmentFor(last, candidate)))
        return true
    }
}
