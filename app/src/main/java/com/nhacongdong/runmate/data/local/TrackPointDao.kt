package com.nhacongdong.runmate.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

// Truy vấn bảng track_point.
@Dao
interface TrackPointDao {
    @Insert suspend fun insert(point: TrackPoint)

    // S7: vẽ quỹ đạo
    @Query("SELECT * FROM track_point WHERE sessionId = :sessionId ORDER BY timestamp")
    suspend fun getBySession(sessionId: Long): List<TrackPoint>

    // S6: số liệu sống, cập nhật mỗi khi có điểm mới
    @Query("SELECT * FROM track_point WHERE sessionId = :sessionId ORDER BY timestamp")
    fun observeBySession(sessionId: Long): Flow<List<TrackPoint>>

    // Điểm cuối đã ghi của buổi chạy, để so với điểm mới (BR-31, BR-39)
    @Query("SELECT * FROM track_point WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLast(sessionId: Long): TrackPoint?
}
