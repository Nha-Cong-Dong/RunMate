package com.nhacongdong.runmate.data.local

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.SessionStatus

// Bảng run_session: buổi chạy (SPEC 7.3, 7.7).
@Entity(
    tableName = "run_session",
    foreignKeys = [ForeignKey(
        entity = RunPlan::class, parentColumns = ["id"], childColumns = ["planId"],
        onDelete = ForeignKey.SET_NULL                    // BR-52: xoá kế hoạch thì thành chạy tự do
    )],
    indices = [Index(value = ["planId"], unique = true)]  // mỗi kế hoạch tối đa 1 buổi
)
data class RunSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long?,                                    // null = chạy tự do
    val status: SessionStatus,
    val startedAt: Long,
    val endedAt: Long? = null,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val goalType: GoalType,
    val goalValue: Int,
    val distanceMeters: Double = 0.0,
    val activeDurationSec: Long = 0,
    val goalReachedAt: Long? = null,
    @Embedded val weather: WeatherSnapshot?
)
