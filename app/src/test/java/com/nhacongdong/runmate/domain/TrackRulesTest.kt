package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.TrackPoint
import com.nhacongdong.runmate.data.model.GoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Test quy tắc ghi buổi chạy (SPEC 5.3, BR-30 đến BR-38). Mọi giá trị thời gian là mili giây
class TrackRulesTest {
    private val t0 = 1_000_000L

    private fun point(
        lat: Double,
        lon: Double,
        timeOffsetMs: Long,
        accuracy: Float = 5f,
        segment: Int = 0,
    ) = TrackPoint(
        sessionId = 1, segmentIndex = segment, timestamp = t0 + timeOffsetMs,
        latitude = lat, longitude = lon, accuracyM = accuracy,
    )

    // 0,0009 độ vĩ tuyến khoảng 100 m
    private val lat100m = 0.0009

    // BR-30
    @Test
    fun accuracy_atThirtyMetres_kept() = assertTrue(TrackRules.isAccurate(30f))

    @Test
    fun accuracy_aboveThirtyMetres_dropped() = assertFalse(TrackRules.isAccurate(30.1f))

    // BR-31: tốc độ tối đa 12 m/s
    @Test
    fun step_atNineMetresPerSecond_plausible() {
        val a = point(0.0, 0.0, 0)
        val b = point(lat100m * 0.9, 0.0, 10_000)   // khoảng 90 m trong 10 giây
        assertTrue(TrackRules.isPlausibleStep(a, b))
    }

    @Test
    fun step_aboveTwelveMetresPerSecond_implausible() {
        val a = point(0.0, 0.0, 0)
        val b = point(lat100m * 1.5, 0.0, 10_000)   // khoảng 150 m trong 10 giây = 15 m/s
        assertFalse(TrackRules.isPlausibleStep(a, b))
    }

    @Test
    fun step_sameTimestamp_implausible() {
        val a = point(0.0, 0.0, 0)
        val b = point(lat100m, 0.0, 0)
        assertFalse(TrackRules.isPlausibleStep(a, b))
    }

    @Test
    fun filterValid_dropsInaccurateAndJumpPoints() {
        val points = listOf(
            point(0.0, 0.0, 0),
            point(0.0, 0.0, 1_000, accuracy = 50f),          // sai số lớn, bỏ
            point(lat100m * 50, 0.0, 10_000),                // nhảy xa quá 12 m/s so với điểm đầu
            point(lat100m, 0.0, 10_000),                     // hợp lệ
        )
        val valid = TrackRules.filterValid(points)
        assertEquals(listOf(points[0], points[3]), valid)
    }

    // BR-39: khoảng trống GPS trên 30 giây mở đoạn mới
    @Test
    fun segmentFor_firstPoint_isSegmentZero() =
        assertEquals(0, TrackRules.segmentFor(null, point(0.0, 0.0, 0)))

    @Test
    fun segmentFor_gapOfExactlyThirtySeconds_sameSegment() {
        val last = point(0.0, 0.0, 0)
        assertEquals(0, TrackRules.segmentFor(last, point(0.0, 0.0, 30_000)))
    }

    @Test
    fun segmentFor_gapJustOverThirtySeconds_newSegment() {
        val last = point(0.0, 0.0, 0, segment = 2)
        assertEquals(3, TrackRules.segmentFor(last, point(0.0, 0.0, 30_001, segment = 2)))
    }

    // SPEC 7.4, US9: forceNewSegment ép mở đoạn mới kể cả khi khoảng cách thời gian dưới 30 giây
    @Test
    fun segmentFor_forcedNewSegment_underThirtySeconds_incrementsSegment() {
        val last = point(0.0, 0.0, 0, segment = 0)
        assertEquals(1, TrackRules.segmentFor(last, point(0.0, 0.0, 10_000), forceNewSegment = true))
    }

    @Test
    fun shouldKeep_forcedNewSegment_skipsSpeedCheck() {
        val last = point(0.0, 0.0, 0)
        val next = point(lat100m * 50, 0.0, 10_000)
        assertTrue(TrackRules.shouldKeep(last, next, forceNewSegment = true))
    }

    @Test
    fun shouldKeep_firstPoint_kept() =
        assertTrue(TrackRules.shouldKeep(null, point(0.0, 0.0, 0)))

    @Test
    fun shouldKeep_inaccuratePointAfterGap_dropped() {
        val last = point(0.0, 0.0, 0)
        assertFalse(TrackRules.shouldKeep(last, point(0.0, 0.0, 40_000, accuracy = 50f)))
    }

    @Test
    fun shouldKeep_jumpWithinGap_dropped() {
        // 500 m trong 10 giây là 50 m/s, khoảng trống 10 giây nên vẫn kiểm tra tốc độ
        val last = point(0.0, 0.0, 0)
        assertFalse(TrackRules.shouldKeep(last, point(lat100m * 5, 0.0, 10_000)))
    }

    @Test
    fun shouldKeep_fastButAfterLongGap_kept() {
        // 500 m trong 40 giây là 12,5 m/s. Khoảng trống trên 30 giây thì không kiểm tra tốc độ, mở đoạn mới
        val last = point(0.0, 0.0, 0)
        assertTrue(TrackRules.shouldKeep(last, point(lat100m * 5, 0.0, 40_000)))
    }

    @Test
    fun filterValid_afterLongGap_startsNewSegmentAndSkipsGapDistance() {
        val points = listOf(
            point(0.0, 0.0, 0),
            point(lat100m, 0.0, 10_000),                  // đoạn 0, cộng khoảng 100 m
            point(lat100m * 6, 0.0, 60_000),              // sau 50 giây: đoạn 1, không cộng khoảng nối
            point(lat100m * 7, 0.0, 70_000),              // đoạn 1, cộng khoảng 100 m
        )
        val valid = TrackRules.filterValid(points)
        assertEquals(listOf(0, 0, 1, 1), valid.map { it.segmentIndex })
        assertEquals(200.0, TrackRules.totalDistanceM(valid), 2.0)
    }

    // BR-33: quãng đường không tính đoạn nối qua lúc tạm dừng
    @Test
    fun distance_excludesGapBetweenSegments() {
        val points = listOf(
            point(0.0, 0.0, 0, segment = 0),
            point(lat100m, 0.0, 10_000, segment = 0),
            point(lat100m * 30, 0.0, 300_000, segment = 1),   // đoạn mới sau tạm dừng
            point(lat100m * 31, 0.0, 310_000, segment = 1),
        )
        val distance = TrackRules.totalDistanceM(points)
        // Chỉ cộng hai đoạn trong cùng segment, mỗi đoạn khoảng 100 m
        assertEquals(200.0, distance, 2.0)
    }

    // BR-34: thời gian chạy không tính lúc tạm dừng
    @Test
    fun activeDuration_excludesPause() {
        val points = listOf(
            point(0.0, 0.0, 0, segment = 0),
            point(0.0, 0.0, 60_000, segment = 0),
            point(0.0, 0.0, 300_000, segment = 1),
            point(0.0, 0.0, 360_000, segment = 1),
        )
        assertEquals(120_000L, TrackRules.activeDurationMs(points))
    }

    // BR-35: pace chỉ hiện khi đã chạy ít nhất 100 m
    @Test
    fun pace_fiveKmInTwentyFiveMinutes_threeHundredSecondsPerKm() {
        // 5000 m trong 1500 giây là 300 giây mỗi km (5:00 /km)
        assertEquals(300.0, TrackRules.paceSecPerKm(5_000.0, 1_500_000L)!!, 0.001)
    }

    @Test
    fun pace_under100Metres_isNull() = assertNull(TrackRules.paceSecPerKm(99.0, 60_000L))

    // BR-36: buổi chạy phải đủ 100 m và 1 phút mới được lưu
    @Test
    fun savable_atThresholds_true() = assertTrue(TrackRules.isSavable(100.0, 60_000L))

    @Test
    fun savable_justUnderDistance_false() = assertFalse(TrackRules.isSavable(99.9, 600_000L))

    @Test
    fun savable_justUnderDuration_false() = assertFalse(TrackRules.isSavable(1_000.0, 59_999L))

    // BR-37: đạt mục tiêu
    @Test
    fun reachedGoal_distanceAtGoal_true() =
        assertTrue(TrackRules.reachedGoal(GoalType.DISTANCE, 5_000, 5_000.0, 0L))

    @Test
    fun reachedGoal_distanceJustBelow_false() =
        assertFalse(TrackRules.reachedGoal(GoalType.DISTANCE, 5_000, 4_999.9, 9_999_999L))

    @Test
    fun reachedGoal_durationAtGoal_true() =
        assertTrue(TrackRules.reachedGoal(GoalType.DURATION, 1_800, 0.0, 1_800_000L))

    @Test
    fun reachedGoal_durationJustBelow_false() =
        assertFalse(TrackRules.reachedGoal(GoalType.DURATION, 1_800, 9_999.0, 1_799_999L))

    // BR-38: báo một lần, chỉ khi vừa đạt
    @Test
    fun justReached_onlyWhenCrossingThreshold() {
        assertTrue(TrackRules.justReached(wasReached = false, isReached = true))
        assertFalse(TrackRules.justReached(wasReached = true, isReached = true))
        assertFalse(TrackRules.justReached(wasReached = false, isReached = false))
    }
}
