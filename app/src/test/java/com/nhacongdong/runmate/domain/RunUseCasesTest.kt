package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.SessionRepository
import com.nhacongdong.runmate.data.local.RunMateDatabase
import com.nhacongdong.runmate.data.local.TrackPoint
import com.nhacongdong.runmate.data.local.WeatherSnapshot
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.data.plan
import com.nhacongdong.runmate.data.testDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Test vòng đời buổi chạy: bắt đầu, tạm dừng, tiếp tục, kết thúc (SPEC 6.3, SPEC 7.3)
class RunUseCasesTest {
    private lateinit var db: RunMateDatabase
    private lateinit var plans: PlanRepository
    private lateinit var sessions: SessionRepository

    private val sampleWeather = WeatherSnapshot(
        weatherFetchedAt = 1_000L,
        temperatureC = 26.5,
        precipitationProb = 15,
        windSpeedKmh = 14.0,
        uvIndex = 5.0,
    )

    @Before
    fun setUp() {
        db = testDatabase()
        plans = PlanRepository(db.planDao())
        sessions = SessionRepository(db, db.sessionDao(), db.trackPointDao(), db.planDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun startFreeRun_withWeather_savesWeatherInSession() = runTest {
        val startFreeRun = StartFreeRun(sessions, clock = { 2_000L })

        val sessionId = startFreeRun(
            locationName = "Hồ Tây",
            latitude = 21.05,
            longitude = 105.82,
            goalType = GoalType.DISTANCE,
            goalValue = 5_000,
            weather = sampleWeather,
        )

        assertNotNull(sessionId)
        val saved = sessions.observeSession(sessionId!!).first()
        assertNotNull(saved)
        assertEquals(SessionStatus.RECORDING, saved?.status)
        assertEquals(sampleWeather, saved?.weather)
    }

    @Test
    fun startFreeRun_withoutWeather_savesNullWeather() = runTest {
        val startFreeRun = StartFreeRun(sessions, clock = { 2_000L })

        val sessionId = startFreeRun(
            locationName = "Hồ Gươm",
            latitude = 21.03,
            longitude = 105.85,
            goalType = GoalType.DURATION,
            goalValue = 1_800,
        )

        assertNotNull(sessionId)
        val saved = sessions.observeSession(sessionId!!).first()
        assertNotNull(saved)
        assertNull(saved?.weather)
    }

    @Test
    fun startPlanRun_copiesWeatherAndSetsPlanInProgress() = runTest {
        val planId = db.planDao().insert(plan(status = PlanStatus.PLANNED, weather = sampleWeather, startTime = 5_000L))
        val startPlanRun = StartPlanRun(sessions, plans, clock = { 5_000L })

        val sessionId = startPlanRun(planId)

        assertNotNull(sessionId)
        val saved = sessions.observeSession(sessionId!!).first()
        assertNotNull(saved)
        assertEquals(planId, saved?.planId)
        assertEquals(sampleWeather, saved?.weather)
        assertEquals(PlanStatus.IN_PROGRESS, db.planDao().getById(planId)?.status)
    }

    @Test
    fun pauseAndResume_changesSessionStatus() = runTest {
        val startFreeRun = StartFreeRun(sessions, clock = { 1_000L })
        val pauseRun = PauseRun(sessions)
        val resumeRun = ResumeRun(sessions)

        val sessionId = startFreeRun("Công viên Thống Nhất", 21.01, 105.84, GoalType.DISTANCE, 3_000)!!

        val paused = pauseRun(sessionId)
        assertTrue(paused)
        assertEquals(SessionStatus.PAUSED, sessions.observeSession(sessionId).first()?.status)

        val resumed = resumeRun(sessionId)
        assertTrue(resumed)
        assertEquals(SessionStatus.RECORDING, sessions.observeSession(sessionId).first()?.status)
    }

    @Test
    fun finishRun_tooShort_deletesSession() = runTest {
        val startFreeRun = StartFreeRun(sessions, clock = { 1_000L })
        val finishRun = FinishRun(sessions, plans, clock = { 1_030L })

        val sessionId = startFreeRun("Hồ Tây", 21.05, 105.82, GoalType.DISTANCE, 5_000)!!
        // Chưa có điểm GPS nào hoặc quá ngắn (< 50m và < 60s)
        val result = finishRun(sessionId)

        assertNotNull(result)
        assertFalse(result!!.saved)
        assertNull(sessions.observeSession(sessionId).first())
    }

    @Test
    fun finishRun_savable_completesSessionAndPlan() = runTest {
        val planId = db.planDao().insert(plan(status = PlanStatus.PLANNED, startTime = 1_000L))
        val startPlanRun = StartPlanRun(sessions, plans, clock = { 1_000L })
        val finishRun = FinishRun(sessions, plans, clock = { 1_100_000L })

        val sessionId = startPlanRun(planId)!!

        // Thêm 2 điểm GPS cách nhau 100m, thời gian 100s (> 50m, > 60s: BR-36)
        db.trackPointDao().insert(
            TrackPoint(sessionId = sessionId, segmentIndex = 0, timestamp = 1_000L, latitude = 21.0000, longitude = 105.0000, accuracyM = 5f)
        )
        db.trackPointDao().insert(
            TrackPoint(sessionId = sessionId, segmentIndex = 0, timestamp = 101_000L, latitude = 21.0009, longitude = 105.0000, accuracyM = 5f)
        )

        val result = finishRun(sessionId)

        assertNotNull(result)
        assertTrue(result!!.saved)
        val finishedSession = sessions.observeSession(sessionId).first()
        assertNotNull(finishedSession)
        assertEquals(SessionStatus.FINISHED, finishedSession?.status)
        assertEquals(PlanStatus.COMPLETED, db.planDao().getById(planId)?.status)
    }
}
