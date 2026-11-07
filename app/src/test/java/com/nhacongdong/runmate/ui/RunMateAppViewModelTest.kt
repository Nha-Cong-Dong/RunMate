package com.nhacongdong.runmate.ui

import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.SessionRepository
import com.nhacongdong.runmate.data.local.RunMateDatabase
import com.nhacongdong.runmate.data.local.TrackPoint
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.data.session
import com.nhacongdong.runmate.data.testDatabase
import com.nhacongdong.runmate.domain.FinishRun
import com.nhacongdong.runmate.domain.PauseRun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Test trạng thái tổng thể app và luồng khôi phục buổi chạy dở (SPEC 10.1, SPEC 10.2a, BR-60)
@OptIn(ExperimentalCoroutinesApi::class)
class RunMateAppViewModelTest {

    private lateinit var db: RunMateDatabase
    private lateinit var plans: PlanRepository
    private lateinit var sessions: SessionRepository
    private lateinit var finishRun: FinishRun
    private lateinit var pauseRun: PauseRun

    private var currentTime = 10_000_000L
    private var serviceRunning = false
    private var locationReady = true
    private var startedServiceSessionId: Long? = null
    private var pauseServiceCalled = false
    private var autoPausedAlertCalled = false

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = testDatabase()
        plans = PlanRepository(db.planDao())
        sessions = SessionRepository(db, db.sessionDao(), db.trackPointDao(), db.planDao())
        finishRun = FinishRun(sessions, plans, clock = { currentTime })
        pauseRun = PauseRun(sessions)
        serviceRunning = false
        locationReady = true
        startedServiceSessionId = null
        pauseServiceCalled = false
        autoPausedAlertCalled = false
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): RunMateAppViewModel {
        return RunMateAppViewModel(
            sessions = sessions,
            finishRun = finishRun,
            startService = { id -> startedServiceSessionId = id },
            pauseService = { pauseServiceCalled = true },
            pauseRun = pauseRun,
            onAutoPaused = { autoPausedAlertCalled = true },
            isLocationReady = { locationReady },
            isServiceRunning = { serviceRunning },
            clock = { currentTime },
        )
    }

    private suspend fun startTestSession(startedAt: Long = currentTime): Long {
        return sessions.startSession(
            session = session(planId = null, status = SessionStatus.RECORDING, startedAt = startedAt),
            planId = null,
            planStatus = null,
        )!!
    }

    @Test
    fun hasActiveRun_emitsFalse_whenNoActiveSession() = runTest {
        val vm = createViewModel()
        assertFalse(vm.hasActiveRun.value)
    }

    @Test
    fun hasActiveRun_emitsTrue_whenSessionIsRecording() = runTest {
        startTestSession()
        val vm = createViewModel()
        val hasActive = vm.hasActiveRun.first { it }
        assertTrue(hasActive)
    }

    @Test
    fun refresh_noSession_setsInterruptedNull() = runTest {
        val vm = createViewModel()
        vm.refresh().join()
        assertNull(vm.interrupted.value)
    }

    @Test
    fun refresh_recordingWithServiceAndLocation_interruptedIsNull() = runTest {
        startTestSession()
        serviceRunning = true
        locationReady = true

        val vm = createViewModel()
        vm.refresh().join()

        assertNull(vm.interrupted.value)
        assertFalse(autoPausedAlertCalled)
        assertFalse(pauseServiceCalled)
    }

    @Test
    fun refresh_recording_serviceLost_autoPausesAndSetsInterrupted() = runTest {
        val sessionId = startTestSession()
        // Thêm 2 điểm GPS cách nhau 10 giây
        db.trackPointDao().insert(
            TrackPoint(
                sessionId = sessionId,
                segmentIndex = 0,
                timestamp = currentTime + 10_000L,
                latitude = 21.028,
                longitude = 105.852,
                accuracyM = 5f,
            )
        )
        db.trackPointDao().insert(
            TrackPoint(
                sessionId = sessionId,
                segmentIndex = 0,
                timestamp = currentTime + 20_000L,
                latitude = 21.029,
                longitude = 105.853,
                accuracyM = 5f,
            )
        )

        // Service bị hệ thống kill
        serviceRunning = false
        locationReady = true
        currentTime += 30_000L // 30s sau

        val vm = createViewModel()
        vm.refresh().join()

        // Phải tự động chuyển DB sang PAUSED
        val active = sessions.getActiveSession()
        assertNotNull(active)
        assertEquals(SessionStatus.PAUSED, active!!.status)
        assertTrue(autoPausedAlertCalled)

        // interrupted state phải được thiết lập
        val interrupted = vm.interrupted.value
        assertNotNull(interrupted)
        assertEquals(sessionId, interrupted!!.sessionId)
        assertEquals(SessionStatus.PAUSED, interrupted.status)
        assertTrue(interrupted.canResume)
        assertEquals(currentTime - 10_000L, interrupted.endedAt) // điểm cuối là (currentTime ban đầu + 20_000L = currentTime - 10_000L)
        assertTrue(interrupted.distanceM > 0)
    }

    @Test
    fun refresh_recording_locationLost_pausesServiceAndAutoPauses() = runTest {
        val sessionId = startTestSession()

        // Đang ghi nhưng người dùng tắt quyền vị trí / GPS
        serviceRunning = true
        locationReady = false

        val vm = createViewModel()
        vm.refresh().join()

        assertTrue(pauseServiceCalled)
        assertTrue(autoPausedAlertCalled)
        val interrupted = vm.interrupted.value
        assertNotNull(interrupted)
        assertEquals(sessionId, interrupted!!.sessionId)
    }

    @Test
    fun refresh_overOneHourSinceLastPoint_canResumeIsFalse() = runTest {
        val sessionId = startTestSession()
        db.trackPointDao().insert(
            TrackPoint(
                sessionId = sessionId,
                segmentIndex = 0,
                timestamp = currentTime,
                latitude = 21.0,
                longitude = 105.8,
                accuracyM = 5f,
            )
        )

        // Trôi qua hơn 1 giờ (BR-60: > 3_600_000L)
        currentTime += 4_000_000L
        serviceRunning = false

        val vm = createViewModel()
        vm.refresh().join()

        val interrupted = vm.interrupted.value
        assertNotNull(interrupted)
        assertFalse("Quá 1 giờ từ điểm cuối thì canResume phải là false (BR-60)", interrupted!!.canResume)
    }

    @Test
    fun resume_startsService_andSignalsOpenRun() = runTest {
        val sessionId = startTestSession()
        serviceRunning = false

        val vm = createViewModel()
        vm.refresh().join()
        assertNotNull(vm.interrupted.value)

        val openRunSignal = async { vm.openRun.first() }

        vm.resume()

        assertEquals(sessionId, startedServiceSessionId)
        assertNull(vm.interrupted.value)
        openRunSignal.await()
    }

    @Test
    fun finish_callsFinishRunWithEndedAt() = runTest {
        val sessionId = startTestSession()
        // Điểm GPS cách hơn 100m để tính là hoàn thành
        db.trackPointDao().insert(
            TrackPoint(
                sessionId = sessionId,
                segmentIndex = 0,
                timestamp = currentTime,
                latitude = 21.050,
                longitude = 105.820,
                accuracyM = 5f,
            )
        )
        db.trackPointDao().insert(
            TrackPoint(
                sessionId = sessionId,
                segmentIndex = 0,
                timestamp = currentTime + 60_000L,
                latitude = 21.055,
                longitude = 105.825,
                accuracyM = 5f,
            )
        )
        serviceRunning = false

        val vm = createViewModel()
        vm.refresh().join()
        assertNotNull(vm.interrupted.value)

        vm.finish()?.join()

        assertNull(vm.interrupted.value)
        // Buổi chạy không còn là active
        assertNull(sessions.getActiveSession())
        val session = sessions.observeSession(sessionId).first()
        assertNotNull(session)
        assertEquals(SessionStatus.FINISHED, session!!.status)
    }
}
