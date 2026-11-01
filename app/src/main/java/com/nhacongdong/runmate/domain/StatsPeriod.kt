package com.nhacongdong.runmate.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

// Kỳ thống kê ở màn hình S8 (SPEC 9.1, DESIGN 8.18, BR-56).
// Tuần từ Thứ Hai đến Chủ Nhật. Tháng theo dương lịch.
sealed interface StatsPeriod {
    fun startEpochMs(zone: ZoneId): Long
    fun endEpochMs(zone: ZoneId): Long // Thời điểm cuối kỳ (inclusive, đến mili-giây cuối)
    fun previous(): StatsPeriod
    fun next(): StatsPeriod
    fun canGoNext(today: LocalDate): Boolean
    fun contains(epochMs: Long, zone: ZoneId): Boolean

    // Kỳ thống kê theo Tuần (Thứ Hai -> Chủ Nhật)
    data class Week(val startOfWeek: LocalDate) : StatsPeriod {
        init {
            require(startOfWeek == WeekRules.weekStartOf(startOfWeek)) {
                "startOfWeek phải là Thứ Hai: $startOfWeek"
            }
        }

        val endOfWeek: LocalDate get() = startOfWeek.plusDays(6)

        override fun startEpochMs(zone: ZoneId): Long =
            startOfWeek.atStartOfDay(zone).toInstant().toEpochMilli()

        override fun endEpochMs(zone: ZoneId): Long =
            endOfWeek.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        override fun previous(): Week = Week(startOfWeek.minusWeeks(1))

        override fun next(): Week = Week(startOfWeek.plusWeeks(1))

        // Không cho tiến vượt quá tuần hiện tại
        override fun canGoNext(today: LocalDate): Boolean =
            startOfWeek.isBefore(WeekRules.weekStartOf(today))

        override fun contains(epochMs: Long, zone: ZoneId): Boolean {
            val date = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
            return !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
        }
    }

    // Kỳ thống kê theo Tháng dương lịch
    data class Month(val yearMonth: YearMonth) : StatsPeriod {
        val startOfMonth: LocalDate get() = yearMonth.atDay(1)
        val endOfMonth: LocalDate get() = yearMonth.atEndOfMonth()

        override fun startEpochMs(zone: ZoneId): Long =
            startOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()

        override fun endEpochMs(zone: ZoneId): Long =
            endOfMonth.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        override fun previous(): Month = Month(yearMonth.minusMonths(1))

        override fun next(): Month = Month(yearMonth.plusMonths(1))

        // Không cho tiến vượt quá tháng hiện tại
        override fun canGoNext(today: LocalDate): Boolean =
            yearMonth.isBefore(YearMonth.from(today))

        override fun contains(epochMs: Long, zone: ZoneId): Boolean {
            val date = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
            return YearMonth.from(date) == yearMonth
        }
    }

    companion object {
        fun currentWeek(today: LocalDate): Week = Week(WeekRules.weekStartOf(today))
        fun currentMonth(today: LocalDate): Month = Month(YearMonth.from(today))
    }
}
