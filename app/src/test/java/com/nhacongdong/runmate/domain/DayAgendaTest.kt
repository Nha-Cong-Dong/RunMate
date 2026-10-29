package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.data.plan
import com.nhacongdong.runmate.data.session
import com.nhacongdong.runmate.domain.DayEntry.PlanEntry
import com.nhacongdong.runmate.domain.DayEntry.ResultEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

// Test gom kế hoạch và buổi chạy theo ngày (BR-50, DESIGN 8.7).
class DayAgendaTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun groupByDay_usesDeviceZone() {
        // 06:00 ngày 30/10 giờ Việt Nam là 23:00 ngày 29/10 giờ UTC
        val days = DayAgenda.groupByDay(listOf(plan(startTime = at(30, 6))), emptyList(), zone)
        assertEquals(setOf(LocalDate.of(2026, 10, 30)), days.keys)
    }

    @Test
    fun completedPlan_movesToDayOfItsRun() {
        // Kế hoạch 23:50 ngày 29/10, chạy muộn lúc 00:10 ngày 30/10: cả hai nằm ở ngày 30/10
        val p = plan(PlanStatus.COMPLETED, startTime = at(29, 23, 50), id = 7)
        val s = session(planId = 7, startedAt = at(30, 0, 10), id = 1)

        val days = DayAgenda.groupByDay(listOf(p), listOf(s), zone)

        assertEquals(setOf(LocalDate.of(2026, 10, 30)), days.keys)
        assertEquals(DayData(listOf(p), listOf(s)), days.getValue(LocalDate.of(2026, 10, 30)))
    }

    @Test
    fun notCompletedPlan_staysOnItsOwnDay() {
        val p = plan(PlanStatus.MISSED, startTime = at(29, 6), id = 7)

        assertEquals(setOf(LocalDate.of(2026, 10, 29)), DayAgenda.groupByDay(listOf(p), emptyList(), zone).keys)
    }

    @Test
    fun completedPlanAndItsSession_areOneResultEntry() {
        val p = plan(PlanStatus.COMPLETED, startTime = at(29, 6), id = 7)
        val s = session(planId = 7, startedAt = at(29, 6, 5), id = 1)

        assertEquals(listOf(ResultEntry(p, s)), DayAgenda.dayEntries(listOf(p), listOf(s)))
    }

    @Test
    fun freeRun_isResultEntryWithoutPlan() {
        val s = session(planId = null, startedAt = at(29, 17), id = 1)

        assertEquals(listOf(ResultEntry(null, s)), DayAgenda.dayEntries(emptyList(), listOf(s)))
    }

    @Test
    fun entries_mixedTypes_sortedByTime() {
        val evening = plan(PlanStatus.PLANNED, startTime = at(29, 19), id = 2)
        val morning = plan(PlanStatus.COMPLETED, startTime = at(29, 6), id = 1)
        val morningRun = session(planId = 1, startedAt = at(29, 6, 2), id = 10)
        val freeRun = session(planId = null, startedAt = at(29, 17, 30), id = 11)

        val entries = DayAgenda.dayEntries(listOf(evening, morning), listOf(freeRun, morningRun))

        assertEquals(
            listOf(ResultEntry(morning, morningRun), ResultEntry(null, freeRun), PlanEntry(evening)),
            entries
        )
    }

    @Test
    fun unfinishedSession_isNotAnEntry() {
        val p = plan(PlanStatus.IN_PROGRESS, startTime = at(29, 6), id = 7)
        val running = session(planId = 7, status = SessionStatus.RECORDING, startedAt = at(29, 6), id = 1)

        assertEquals(listOf(PlanEntry(p)), DayAgenda.dayEntries(listOf(p), listOf(running)))
    }
}
