package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.PlanStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

// Test cảnh báo và gợi ý vật dụng (SPEC 5.2, BR-10 đến BR-13, BR-20 đến BR-24).
// Mọi ngưỡng là ≥, nên đúng bằng ngưỡng vẫn bị cảnh báo
class WeatherRulesTest {
    // Thời tiết dễ chịu: không cảnh báo, không gợi ý gì
    private val calmRain = 0
    private val calmTemp = 25.0
    private val calmWind = 5.0
    private val calmUv = 2.0

    private fun warnings(
        rain: Int = calmRain,
        temp: Double = calmTemp,
        wind: Double = calmWind,
        uv: Double = calmUv,
    ) = WeatherRules.warnings(rain, temp, wind, uv)

    private fun gear(
        rain: Int = calmRain,
        temp: Double = calmTemp,
        wind: Double = calmWind,
        uv: Double = calmUv,
        goalType: GoalType = GoalType.DISTANCE,
        goalValue: Int = 5_000,
    ) = WeatherRules.gear(rain, temp, wind, uv, goalType, goalValue)

    // BR-10 đến BR-13
    @Test
    fun calmWeather_noWarnings() = assertEquals(emptyList<WeatherWarning>(), warnings())

    @Test
    fun rainAtThreshold_warns() = assertEquals(listOf(WeatherWarning.RAIN), warnings(rain = 50))

    @Test
    fun rainJustBelowThreshold_noWarning() = assertEquals(emptyList<WeatherWarning>(), warnings(rain = 49))

    @Test
    fun temperatureAtThreshold_warns() = assertEquals(listOf(WeatherWarning.HOT), warnings(temp = 35.0))

    @Test
    fun temperatureJustBelowThreshold_noWarning() = assertEquals(emptyList<WeatherWarning>(), warnings(temp = 34.9))

    @Test
    fun windAtThreshold_warns() = assertEquals(listOf(WeatherWarning.WINDY), warnings(wind = 30.0))

    @Test
    fun windJustBelowThreshold_noWarning() = assertEquals(emptyList<WeatherWarning>(), warnings(wind = 29.9))

    @Test
    fun uvAtThreshold_warns() = assertEquals(listOf(WeatherWarning.UV), warnings(uv = 8.0))

    @Test
    fun uvJustBelowThreshold_noWarning() = assertEquals(emptyList<WeatherWarning>(), warnings(uv = 7.9))

    @Test
    fun allWarnings_returnedInOrder() =
        assertEquals(
            listOf(WeatherWarning.RAIN, WeatherWarning.HOT, WeatherWarning.WINDY, WeatherWarning.UV),
            warnings(rain = 80, temp = 38.0, wind = 40.0, uv = 9.0),
        )

    // BR-20 đến BR-24
    @Test
    fun calmWeather_noGear() = assertEquals(emptySet<Gear>(), gear())

    @Test
    fun rain_suggestsRainCoat() = assertEquals(setOf(Gear.RAIN_COAT), gear(rain = 50))

    @Test
    fun uv_suggestsHatAndSunscreen() = assertEquals(setOf(Gear.HAT, Gear.SUNSCREEN), gear(uv = 8.0))

    @Test
    fun hot_suggestsHatAndWater() = assertEquals(setOf(Gear.HAT, Gear.WATER), gear(temp = 35.0))

    @Test
    fun wind_suggestsWindbreaker() = assertEquals(setOf(Gear.WINDBREAKER), gear(wind = 30.0))

    @Test
    fun distanceAtTenKm_suggestsWater() = assertEquals(setOf(Gear.WATER), gear(goalValue = 10_000))

    @Test
    fun distanceJustBelowTenKm_noWater() = assertEquals(emptySet<Gear>(), gear(goalValue = 9_900))

    @Test
    fun durationAtSixtyMinutes_suggestsWater() =
        assertEquals(setOf(Gear.WATER), gear(goalType = GoalType.DURATION, goalValue = 3_600))

    @Test
    fun durationJustBelowSixtyMinutes_noWater() =
        assertEquals(emptySet<Gear>(), gear(goalType = GoalType.DURATION, goalValue = 3_540))

    @Test
    fun sameItemFromSeveralRules_appearsOnce() {
        // Nóng (BR-22) và mục tiêu dài (BR-24) cùng gợi ý nước uống, chỉ hiện một lần
        val result = gear(temp = 36.0, goalType = GoalType.DURATION, goalValue = 3_600)
        assertTrue(Gear.WATER in result)
        assertEquals(setOf(Gear.HAT, Gear.WATER), result)
    }

    // SPEC 5.2: giờ dự báo làm tròn xuống giờ chẵn
    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    private fun millisAt(hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 10, 29, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun forecastHour_halfPastSix_roundsToSix() =
        assertEquals(millisAt(6, 0), WeatherRules.forecastHour(millisAt(6, 30), zone))

    @Test
    fun forecastHour_seven_roundsToSix() =
        assertEquals(millisAt(6, 0), WeatherRules.forecastHour(millisAt(7, 0), zone))

    @Test
    fun forecastHour_evenHour_unchanged() =
        assertEquals(millisAt(6, 0), WeatherRules.forecastHour(millisAt(6, 0), zone))

    @Test
    fun forecastHour_oneThirty_roundsToMidnight() =
        assertEquals(millisAt(0, 0), WeatherRules.forecastHour(millisAt(1, 30), zone))

    // SPEC 5.2: dữ liệu đã lưu hơn 3 giờ thì lấy lại
    @Test
    fun isStale_exactlyThreeHours_notStale() {
        val fetchedAt = millisAt(6, 0)
        assertFalse(WeatherRules.isStale(fetchedAt, fetchedAt + 3 * 60 * 60_000L))
    }

    @Test
    fun isStale_justOverThreeHours_stale() {
        val fetchedAt = millisAt(6, 0)
        assertTrue(WeatherRules.isStale(fetchedAt, fetchedAt + 3 * 60 * 60_000L + 1))
    }

    @Test
    fun showRefreshButton_noData_true() = assertTrue(WeatherRules.showRefreshButton(null, millisAt(8, 0)))

    @Test
    fun showRefreshButton_freshData_false() =
        assertFalse(WeatherRules.showRefreshButton(millisAt(6, 0), millisAt(8, 0)))

    @Test
    fun showRefreshButton_staleData_true() =
        assertTrue(WeatherRules.showRefreshButton(millisAt(4, 0), millisAt(8, 0)))

    @Test
    fun canRefresh_completedCancelledMissed_false() {
        assertFalse(WeatherRules.canRefresh(PlanStatus.COMPLETED))
        assertFalse(WeatherRules.canRefresh(PlanStatus.CANCELLED))
        assertFalse(WeatherRules.canRefresh(PlanStatus.MISSED))
    }

    @Test
    fun canRefresh_plannedAndInProgress_true() {
        assertTrue(WeatherRules.canRefresh(PlanStatus.PLANNED))
        assertTrue(WeatherRules.canRefresh(PlanStatus.IN_PROGRESS))
    }
}
