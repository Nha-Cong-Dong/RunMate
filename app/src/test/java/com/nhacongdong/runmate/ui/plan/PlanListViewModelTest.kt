package com.nhacongdong.runmate.ui.plan

import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.SessionRepository
import com.nhacongdong.runmate.data.local.RunMateDatabase
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.plan
import com.nhacongdong.runmate.data.testDatabase
import com.nhacongdong.runmate.domain.DayEntry
import com.nhacongdong.runmate.domain.DayRingState
import com.nhacongdong.runmate.domain.RefreshMissedPlans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// Test trạng thái S1: tuần, ngày đang chọn, giới hạn 16 ngày, MISSED khi quay lại (SPEC 9.1, BR-02, BR-09).
@OptIn(ExperimentalCoroutinesApi::class)
class PlanListViewModelTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val today = LocalDate.of(2026, 10, 29)                 // Thứ Năm
    private var now = millis(today, 7, 0)

    private lateinit var db: RunMateDatabase
    private lateinit var planRepository: PlanRepository
    private lateinit var vm: PlanListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = testDatabase()
        planRepository = PlanRepository(db.planDao())
        vm = PlanListViewModel(
            planRepository,
            SessionRepository(db, db.sessionDao(), db.trackPointDao(), db.planDao()),
            RefreshMissedPlans(planRepository),
            clock = { now },
            zone = zone,
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun start_selectsToday_inCurrentWeek_firstUse() = runTest {
        val state = vm.uiState.first { !it.isLoading }

        assertEquals(today, state.selected)
        assertEquals(LocalDate.of(2026, 10, 26), state.weekStart)
        assertEquals(7, state.days.size)
        assertTrue(state.isFirstUse)
        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun planToday_showsInListAndRing() = runTest {
        planRepository.addPlan(plan(startTime = millis(today, 18, 0)))

        val state = vm.uiState.first { it.entries.isNotEmpty() }

        assertTrue(state.entries.single() is DayEntry.PlanEntry)
        assertEquals(DayRingState.Planned, state.days[3].ring)    // T5
        assertEquals(1, state.days[3].planCount)
        assertFalse(state.isFirstUse)
    }

    @Test
    fun selectOtherDay_listFollows() = runTest {
        planRepository.addPlan(plan(startTime = millis(today, 18, 0)))
        vm.uiState.first { it.entries.isNotEmpty() }

        vm.selectDate(today.plusDays(1))

        val state = vm.uiState.first { it.selected == today.plusDays(1) }
        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun nextWeek_stopsAtWeekOfToday16Days() = runTest {
        vm.nextWeek()
        vm.nextWeek()                                              // tuần 09/11 chứa 14/11 = hôm nay + 16
        assertFalse(vm.uiState.first { !it.isLoading }.canGoNext)

        vm.nextWeek()                                              // bị chặn

        assertEquals(LocalDate.of(2026, 11, 9), vm.uiState.first { !it.isLoading }.weekStart)
    }

    @Test
    fun prevWeek_keepsWeekday_thenTodayReturns() = runTest {
        vm.prevWeek()
        assertEquals(LocalDate.of(2026, 10, 22), vm.uiState.first { !it.isLoading }.selected)

        vm.goToday()

        val state = vm.uiState.first { !it.isLoading }
        assertEquals(today, state.selected)
        assertEquals(LocalDate.of(2026, 10, 26), state.weekStart)
    }

    @Test
    fun refreshOnResume_marksOverduePlanMissed() = runTest {
        planRepository.addPlan(plan(startTime = millis(today, 6, 0)))
        now = millis(today, 7, 0)                                  // quá 30 phút

        vm.refreshStatuses()

        val state = vm.uiState.first { it.days.getOrNull(3)?.ring == DayRingState.Missed }
        assertEquals(PlanStatus.MISSED, (state.entries.single() as DayEntry.PlanEntry).plan.status)
    }

    @Test
    fun refreshAfterMidnight_movesToNewToday() = runTest {
        now = millis(today.plusDays(1), 0, 5)

        vm.refreshStatuses()

        val state = vm.uiState.first { !it.isLoading }
        assertEquals(today.plusDays(1), state.today)
        assertEquals(today.plusDays(1), state.selected)
    }

    private fun millis(date: LocalDate, hour: Int, minute: Int): Long =
        LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
}
