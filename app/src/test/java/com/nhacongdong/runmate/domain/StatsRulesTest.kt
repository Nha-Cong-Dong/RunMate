package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.data.plan
import com.nhacongdong.runmate.data.session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

// Unit test cho StatsPeriod và StatsRules (SPEC 5.5, BR-55, BR-56, DESIGN 9 S8).
class StatsRulesTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 6, minute: Int = 0): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    // --- StatsPeriod.Week ---

    @Test
    fun weekPeriod_boundariesAndContains() {
        val week = StatsPeriod.Week(LocalDate.of(2026, 10, 26)) // Thứ Hai
        assertEquals(LocalDate.of(2026, 11, 1), week.endOfWeek) // Chủ Nhật

        // Trong tuần
        assertTrue(week.contains(at(2026, 10, 26, 0, 0), zone))
        assertTrue(week.contains(at(2026, 10, 29, 12, 0), zone))
        assertTrue(week.contains(at(2026, 11, 1, 23, 59), zone))

        // Ngoài tuần
        assertFalse(week.contains(at(2026, 10, 25, 23, 59), zone)) // Chủ Nhật trước
        assertFalse(week.contains(at(2026, 11, 2, 0, 0), zone))    // Thứ Hai sau
    }

    @Test
    fun weekPeriod_previousAndNext() {
        val week = StatsPeriod.Week(LocalDate.of(2026, 10, 26))
        assertEquals(StatsPeriod.Week(LocalDate.of(2026, 10, 19)), week.previous())
        assertEquals(StatsPeriod.Week(LocalDate.of(2026, 11, 2)), week.next())
    }

    @Test
    fun weekPeriod_canGoNext_blocksFuture() {
        val today = LocalDate.of(2026, 10, 28) // Thứ Tư của tuần 26/10
        val currentWeek = StatsPeriod.Week(LocalDate.of(2026, 10, 26))
        val pastWeek = StatsPeriod.Week(LocalDate.of(2026, 10, 19))

        assertTrue(pastWeek.canGoNext(today))
        assertFalse(currentWeek.canGoNext(today))
    }

    // --- StatsPeriod.Month ---

    @Test
    fun monthPeriod_boundariesAndContains() {
        val month = StatsPeriod.Month(YearMonth.of(2026, 10))
        assertEquals(LocalDate.of(2026, 10, 1), month.startOfMonth)
        assertEquals(LocalDate.of(2026, 10, 31), month.endOfMonth)

        assertTrue(month.contains(at(2026, 10, 1, 0, 0), zone))
        assertTrue(month.contains(at(2026, 10, 31, 23, 59), zone))
        assertFalse(month.contains(at(2026, 9, 30, 23, 59), zone))
        assertFalse(month.contains(at(2026, 11, 1, 0, 0), zone))
    }

    @Test
    fun monthPeriod_leapYearFebruary() {
        val feb2024 = StatsPeriod.Month(YearMonth.of(2024, 2))
        assertEquals(LocalDate.of(2024, 2, 29), feb2024.endOfMonth)

        val feb2026 = StatsPeriod.Month(YearMonth.of(2026, 2))
        assertEquals(LocalDate.of(2026, 2, 28), feb2026.endOfMonth)
    }

    @Test
    fun monthPeriod_previousAndNext() {
        val month = StatsPeriod.Month(YearMonth.of(2026, 10))
        assertEquals(StatsPeriod.Month(YearMonth.of(2026, 9)), month.previous())
        assertEquals(StatsPeriod.Month(YearMonth.of(2026, 11)), month.next())
    }

    @Test
    fun monthPeriod_canGoNext_blocksFuture() {
        val today = LocalDate.of(2026, 10, 15)
        val currentMonth = StatsPeriod.Month(YearMonth.of(2026, 10))
        val pastMonth = StatsPeriod.Month(YearMonth.of(2026, 9))

        assertTrue(pastMonth.canGoNext(today))
        assertFalse(currentMonth.canGoNext(today))
    }

    // --- BR-55: Tỷ lệ hoàn thành kế hoạch ---

    @Test
    fun planCompletionRate_standardCases() {
        // 4 hoàn thành, 1 bỏ lỡ -> 80% (SPEC 5.5, DESIGN S8)
        assertEquals(80, StatsRules.planCompletionRate(completed = 4, missed = 1))

        // 3 hoàn thành, 0 bỏ lỡ -> 100%
        assertEquals(100, StatsRules.planCompletionRate(completed = 3, missed = 0))

        // 0 hoàn thành, 2 bỏ lỡ -> 0%
        assertEquals(0, StatsRules.planCompletionRate(completed = 0, missed = 2))

        // Không có kế hoạch nào -> null (hiện "--")
        assertNull(StatsRules.planCompletionRate(completed = 0, missed = 0))
    }

    @Test
    fun planCompletionRate_roundsDown() {
        // 299 / 300 = 99.666% -> làm tròn xuống 99% để 100% luôn nghĩa là trọn vẹn (BR-37, DESIGN 5.3)
        assertEquals(99, StatsRules.planCompletionRate(completed = 299, missed = 1))
    }

    // --- BR-56: Pace trung bình theo phút/km ---

    @Test
    fun avgPaceSecPerKm_calculatesCorrectly() {
        // 5.000 mét (5 km) trong 1.500 giây (25 phút) -> 300.0 giây/km (tương đương 5:00 /km)
        assertEquals(300.0, StatsRules.avgPaceSecPerKm(totalDistanceM = 5_000.0, totalDurationSec = 1_500L)!!, 0.01)

        // Quãng đường <= 0 -> null
        assertNull(StatsRules.avgPaceSecPerKm(totalDistanceM = 0.0, totalDurationSec = 100L))
        assertNull(StatsRules.avgPaceSecPerKm(totalDistanceM = -50.0, totalDurationSec = 100L))
    }

    // --- Tổng hợp PeriodSummary (BR-55, BR-56) ---

    @Test
    fun summarize_filtersAndAggregatesProperly() {
        val week = StatsPeriod.Week(LocalDate.of(2026, 10, 26))

        val plans = listOf(
            plan(status = PlanStatus.COMPLETED, startTime = at(2026, 10, 27)),
            plan(status = PlanStatus.COMPLETED, startTime = at(2026, 10, 28)),
            plan(status = PlanStatus.MISSED, startTime = at(2026, 10, 29)),
            // Bị bỏ qua khi tính tỷ lệ theo BR-55
            plan(status = PlanStatus.PLANNED, startTime = at(2026, 10, 30)),
            plan(status = PlanStatus.CANCELLED, startTime = at(2026, 10, 31)),
            // Ngoài tuần
            plan(status = PlanStatus.COMPLETED, startTime = at(2026, 11, 2)),
        )

        val sessions = listOf(
            session(planId = 1, status = SessionStatus.FINISHED, startedAt = at(2026, 10, 27), distanceMeters = 5_000.0)
                .copy(activeDurationSec = 1_500L),
            session(planId = 2, status = SessionStatus.FINISHED, startedAt = at(2026, 10, 28), distanceMeters = 3_000.0)
                .copy(activeDurationSec = 900L),
            // Buổi chưa hoàn thành -> bỏ qua
            session(planId = null, status = SessionStatus.RECORDING, startedAt = at(2026, 10, 29), distanceMeters = 2_000.0),
            // Ngoài tuần -> bỏ qua
            session(planId = null, status = SessionStatus.FINISHED, startedAt = at(2026, 11, 2), distanceMeters = 4_000.0),
        )

        val summary = StatsRules.summarize(week, plans, sessions, zone)

        assertEquals(2, summary.sessionCount)
        assertEquals(8_000.0, summary.totalDistanceM, 0.01)
        assertEquals(2_400L, summary.totalDurationSec)
        assertEquals(300.0, summary.avgPaceSecPerKm!!, 0.01) // 2.400 / 8 = 300 s/km (5:00 /km)
        assertEquals(2, summary.completedPlans)
        assertEquals(1, summary.missedPlans)
        assertEquals(66, summary.planCompletionRate) // 2/3 = 66.6% -> 66%
    }

    @Test
    fun summarize_emptyPeriod_returnsZeroAndNulls() {
        val week = StatsPeriod.Week(LocalDate.of(2026, 10, 26))
        val summary = StatsRules.summarize(week, emptyList(), emptyList(), zone)

        assertEquals(0, summary.sessionCount)
        assertEquals(0.0, summary.totalDistanceM, 0.01)
        assertEquals(0L, summary.totalDurationSec)
        assertNull(summary.avgPaceSecPerKm)
        assertEquals(0, summary.completedPlans)
        assertEquals(0, summary.missedPlans)
        assertNull(summary.planCompletionRate)
    }

    // --- Bảng ngày theo tuần (dailyDistances) ---

    @Test
    fun dailyDistances_returns7Days_aggregatesDistance() {
        val week = StatsPeriod.Week(LocalDate.of(2026, 10, 26))

        val sessions = listOf(
            // Thứ Ba: 2 buổi chạy
            session(planId = null, status = SessionStatus.FINISHED, startedAt = at(2026, 10, 27, 6, 0), distanceMeters = 5_000.0),
            session(planId = null, status = SessionStatus.FINISHED, startedAt = at(2026, 10, 27, 18, 0), distanceMeters = 3_000.0),
            // Thứ Bảy: 1 buổi chạy
            session(planId = null, status = SessionStatus.FINISHED, startedAt = at(2026, 10, 31, 7, 0), distanceMeters = 10_000.0),
            // Ngoài tuần -> không tính
            session(planId = null, status = SessionStatus.FINISHED, startedAt = at(2026, 11, 2, 6, 0), distanceMeters = 4_000.0),
        )

        val days = StatsRules.dailyDistances(week, sessions, zone)
        assertEquals(7, days.size)

        // Thứ Hai 26/10: không chạy
        assertEquals(LocalDate.of(2026, 10, 26), days[0].date)
        assertEquals(0.0, days[0].distanceMeters, 0.01)

        // Thứ Ba 27/10: 8.000m
        assertEquals(LocalDate.of(2026, 10, 27), days[1].date)
        assertEquals(8_000.0, days[1].distanceMeters, 0.01)

        // Thứ Bảy 31/10: 10.000m
        assertEquals(LocalDate.of(2026, 10, 31), days[5].date)
        assertEquals(10_000.0, days[5].distanceMeters, 0.01)
    }

    // --- Bảng tuần theo tháng (weeklyDistances) ---

    @Test
    fun weeklyDistances_october2026_slicesCorrectly() {
        val month = StatsPeriod.Month(YearMonth.of(2026, 10))

        val sessions = listOf(
            // Tuần 1: 01/10 - 04/10
            session(planId = null, status = SessionStatus.FINISHED, startedAt = at(2026, 10, 2), distanceMeters = 4_000.0),
            // Tuần 2: 05/10 - 11/10
            session(planId = null, status = SessionStatus.FINISHED, startedAt = at(2026, 10, 6), distanceMeters = 5_000.0),
            // Tuần 5: 26/10 - 31/10
            session(planId = null, status = SessionStatus.FINISHED, startedAt = at(2026, 10, 30), distanceMeters = 7_000.0),
        )

        val weeks = StatsRules.weeklyDistances(month, sessions, zone)
        assertEquals(5, weeks.size)

        // Tuần 1: 01/10 (Thứ Năm) -> 04/10 (Chủ Nhật)
        assertEquals(LocalDate.of(2026, 10, 1), weeks[0].startDate)
        assertEquals(LocalDate.of(2026, 10, 4), weeks[0].endDate)
        assertEquals(4_000.0, weeks[0].distanceMeters, 0.01)

        // Tuần 2: 05/10 -> 11/10
        assertEquals(LocalDate.of(2026, 10, 5), weeks[1].startDate)
        assertEquals(LocalDate.of(2026, 10, 11), weeks[1].endDate)
        assertEquals(5_000.0, weeks[1].distanceMeters, 0.01)

        // Tuần 3: 12/10 -> 18/10
        assertEquals(LocalDate.of(2026, 10, 12), weeks[2].startDate)
        assertEquals(LocalDate.of(2026, 10, 18), weeks[2].endDate)
        assertEquals(0.0, weeks[2].distanceMeters, 0.01)

        // Tuần 4: 19/10 -> 25/10
        assertEquals(LocalDate.of(2026, 10, 19), weeks[3].startDate)
        assertEquals(LocalDate.of(2026, 10, 25), weeks[3].endDate)
        assertEquals(0.0, weeks[3].distanceMeters, 0.01)

        // Tuần 5: 26/10 (Thứ Hai) -> 31/10 (Thứ Bảy - cuối tháng)
        assertEquals(LocalDate.of(2026, 10, 26), weeks[4].startDate)
        assertEquals(LocalDate.of(2026, 10, 31), weeks[4].endDate)
        assertEquals(7_000.0, weeks[4].distanceMeters, 0.01)
    }

    // --- Chấm lịch tháng (runningDaysInMonth) ---

    @Test
    fun runningDaysInMonth_returnsUniqueDaysWithFinishedRuns() {
        val month = StatsPeriod.Month(YearMonth.of(2026, 10))

        val sessions = listOf(
            session(planId = null, status = SessionStatus.FINISHED, startedAt = at(2026, 10, 5, 6, 0)),
            session(planId = null, status = SessionStatus.FINISHED, startedAt = at(2026, 10, 5, 17, 0)), // cùng ngày
            session(planId = null, status = SessionStatus.FINISHED, startedAt = at(2026, 10, 12, 6, 0)),
            // Buổi chưa xong -> không chấm
            session(planId = null, status = SessionStatus.RECORDING, startedAt = at(2026, 10, 15, 6, 0)),
            // Ngoài tháng -> không chấm
            session(planId = null, status = SessionStatus.FINISHED, startedAt = at(2026, 11, 1, 6, 0)),
        )

        val days = StatsRules.runningDaysInMonth(month, sessions, zone)
        assertEquals(setOf(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12)), days)
    }
}
