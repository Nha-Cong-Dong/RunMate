package com.nhacongdong.runmate.data.local

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.nhacongdong.runmate.domain.model.GoalType
import com.nhacongdong.runmate.domain.model.PlanStatus

// Bảng run_plan: kế hoạch chạy (SPEC 7.2).
@Entity(tableName = "run_plan")
data class RunPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val goalType: GoalType,
    val goalValue: Int,
    val status: PlanStatus,
    val createdAt: Long,
    @Embedded val weather: WeatherSnapshot?
)
