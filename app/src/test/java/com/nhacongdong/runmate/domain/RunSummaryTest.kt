package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.TrackPoint
import com.nhacongdong.runmate.data.model.GoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Test số liệu sống (SPEC 5.3, BR-35, BR-37)
class RunSummaryTest {
    private val t0 = 1_000_000L
    private val lat100m = 0.0009

    private fun point(lat: Double, offsetMs: Long) = TrackPoint(
        sessionId = 1, segmentIndex = 0, timestamp = t0 + offsetMs,
        latitude = lat, longitude = 0.0, accuracyM = 5f,
    )

    // 10 điểm, mỗi điểm cách nhau khoảng 100 m và 10 giây: tổng khoảng 900 m trong 90 giây
    private val points = (0 until 10).map { point(lat100m * it, it * 10_000L) }

    @Test
    fun summarize_distanceAndDuration() {
        val s = RunSummaryRules.summarize(points, GoalType.DISTANCE, 5_000)
        assertEquals(900.0, s.distanceM, 2.0)
        assertEquals(90_000L, s.durationMs)
    }

    @Test
    fun summarize_pace_fromDistanceAndDuration() {
        val s = RunSummaryRules.summarize(points, GoalType.DISTANCE, 5_000)
        // 90 giây cho 0,9 km, tức 100 giây mỗi km
        assertEquals(100.0, s.paceSecPerKm!!, 0.5)
    }

    @Test
    fun summarize_progressDistance() {
        val s = RunSummaryRules.summarize(points, GoalType.DISTANCE, 1_800)
        assertEquals(0.5, s.progress, 0.01)
        assertFalse(s.goalReached)
    }

    @Test
    fun summarize_progressDuration() {
        val s = RunSummaryRules.summarize(points, GoalType.DURATION, 180)
        assertEquals(0.5, s.progress, 0.001)
        assertFalse(s.goalReached)
    }

    @Test
    fun summarize_goalReached_whenDistanceReachesGoal() {
        val s = RunSummaryRules.summarize(points, GoalType.DISTANCE, 900)
        assertTrue(s.goalReached)
    }

    @Test
    fun summarize_underHundredMetres_noPace() {
        val short = listOf(point(0.0, 0), point(lat100m * 0.5, 60_000))
        assertNull(RunSummaryRules.summarize(short, GoalType.DISTANCE, 5_000).paceSecPerKm)
    }
}
