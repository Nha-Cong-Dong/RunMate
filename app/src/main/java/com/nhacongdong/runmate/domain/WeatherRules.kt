package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.PlanStatus
import java.time.Instant
import java.time.ZoneId

// Cảnh báo thời tiết (SPEC 5.2, BR-10 đến BR-13). Chỉ thông báo, không chặn lưu hay chạy
enum class WeatherWarning { RAIN, HOT, WINDY, UV }

// Gợi ý vật dụng (SPEC 5.2, BR-20 đến BR-24). Các mục trùng nhau chỉ xuất hiện một lần
enum class Gear { RAIN_COAT, HAT, SUNSCREEN, WATER, WINDBREAKER }

// Quy tắc thời tiết. Hàm thuần: số liệu truyền vào, không đọc mạng hay đồng hồ.
// Mọi ngưỡng là ≥, nên đúng bằng ngưỡng vẫn bị cảnh báo
object WeatherRules {
    const val RAIN_PERCENT = 50        // BR-10, BR-20
    const val HOT_CELSIUS = 35.0       // BR-11, BR-22
    const val WIND_KMH = 30.0          // BR-12, BR-23
    const val UV_INDEX = 8.0           // BR-13, BR-21
    const val LONG_DISTANCE_M = 10_000 // BR-24: 10 km
    const val LONG_DURATION_S = 3_600  // BR-24: 60 phút
    const val REFRESH_AFTER_MS = 3 * 60 * 60_000L  // SPEC 5.2: dữ liệu đã lưu quá 3 giờ thì lấy lại

    // SPEC 5.2: dự báo lấy theo giờ chẵn, làm tròn xuống. 06:30 lấy dữ liệu 06:00, 07:00 cũng lấy 06:00.
    // Trả về epoch millis của giờ chẵn đó
    fun forecastHour(startTime: Long, zone: ZoneId): Long {
        val time = Instant.ofEpochMilli(startTime).atZone(zone)
        return time.withMinute(0).withSecond(0).withNano(0).withHour(time.hour - time.hour % 2).toInstant().toEpochMilli()
    }

    // SPEC 5.2: dữ liệu đã lưu hơn 3 giờ thì coi là cũ
    fun isStale(fetchedAt: Long, now: Long): Boolean = now - fetchedAt > REFRESH_AFTER_MS

    // S4 (SPEC 5.2): hiện nút Lấy lại khi chưa có dữ liệu hoặc dữ liệu đã cũ
    fun showRefreshButton(fetchedAt: Long?, now: Long): Boolean = fetchedAt == null || isStale(fetchedAt, now)

    // SPEC 5.2: chỉ kế hoạch chưa qua giờ hoặc đang chạy mới lấy lại thời tiết.
    // COMPLETED, CANCELLED và MISSED chỉ hiện dữ liệu đã lưu
    fun canRefresh(status: PlanStatus): Boolean = status == PlanStatus.PLANNED || status == PlanStatus.IN_PROGRESS

    fun warnings(rainPercent: Int, temperature: Double, windKmh: Double, uvIndex: Double): List<WeatherWarning> =
        buildList {
            if (rainPercent >= RAIN_PERCENT) add(WeatherWarning.RAIN)
            if (temperature >= HOT_CELSIUS) add(WeatherWarning.HOT)
            if (windKmh >= WIND_KMH) add(WeatherWarning.WINDY)
            if (uvIndex >= UV_INDEX) add(WeatherWarning.UV)
        }

    fun gear(
        rainPercent: Int,
        temperature: Double,
        windKmh: Double,
        uvIndex: Double,
        goalType: GoalType,
        goalValue: Int,
    ): Set<Gear> = buildSet {
        if (rainPercent >= RAIN_PERCENT) add(Gear.RAIN_COAT)
        if (uvIndex >= UV_INDEX) {
            add(Gear.HAT)
            add(Gear.SUNSCREEN)
        }
        if (temperature >= HOT_CELSIUS) {
            add(Gear.HAT)
            add(Gear.WATER)
        }
        if (windKmh >= WIND_KMH) add(Gear.WINDBREAKER)
        val longGoal = when (goalType) {
            GoalType.DISTANCE -> goalValue >= LONG_DISTANCE_M
            GoalType.DURATION -> goalValue >= LONG_DURATION_S
        }
        if (longGoal) add(Gear.WATER)
    }
}
