package com.nhacongdong.runmate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// Test lịch tuần (DESIGN 8.3, BR-02). 29/10/2026 là Thứ Năm.
class WeekRulesTest {
    private val today = LocalDate.of(2026, 10, 29)
    private val monday = LocalDate.of(2026, 10, 26)

    @Test
    fun weekStartOf_monday_isItself() = assertEquals(monday, WeekRules.weekStartOf(monday))

    @Test
    fun weekStartOf_midWeekAndSunday_goBackToMonday() {
        assertEquals(monday, WeekRules.weekStartOf(today))
        assertEquals(monday, WeekRules.weekStartOf(LocalDate.of(2026, 11, 1)))   // Chủ nhật
    }

    @Test
    fun weekDays_crossesMonth() {
        val days = WeekRules.weekDays(monday)
        assertEquals(7, days.size)
        assertEquals(monday, days.first())
        assertEquals(LocalDate.of(2026, 11, 1), days.last())
    }

    @Test
    fun lastAllowedWeekStart_isWeekOfTodayPlus16Days() {
        // 29/10 + 16 ngày = 14/11 (Thứ Bảy), tuần đó bắt đầu 09/11
        assertEquals(LocalDate.of(2026, 11, 9), WeekRules.lastAllowedWeekStart(today))
    }

    @Test
    fun isPlannable_fromTodayTo16DaysAhead() {
        assertTrue(WeekRules.isPlannable(today, today))
        assertTrue(WeekRules.isPlannable(today.plusDays(16), today))
        assertFalse(WeekRules.isPlannable(today.minusDays(1), today))
        assertFalse(WeekRules.isPlannable(today.plusDays(17), today))
    }
}
