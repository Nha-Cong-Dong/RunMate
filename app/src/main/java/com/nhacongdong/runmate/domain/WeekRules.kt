package com.nhacongdong.runmate.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// Lịch tuần ở S1 (SPEC 9.1, DESIGN 8.3). Tuần bắt đầu từ Thứ Hai.
object WeekRules {
    fun weekStartOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekDays(weekStart: LocalDate): List<LocalDate> = (0L until 7L).map { weekStart.plusDays(it) }

    // BR-02: nút › tắt từ tuần chứa hôm nay + 16 ngày
    fun lastAllowedWeekStart(today: LocalDate): LocalDate =
        weekStartOf(today.plusDays(PlanRules.MAX_DAYS_AHEAD.toLong()))

    // Ngày còn lên kế hoạch được: hôm nay đến hôm nay + 16 ngày (DESIGN 8.7)
    fun isPlannable(date: LocalDate, today: LocalDate): Boolean =
        !date.isBefore(today) && !date.isAfter(today.plusDays(PlanRules.MAX_DAYS_AHEAD.toLong()))
}
