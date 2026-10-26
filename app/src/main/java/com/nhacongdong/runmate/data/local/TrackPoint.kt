package com.nhacongdong.runmate.data.local

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

// Bảng track_point: điểm GPS đã qua lọc BR-30, BR-31 (SPEC 7.4, 7.7).
@Entity(
    tableName = "track_point",
    foreignKeys = [ForeignKey(
        entity = RunSession::class, parentColumns = ["id"], childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val segmentIndex: Int,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float
)
