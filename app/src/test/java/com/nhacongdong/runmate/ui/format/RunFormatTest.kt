package com.nhacongdong.runmate.ui.format

import com.nhacongdong.runmate.data.model.GoalType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

// Test định dạng theo DESIGN 5.3.
class RunFormatTest {
    @Test
    fun distance_twoDecimalsBelow10Km_oneFrom10Km() {
        assertEquals("5,24", RunFormat.formatDistanceKm(5_240.0))
        assertEquals("0,05", RunFormat.formatDistanceKm(50.0))
        assertEquals("12,4", RunFormat.formatDistanceKm(12_400.0))
        assertEquals("10,0", RunFormat.formatDistanceKm(9_996.0))   // làm tròn lên 10 km thì đổi sang 1 số thập phân
    }

    @Test
    fun duration_minutesOrHours() {
        assertEquals("26:55", RunFormat.formatDuration(1_615))
        assertEquals("0:09", RunFormat.formatDuration(9))
        assertEquals("1:04:10", RunFormat.formatDuration(3_850))
    }

    @Test
    fun percent_roundsDown() {
        assertEquals("104%", RunFormat.formatPercent(1.048))
        assertEquals("99%", RunFormat.formatPercent(0.996))
        assertEquals("100%", RunFormat.formatPercent(1.0))
        assertEquals("29%", RunFormat.formatPercent(0.29))   // 0.29 * 100 = 28.999… trong máy tính
    }

    @Test
    fun time_inDeviceZone() {
        val zone = ZoneId.of("Asia/Ho_Chi_Minh")
        val millis = LocalDateTime.of(2026, 10, 29, 6, 5).atZone(zone).toInstant().toEpochMilli()
        assertEquals("06:05", RunFormat.formatTime(millis, zone))
    }

    @Test
    fun weekdayAndDayTitle() {
        assertEquals("T5", RunFormat.formatWeekday(LocalDate.of(2026, 10, 29)))
        assertEquals("CN", RunFormat.formatWeekday(LocalDate.of(2026, 11, 1)))
        assertEquals("Thứ Năm, 29/10", RunFormat.formatDayTitle(LocalDate.of(2026, 10, 29)))
        assertEquals("Chủ Nhật, 01/11", RunFormat.formatDayTitle(LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun monthHeader_sameMonth_crossMonth_crossYear() {
        assertEquals("Tháng 10, 2026", RunFormat.formatMonthHeader(LocalDate.of(2026, 10, 19)))
        assertEquals("Tháng 10 – 11, 2026", RunFormat.formatMonthHeader(LocalDate.of(2026, 10, 26)))
        assertEquals("Tháng 12, 2026 – Tháng 1, 2027", RunFormat.formatMonthHeader(LocalDate.of(2026, 12, 28)))
    }

    @Test
    fun goal_distanceAndDuration() {
        assertEquals("5 km", RunFormat.formatGoal(GoalType.DISTANCE, 5_000))
        assertEquals("5,5 km", RunFormat.formatGoal(GoalType.DISTANCE, 5_500))
        assertEquals("45 phút", RunFormat.formatGoal(GoalType.DURATION, 2_700))
        assertEquals("2 giờ", RunFormat.formatGoal(GoalType.DURATION, 7_200))
        assertEquals("1 giờ 30 phút", RunFormat.formatGoal(GoalType.DURATION, 5_400))
    }
}
