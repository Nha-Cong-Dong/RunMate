package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Test chuyển trạng thái khi bắt đầu, tạm dừng, tiếp tục, kết thúc buổi chạy (SPEC 6.3, BR-08, BR-36).
// Thời gian tính bằng mili giây
class RunRulesTest {
    private val minute = 60_000L
    private val startTime = 10 * 60 * minute   // giờ bắt đầu kế hoạch

    // BR-08: chỉ một buổi chạy cùng lúc
    @Test
    fun canStartRun_noActiveSession_true() = assertTrue(RunRules.canStartRun(hasActiveSession = false))

    @Test
    fun canStartRun_activeSession_false() = assertFalse(RunRules.canStartRun(hasActiveSession = true))

    // Bắt đầu theo kế hoạch: PLANNED hoặc MISSED thành IN_PROGRESS
    @Test
    fun planAfterStart_plannedAndMissed_inProgress() {
        assertEquals(PlanStatus.IN_PROGRESS, RunRules.planAfterStart(PlanStatus.PLANNED))
        assertEquals(PlanStatus.IN_PROGRESS, RunRules.planAfterStart(PlanStatus.MISSED))
    }

    @Test
    fun planAfterStart_otherStatuses_notAllowed() {
        for (status in listOf(PlanStatus.IN_PROGRESS, PlanStatus.COMPLETED, PlanStatus.CANCELLED)) {
            assertNull(RunRules.planAfterStart(status))
        }
    }

    // Tạm dừng: RECORDING thành PAUSED
    @Test
    fun sessionAfterPause_recording_paused() =
        assertEquals(SessionStatus.PAUSED, RunRules.sessionAfterPause(SessionStatus.RECORDING))

    @Test
    fun sessionAfterPause_otherStatuses_notAllowed() {
        assertNull(RunRules.sessionAfterPause(SessionStatus.PAUSED))
        assertNull(RunRules.sessionAfterPause(SessionStatus.FINISHED))
    }

    // Tiếp tục: PAUSED thành RECORDING
    @Test
    fun sessionAfterResume_paused_recording() =
        assertEquals(SessionStatus.RECORDING, RunRules.sessionAfterResume(SessionStatus.PAUSED))

    @Test
    fun sessionAfterResume_otherStatuses_notAllowed() {
        assertNull(RunRules.sessionAfterResume(SessionStatus.RECORDING))
        assertNull(RunRules.sessionAfterResume(SessionStatus.FINISHED))
    }

    // Kết thúc có lưu: kế hoạch thành COMPLETED (SPEC 6.3)
    @Test
    fun planAfterFinish_saved_completed() =
        assertEquals(PlanStatus.COMPLETED, RunRules.planAfterFinish(saved = true, startTime = startTime, now = startTime))

    // Kết thúc không lưu: chưa quá giờ thì về PLANNED, quá rồi thì về MISSED (SPEC 6.3)
    @Test
    fun planAfterFinish_notSavedWithinGrace_planned() {
        val now = startTime + 30 * minute
        assertEquals(PlanStatus.PLANNED, RunRules.planAfterFinish(saved = false, startTime = startTime, now = now))
    }

    @Test
    fun planAfterFinish_notSavedAfterGrace_missed() {
        val now = startTime + 30 * minute + 1
        assertEquals(PlanStatus.MISSED, RunRules.planAfterFinish(saved = false, startTime = startTime, now = now))
    }

    // BR-44: gợi ý kế hoạch khi giờ hiện tại nằm trong khoảng từ 30 phút trước đến ân hạn 30 phút sau giờ bắt đầu
    private fun plan(id: Long, start: Long, status: PlanStatus = PlanStatus.PLANNED) = RunPlan(
        id = id, startTime = start, locationName = "Hồ Tây", latitude = 21.05, longitude = 105.82,
        goalType = GoalType.DISTANCE, goalValue = 5_000, status = status, createdAt = 0, weather = null,
    )

    @Test
    fun suggestedPlan_noPlans_null() = assertNull(RunRules.suggestedPlan(emptyList(), startTime))

    @Test
    fun suggestedPlan_inWindow_returnsPlan() {
        val p = plan(1, startTime)
        assertEquals(p, RunRules.suggestedPlan(listOf(p), startTime))
    }

    @Test
    fun suggestedPlan_exactlyThirtyMinutesBefore_included() {
        val p = plan(1, startTime)
        assertEquals(p, RunRules.suggestedPlan(listOf(p), startTime - 30 * minute))
    }

    @Test
    fun suggestedPlan_justBeforeWindow_null() {
        val p = plan(1, startTime)
        assertNull(RunRules.suggestedPlan(listOf(p), startTime - 30 * minute - 1))
    }

    @Test
    fun suggestedPlan_exactlyAtGraceEnd_included() {
        val p = plan(1, startTime)
        assertEquals(p, RunRules.suggestedPlan(listOf(p), startTime + 30 * minute))
    }

    @Test
    fun suggestedPlan_afterGrace_null() {
        val p = plan(1, startTime)
        assertNull(RunRules.suggestedPlan(listOf(p), startTime + 30 * minute + 1))
    }

    @Test
    fun suggestedPlan_nonPlannedIgnored() {
        val p = plan(1, startTime, status = PlanStatus.MISSED)
        assertNull(RunRules.suggestedPlan(listOf(p), startTime))
    }

    @Test
    fun suggestedPlan_severalInWindow_nearestStartWins() {
        val now = startTime
        val far = plan(1, startTime + 20 * minute)
        val near = plan(2, startTime - 5 * minute)
        assertEquals(near, RunRules.suggestedPlan(listOf(far, near), now))
    }
}
