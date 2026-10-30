package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Test tỷ lệ mục tiêu (SPEC 5.3, BR-37).
class SessionResultTest {
    @Test
    fun distanceGoal_ratioFromDistance() =
        assertEquals(1.048, SessionResult.goalRatio(GoalType.DISTANCE, 5_000, 5_240.0, 9_999), 1e-9)

    @Test
    fun durationGoal_ratioFromActiveTime() =
        assertEquals(0.5, SessionResult.goalRatio(GoalType.DURATION, 3_600, 99_999.0, 1_800), 1e-9)

    @Test
    fun exactlyGoal_isReached() = assertTrue(SessionResult.isGoalReached(session(null, distanceMeters = 5_000.0)))

    @Test
    fun justBelowGoal_isNotReached() = assertFalse(SessionResult.isGoalReached(session(null, distanceMeters = 4_999.0)))
}
