package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.local.RunSession
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Kế hoạch và buổi chạy của một ngày trên lịch tuần
data class DayData(
    val plans: List<RunPlan> = emptyList(),
    val sessions: List<RunSession> = emptyList(),
)

// Một dòng trong DayList (DESIGN 8.5, 8.6, 8.7)
sealed interface DayEntry {
    val time: Long

    // PlanCard: kế hoạch chưa chạy
    data class PlanEntry(val plan: RunPlan) : DayEntry {
        override val time: Long get() = plan.startTime
    }

    // ResultCard: kế hoạch COMPLETED kèm buổi chạy của nó, hoặc chạy tự do (plan = null).
    // Xếp theo giờ bắt đầu chạy: dữ liệu chạy quan trọng hơn giờ dự kiến
    data class ResultEntry(val plan: RunPlan?, val session: RunSession) : DayEntry {
        override val time: Long get() = session.startedAt
    }
}

object DayAgenda {
    // Gom theo ngày, tính theo múi giờ của máy (BR-50).
    // Buổi chạy luôn nằm ở ngày bắt đầu chạy. Kế hoạch COMPLETED đi theo buổi chạy của nó,
    // kể cả khi chạy muộn sang ngày sau, để kế hoạch và kết quả luôn là một thẻ.
    fun groupByDay(plans: List<RunPlan>, sessions: List<RunSession>, zone: ZoneId): Map<LocalDate, DayData> {
        val runByPlanId = sessions
            .filter { it.status == SessionStatus.FINISHED && it.planId != null }
            .associateBy { it.planId }
        val planDays = plans.groupBy { plan ->
            val run = runByPlanId[plan.id]
            if (plan.status == PlanStatus.COMPLETED && run != null) dateOf(run.startedAt, zone)
            else dateOf(plan.startTime, zone)
        }
        val sessionDays = sessions.groupBy { dateOf(it.startedAt, zone) }
        return (planDays.keys + sessionDays.keys).associateWith {
            DayData(planDays[it].orEmpty(), sessionDays[it].orEmpty())
        }
    }

    // Kế hoạch COMPLETED và buổi chạy của nó là MỘT ResultEntry. Sắp theo giờ tăng dần (BR-50)
    fun dayEntries(plans: List<RunPlan>, sessions: List<RunSession>): List<DayEntry> {
        val finished = sessions.filter { it.status == SessionStatus.FINISHED }
        val finishedByPlanId = finished.filter { it.planId != null }.associateBy { it.planId }
        val usedSessions = mutableSetOf<RunSession>()

        val planEntries = plans.map { plan ->
            val session = finishedByPlanId[plan.id]
            if (plan.status == PlanStatus.COMPLETED && session != null) {
                usedSessions += session
                DayEntry.ResultEntry(plan, session)
            } else {
                DayEntry.PlanEntry(plan)
            }
        }
        // Buổi chạy không gắn với kế hoạch nào trong ngày thì hiện như chạy tự do, để không bao giờ bị ẩn
        val freeRuns = (finished - usedSessions).map { DayEntry.ResultEntry(null, it) }
        return (planEntries + freeRuns).sortedBy { it.time }
    }

    fun dayEntries(day: DayData): List<DayEntry> = dayEntries(day.plans, day.sessions)

    fun dateOf(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
