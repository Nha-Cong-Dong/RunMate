package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.local.RunSession
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus

// Trạng thái vòng chạy của một ngày trên lịch tuần (DESIGN 3.2, 13.2)
sealed interface DayRingState {
    data object Empty : DayRingState                       // không có gì hoặc chỉ CANCELLED
    data object Planned : DayRingState                     // vòng nét đứt
    data object Missed : DayRingState                      // vòng xám + "!"
    data class Ran(
        val runCount: Int,                                 // 1 = vòng đầy, >1 = chia đoạn (tối đa 4)
        val allGoalsReached: Boolean,                      // huy hiệu ✓
        val thumbnailSessionId: Long,                      // buổi dài nhất, vẽ trong lõi
    ) : DayRingState
    data object Running : DayRingState                     // vòng xoay
}

// Ưu tiên: Running > Ran > Planned > Missed > Empty
fun dayRingState(plans: List<RunPlan>, sessions: List<RunSession>): DayRingState {
    if (sessions.any { it.status != SessionStatus.FINISHED }) return DayRingState.Running

    val finished = sessions.filter { it.status == SessionStatus.FINISHED }
    if (finished.isNotEmpty()) {
        return DayRingState.Ran(
            runCount = finished.size,
            allGoalsReached = finished.all { SessionResult.isGoalReached(it) },
            thumbnailSessionId = finished.maxBy { it.distanceMeters }.id,
        )
    }
    // IN_PROGRESS mà không có buổi chạy đang diễn ra trong ngày thì coi như chưa chạy
    if (plans.any { it.status == PlanStatus.PLANNED || it.status == PlanStatus.IN_PROGRESS }) {
        return DayRingState.Planned
    }
    if (plans.any { it.status == PlanStatus.MISSED }) return DayRingState.Missed
    return DayRingState.Empty
}

fun dayRingState(day: DayData): DayRingState = dayRingState(day.plans, day.sessions)
