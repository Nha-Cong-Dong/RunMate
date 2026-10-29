package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.data.plan
import com.nhacongdong.runmate.data.session
import org.junit.Assert.assertEquals
import org.junit.Test

// Test đủ 6 dòng của DESIGN bảng 3.2 và thứ tự ưu tiên. Mục tiêu của session() là 5 km.
class DayRingStateTest {
    private val reached = 5_200.0      // 104%
    private val notReached = 3_000.0   // 60%

    @Test
    fun nothing_orOnlyCancelled_isEmpty() {
        assertEquals(DayRingState.Empty, dayRingState(emptyList(), emptyList()))
        assertEquals(DayRingState.Empty, dayRingState(listOf(plan(PlanStatus.CANCELLED)), emptyList()))
    }

    @Test
    fun onlyPlanned_isPlanned() =
        assertEquals(DayRingState.Planned, dayRingState(listOf(plan(PlanStatus.PLANNED)), emptyList()))

    @Test
    fun onlyMissed_isMissed() =
        assertEquals(DayRingState.Missed, dayRingState(listOf(plan(PlanStatus.MISSED)), emptyList()))

    @Test
    fun plannedAndMissed_plannedWins() =
        assertEquals(
            DayRingState.Planned,
            dayRingState(listOf(plan(PlanStatus.MISSED), plan(PlanStatus.PLANNED)), emptyList())
        )

    @Test
    fun oneRunReached_isRanWithBadge() =
        assertEquals(
            DayRingState.Ran(runCount = 1, allGoalsReached = true, thumbnailSessionId = 1),
            dayRingState(emptyList(), listOf(session(null, distanceMeters = reached, id = 1)))
        )

    @Test
    fun runAndPlanned_ranWins() {
        val state = dayRingState(
            listOf(plan(PlanStatus.PLANNED)),
            listOf(session(null, distanceMeters = reached, id = 1))
        )
        assertEquals(DayRingState.Ran(1, true, 1), state)
    }

    @Test
    fun twoRuns_oneNotReached_noBadge_thumbnailIsLongest() {
        val state = dayRingState(
            emptyList(),
            listOf(session(null, distanceMeters = reached, id = 1), session(null, distanceMeters = notReached, id = 2))
        )
        assertEquals(DayRingState.Ran(runCount = 2, allGoalsReached = false, thumbnailSessionId = 1), state)
    }

    @Test
    fun activeSession_runningWinsOverEverything() {
        val state = dayRingState(
            listOf(plan(PlanStatus.IN_PROGRESS)),
            listOf(
                session(null, distanceMeters = reached, id = 1),
                session(null, status = SessionStatus.PAUSED, id = 2),
            )
        )
        assertEquals(DayRingState.Running, state)
    }
}
