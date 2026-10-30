package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.domain.model.PlanError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

// Test quy tắc kế hoạch, tập trung vào giá trị biên (SPEC 5.1, 6.3, 6.4).
class PlanRulesTest {
    private val minute = 60_000L
    private val day = 24 * 60 * minute
    private val now = 1_000 * day + 6 * 60 * minute   // một ngày nào đó, 06:00:00
    private val start = now                            // giờ bắt đầu kế hoạch dùng cho BR-09

    private fun distance(meters: Int, startTime: Long = now + day) =
        PlanRules.validate(startTime, GoalType.DISTANCE, meters, now)

    private fun duration(seconds: Int) =
        PlanRules.validate(now + day, GoalType.DURATION, seconds, now)

    // BR-01
    @Test
    fun startEqualsNow_isValid() = assertEquals(emptyList<PlanError>(), distance(5_000, startTime = now))

    @Test
    fun startInSameMinuteButFewSecondsAgo_isValid() {
        // chọn 06:00, bấm Lưu lúc 06:00:20
        val errors = PlanRules.validate(now, GoalType.DISTANCE, 5_000, now + 20_000)
        assertEquals(emptyList<PlanError>(), errors)
    }

    @Test
    fun startOneMinuteAgo_isInPast() =
        assertEquals(listOf(PlanError.START_IN_PAST), distance(5_000, startTime = now - minute))

    // BR-02
    @Test
    fun startExactly16DaysAhead_isValid() =
        assertEquals(emptyList<PlanError>(), distance(5_000, startTime = now + 16 * day))

    @Test
    fun startMoreThan16DaysAhead_isTooFar() =
        assertEquals(listOf(PlanError.START_TOO_FAR), distance(5_000, startTime = now + 16 * day + minute))

    // BR-04
    @Test
    fun distanceBoundaries_areValid() {
        assertEquals(emptyList<PlanError>(), distance(500))
        assertEquals(emptyList<PlanError>(), distance(100_000))
    }

    @Test
    fun distanceOutOfRangeOrWrongStep_isError() {
        assertEquals(listOf(PlanError.DISTANCE_OUT_OF_RANGE), distance(400))
        assertEquals(listOf(PlanError.DISTANCE_OUT_OF_RANGE), distance(100_100))
        assertEquals(listOf(PlanError.DISTANCE_OUT_OF_RANGE), distance(1_050))   // không chia hết 0,1 km
    }

    // BR-05
    @Test
    fun durationBoundaries_areValid() {
        assertEquals(emptyList<PlanError>(), duration(300))
        assertEquals(emptyList<PlanError>(), duration(43_200))
    }

    @Test
    fun durationOutOfRangeOrWrongStep_isError() {
        assertEquals(listOf(PlanError.DURATION_OUT_OF_RANGE), duration(240))
        assertEquals(listOf(PlanError.DURATION_OUT_OF_RANGE), duration(43_260))
        assertEquals(listOf(PlanError.DURATION_OUT_OF_RANGE), duration(330))     // không chia hết 1 phút
    }

    @Test
    fun severalProblems_returnsAllErrors() =
        assertEquals(
            listOf(PlanError.START_IN_PAST, PlanError.DISTANCE_OUT_OF_RANGE),
            distance(400, startTime = now - day)
        )

    // BR-09
    @Test
    fun exactlyAtGraceEnd_isNotOverdue() = assertFalse(PlanRules.isOverdue(start, start + 30 * minute))

    @Test
    fun oneMillisecondAfterGrace_isOverdue() = assertTrue(PlanRules.isOverdue(start, start + 30 * minute + 1))

    // SPEC 6.3
    @Test
    fun refreshStatus_plannedOverdue_becomesMissed() =
        assertEquals(PlanStatus.MISSED, PlanRules.refreshStatus(PlanStatus.PLANNED, start, start + day))

    @Test
    fun refreshStatus_plannedWithinGrace_staysPlanned() =
        assertEquals(PlanStatus.PLANNED, PlanRules.refreshStatus(PlanStatus.PLANNED, start, start + 10 * minute))

    @Test
    fun refreshStatus_otherStatuses_unchanged() {
        for (status in listOf(PlanStatus.IN_PROGRESS, PlanStatus.COMPLETED, PlanStatus.CANCELLED, PlanStatus.MISSED)) {
            assertEquals(status, PlanRules.refreshStatus(status, start, start + day))
        }
    }

    @Test
    fun statusAfterSessionRemoved_beforeAndAfterGrace() {
        assertEquals(PlanStatus.PLANNED, PlanRules.statusAfterSessionRemoved(start, start + 30 * minute))
        assertEquals(PlanStatus.MISSED, PlanRules.statusAfterSessionRemoved(start, start + 30 * minute + 1))
    }

    // SPEC 6.4
    @Test
    fun canEdit_onlyPlannedMissedCancelled() {
        assertTrue(PlanRules.canEdit(PlanStatus.PLANNED))
        assertTrue(PlanRules.canEdit(PlanStatus.MISSED))
        assertTrue(PlanRules.canEdit(PlanStatus.CANCELLED))
        assertFalse(PlanRules.canEdit(PlanStatus.IN_PROGRESS))
        assertFalse(PlanRules.canEdit(PlanStatus.COMPLETED))
    }

    @Test
    fun canDelete_allExceptInProgress() {
        for (status in PlanStatus.entries) {
            assertEquals(status != PlanStatus.IN_PROGRESS, PlanRules.canDelete(status))
        }
    }

    @Test
    fun canCancelAndCanStart_onlyPlannedOrMissed() {
        for (status in PlanStatus.entries) {
            val expected = status == PlanStatus.PLANNED || status == PlanStatus.MISSED
            assertEquals(expected, PlanRules.canCancel(status))
            assertEquals(expected, PlanRules.canStart(status))
        }
    }

    // SPEC 6.4: sửa sang giờ tương lai
    @Test
    fun statusAfterEdit_missedAndCancelledBecomePlanned() {
        assertEquals(PlanStatus.PLANNED, PlanRules.statusAfterEdit(PlanStatus.MISSED))
        assertEquals(PlanStatus.PLANNED, PlanRules.statusAfterEdit(PlanStatus.CANCELLED))
    }

    @Test
    fun statusAfterEdit_plannedStaysPlanned() =
        assertEquals(PlanStatus.PLANNED, PlanRules.statusAfterEdit(PlanStatus.PLANNED))

    // S2, PLAN B4. 29/10/2026 là Thứ Năm
    private val editNow = LocalDateTime.of(2026, 10, 29, 8, 20)

    @Test
    fun defaultStart_noSelectedDay_usesNextFullHourToday() =
        assertEquals(LocalDateTime.of(2026, 10, 29, 9, 0), PlanRules.defaultStart(null, editNow))

    @Test
    fun defaultStart_selectedToday_usesNextFullHour() =
        assertEquals(LocalDateTime.of(2026, 10, 29, 9, 0), PlanRules.defaultStart(LocalDate.of(2026, 10, 29), editNow))

    @Test
    fun defaultStart_selectedPastDay_fallsBackToToday() =
        assertEquals(LocalDateTime.of(2026, 10, 29, 9, 0), PlanRules.defaultStart(LocalDate.of(2026, 10, 27), editNow))

    @Test
    fun defaultStart_selectedFutureDay_usesEarlyMorning() =
        assertEquals(LocalDateTime.of(2026, 11, 2, 6, 0), PlanRules.defaultStart(LocalDate.of(2026, 11, 2), editNow))

    @Test
    fun defaultStart_lateEvening_rollsOverToNextDay() =
        assertEquals(
            LocalDateTime.of(2026, 10, 30, 0, 0),
            PlanRules.defaultStart(null, LocalDateTime.of(2026, 10, 29, 23, 30)),
        )

    // DESIGN 8.13: bánh xe tự chỉnh về khoảng hợp lệ
    @Test
    fun clampGoal_inRangeUnchanged() {
        assertEquals(5_000, PlanRules.clampGoal(GoalType.DISTANCE, 5_000))
        assertEquals(1_800, PlanRules.clampGoal(GoalType.DURATION, 1_800))
    }

    @Test
    fun clampGoal_distanceBelowMinimum_becomesHalfKm() = assertEquals(500, PlanRules.clampGoal(GoalType.DISTANCE, 300))

    @Test
    fun clampGoal_distanceAboveMaximum_becomes100Km() =
        assertEquals(100_000, PlanRules.clampGoal(GoalType.DISTANCE, 100_500))

    @Test
    fun clampGoal_durationBelowMinimum_becomes5Minutes() = assertEquals(300, PlanRules.clampGoal(GoalType.DURATION, 180))

    @Test
    fun clampGoal_durationAboveMaximum_becomes12Hours() = assertEquals(43_200, PlanRules.clampGoal(GoalType.DURATION, 43_800))
}
