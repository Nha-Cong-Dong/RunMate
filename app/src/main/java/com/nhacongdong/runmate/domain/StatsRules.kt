package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.local.RunSession
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.floor

// Quãng đường của một ngày trong tuần (DESIGN 9 S8)
data class DayDistance(
    val date: LocalDate,
    val distanceMeters: Double,
)

// Quãng đường của một tuần trong tháng (DESIGN 9 S8)
data class WeekDistance(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val distanceMeters: Double,
)

// Quy tắc thống kê ở màn hình S8 (SPEC 5.5, BR-55, BR-56, DESIGN 9 S8). Hàm thuần.
object StatsRules {

    // BR-55: Tỷ lệ giữ lịch / hoàn thành kế hoạch = COMPLETED / (COMPLETED + MISSED).
    // Tính theo giờ bắt đầu của kế hoạch. Không tính PLANNED, IN_PROGRESS, CANCELLED.
    // Kỳ không có kế hoạch nào (completed + missed == 0) thì trả về null (hiển thị "--").
    // Làm tròn xuống (ví dụ: 99.6% -> 99).
    fun planCompletionRate(completed: Int, missed: Int): Int? {
        val total = completed + missed
        if (total == 0) return null
        return floor(completed.toDouble() / total * 100.0).toInt()
    }

    // BR-56: Pace trung bình = tổng thời gian chạy ÷ tổng quãng đường (tính theo giây/km).
    // Nếu tổng quãng đường <= 0 hoặc không có buổi chạy thì trả về null (hiển thị "--").
    fun avgPaceSecPerKm(totalDistanceM: Double, totalDurationSec: Long): Double? {
        if (totalDistanceM <= 0.0) return null
        val km = totalDistanceM / 1_000.0
        return totalDurationSec / km
    }

    // Lọc kế hoạch có startTime nằm trong kỳ (BR-55)
    fun filterPlansInPeriod(plans: List<RunPlan>, period: StatsPeriod, zone: ZoneId): List<RunPlan> =
        plans.filter { period.contains(it.startTime, zone) }

    // Lọc các buổi chạy đã hoàn thành có startedAt nằm trong kỳ (BR-56)
    fun filterSessionsInPeriod(sessions: List<RunSession>, period: StatsPeriod, zone: ZoneId): List<RunSession> =
        sessions.filter { it.status == SessionStatus.FINISHED && period.contains(it.startedAt, zone) }

    // BR-55, BR-56: Tổng hợp số liệu thống kê cho một kỳ
    fun summarize(
        period: StatsPeriod,
        plans: List<RunPlan>,
        sessions: List<RunSession>,
        zone: ZoneId,
    ): PeriodSummary {
        val periodPlans = filterPlansInPeriod(plans, period, zone)
        val completedPlans = periodPlans.count { it.status == PlanStatus.COMPLETED }
        val missedPlans = periodPlans.count { it.status == PlanStatus.MISSED }

        val periodSessions = filterSessionsInPeriod(sessions, period, zone)
        val totalDistanceM = periodSessions.sumOf { it.distanceMeters }
        val totalDurationSec = periodSessions.sumOf { it.activeDurationSec }

        return PeriodSummary(
            sessionCount = periodSessions.size,
            totalDistanceM = totalDistanceM,
            totalDurationSec = totalDurationSec,
            avgPaceSecPerKm = avgPaceSecPerKm(totalDistanceM, totalDurationSec),
            completedPlans = completedPlans,
            missedPlans = missedPlans,
            planCompletionRate = planCompletionRate(completedPlans, missedPlans),
        )
    }

    // Bảng theo ngày cho kỳ Tuần: 7 ngày từ Thứ Hai đến Chủ Nhật (DESIGN 9 S8).
    // Ngày không chạy có distanceMeters = 0.0 (UI hiện "–")
    fun dailyDistances(
        week: StatsPeriod.Week,
        sessions: List<RunSession>,
        zone: ZoneId,
    ): List<DayDistance> {
        val weekSessions = filterSessionsInPeriod(sessions, week, zone)
        val sessionByDate = weekSessions.groupBy {
            Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate()
        }
        return (0L until 7L).map { dayOffset ->
            val date = week.startOfWeek.plusDays(dayOffset)
            val distance = sessionByDate[date]?.sumOf { it.distanceMeters } ?: 0.0
            DayDistance(date, distance)
        }
    }

    // Bảng theo tuần cho kỳ Tháng: mỗi dòng là một khoảng tuần trong tháng (DESIGN 9 S8).
    // Tuần bị cắt bởi đầu/cuối tháng chỉ tính các ngày thuộc tháng (ví dụ 01/10 – 04/10).
    fun weeklyDistances(
        month: StatsPeriod.Month,
        sessions: List<RunSession>,
        zone: ZoneId,
    ): List<WeekDistance> {
        val monthSessions = filterSessionsInPeriod(sessions, month, zone)
        val sessionByDate = monthSessions.groupBy {
            Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate()
        }

        val result = mutableListOf<WeekDistance>()
        var curr = month.startOfMonth
        val endOfMonth = month.endOfMonth

        while (!curr.isAfter(endOfMonth)) {
            val sunday = curr.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            val sliceEnd = if (sunday.isAfter(endOfMonth)) endOfMonth else sunday

            // Tính tổng quãng đường từ curr đến sliceEnd
            var distanceSum = 0.0
            var d = curr
            while (!d.isAfter(sliceEnd)) {
                distanceSum += sessionByDate[d]?.sumOf { it.distanceMeters } ?: 0.0
                d = d.plusDays(1)
            }

            result += WeekDistance(startDate = curr, endDate = sliceEnd, distanceMeters = distanceSum)
            curr = sliceEnd.plusDays(1)
        }

        return result
    }

    // Lịch tháng: tập hợp các ngày trong tháng có ít nhất một buổi chạy FINISHED (DESIGN 9 S8).
    // Dùng để vẽ chấm tròn 6 dp dưới ngày chạy.
    fun runningDaysInMonth(
        month: StatsPeriod.Month,
        sessions: List<RunSession>,
        zone: ZoneId,
    ): Set<LocalDate> {
        val monthSessions = filterSessionsInPeriod(sessions, month, zone)
        return monthSessions
            .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .toSet()
    }
}
