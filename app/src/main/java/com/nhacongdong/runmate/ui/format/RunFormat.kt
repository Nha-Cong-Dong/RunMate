package com.nhacongdong.runmate.ui.format

import com.nhacongdong.runmate.data.model.GoalType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToLong

// Định dạng số và ngày giờ (SPEC 10.5, DESIGN 5.3). Hàm thuần, không cần Context.
// Tên thứ tự ánh xạ để giống nhau trên máy tính (khi test) và trên Android.
object RunFormat {
    private val WEEKDAY_SHORT = mapOf(
        DayOfWeek.MONDAY to "T2", DayOfWeek.TUESDAY to "T3", DayOfWeek.WEDNESDAY to "T4",
        DayOfWeek.THURSDAY to "T5", DayOfWeek.FRIDAY to "T6", DayOfWeek.SATURDAY to "T7",
        DayOfWeek.SUNDAY to "CN",
    )
    private val WEEKDAY_LONG = mapOf(
        DayOfWeek.MONDAY to "Thứ Hai", DayOfWeek.TUESDAY to "Thứ Ba", DayOfWeek.WEDNESDAY to "Thứ Tư",
        DayOfWeek.THURSDAY to "Thứ Năm", DayOfWeek.FRIDAY to "Thứ Sáu", DayOfWeek.SATURDAY to "Thứ Bảy",
        DayOfWeek.SUNDAY to "Chủ Nhật",
    )

    // Chỉ trả về số, chữ "km" vẽ riêng (DESIGN 5.2). Dưới 10 km: 2 số thập phân, từ 10 km: 1
    fun formatDistanceKm(meters: Double): String {
        val hundredths = (meters / 10).roundToLong()        // số phần trăm km
        return if (hundredths < 1_000) decimal(hundredths, 100, 2) else decimal((meters / 100).roundToLong(), 10, 1)
    }

    // m:ss dưới 1 giờ, h:mm:ss từ 1 giờ
    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = seconds % 3600 / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    // Làm tròn xuống để "100%" luôn đi kèm đạt mục tiêu (BR-37)
    fun formatPercent(ratio: Double): String = "${floor(ratio * 100 + 1e-9).toInt()}%"

    fun formatTime(millis: Long, zone: ZoneId): String {
        val time = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
        return "%02d:%02d".format(time.hour, time.minute)
    }

    fun formatWeekday(date: LocalDate): String = WEEKDAY_SHORT.getValue(date.dayOfWeek)

    // "Thứ Năm, 29/10"
    fun formatDayTitle(date: LocalDate): String =
        "${WEEKDAY_LONG.getValue(date.dayOfWeek)}, %02d/%02d".format(date.dayOfMonth, date.monthValue)

    // "Tháng 10, 2026" · "Tháng 10 – 11, 2026" · "Tháng 12, 2026 – Tháng 1, 2027" (DESIGN 8.3)
    fun formatMonthHeader(weekStart: LocalDate): String {
        val end = weekStart.plusDays(6)
        return when {
            weekStart.month == end.month -> "Tháng ${end.monthValue}, ${end.year}"
            weekStart.year == end.year -> "Tháng ${weekStart.monthValue} – ${end.monthValue}, ${end.year}"
            else -> "Tháng ${weekStart.monthValue}, ${weekStart.year} – Tháng ${end.monthValue}, ${end.year}"
        }
    }

    // "5 km", "5,5 km", "45 phút", "1 giờ 30 phút", "2 giờ"
    fun formatGoal(goalType: GoalType, goalValue: Int): String = when (goalType) {
        GoalType.DISTANCE -> {
            val km = goalValue / 1000
            val tenths = goalValue % 1000 / 100
            if (tenths == 0) "$km km" else "$km,$tenths km"
        }
        GoalType.DURATION -> hoursMinutes(goalValue / 60)
    }

    // Thời lượng trên ResultCard, bỏ giây: "26 phút", "1 giờ 4 phút", "2 giờ" (DESIGN 5.3)
    fun formatDurationWords(seconds: Long): String =
        if (seconds < 60) "Dưới 1 phút" else hoursMinutes((seconds / 60).toInt())

    // Tóm tắt ở tiêu đề DayList: "2 buổi · 15,4 km" (DESIGN 8.7)
    fun formatDaySummary(runCount: Int, meters: Double): String =
        "$runCount buổi · ${formatDistanceKm(meters)} km"

    // "45 phút", "1 giờ 30 phút", "2 giờ"
    private fun hoursMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "$m phút"
            m == 0 -> "$h giờ"
            else -> "$h giờ $m phút"
        }
    }

    // value / scale với đúng `digits` chữ số thập phân, dấu phẩy
    private fun decimal(value: Long, scale: Int, digits: Int): String =
        "%d,%0${digits}d".format(Locale.ROOT, value / scale, value % scale)
}
