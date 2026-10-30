package com.nhacongdong.runmate.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nhacongdong.runmate.data.local.RunMateDatabase
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.local.RunSession
import com.nhacongdong.runmate.data.local.TrackPoint
import com.nhacongdong.runmate.data.local.WeatherSnapshot
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus

// Dữ liệu mẫu dùng chung cho test. Mỗi test chỉ ghi trường mà nó quan tâm.

// Database trong RAM, mỗi lần gọi là một database mới
fun testDatabase(): RunMateDatabase =
    Room.inMemoryDatabaseBuilder<RunMateDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()

fun plan(
    status: PlanStatus = PlanStatus.PLANNED,
    weather: WeatherSnapshot? = null,
    startTime: Long = 1_000,
    id: Long = 0,
) = RunPlan(
    id = id, startTime = startTime, locationName = "Hồ Tây", latitude = 21.05, longitude = 105.82,
    goalType = GoalType.DISTANCE, goalValue = 5_000, status = status, createdAt = 0, weather = weather
)

fun session(
    planId: Long?,
    status: SessionStatus = SessionStatus.FINISHED,
    startedAt: Long = 1_000,
    distanceMeters: Double = 0.0,
    id: Long = 0,
) = RunSession(
    id = id, planId = planId, status = status, startedAt = startedAt, locationName = "Hồ Tây",
    latitude = 21.05, longitude = 105.82, goalType = GoalType.DISTANCE, goalValue = 5_000,
    distanceMeters = distanceMeters, weather = null
)

fun point(sessionId: Long, timestamp: Long = 0) = TrackPoint(
    sessionId = sessionId, segmentIndex = 0, timestamp = timestamp,
    latitude = 21.05, longitude = 105.82, accuracyM = 5f
)
