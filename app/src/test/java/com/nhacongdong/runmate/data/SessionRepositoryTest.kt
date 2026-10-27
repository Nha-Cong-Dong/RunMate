package com.nhacongdong.runmate.data

import com.nhacongdong.runmate.data.local.RunMateDatabase
import com.nhacongdong.runmate.domain.model.PlanStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Test transaction xoá buổi chạy (SPEC 7.7, BR-53).
class SessionRepositoryTest {
    private lateinit var db: RunMateDatabase
    private lateinit var repo: SessionRepository

    @Before
    fun setUp() {
        db = testDatabase()
        repo = SessionRepository(db, db.sessionDao(), db.trackPointDao(), db.planDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun deleteSession_ofPlan_updatesPlanStatus() = runTest {
        val planId = db.planDao().insert(plan(status = PlanStatus.COMPLETED))
        db.sessionDao().insert(session(planId))
        val saved = db.sessionDao().getByPlanId(planId)!!

        repo.deleteSession(saved, planStatusAfter = PlanStatus.MISSED)

        assertNull(db.sessionDao().getByPlanId(planId))
        assertEquals(PlanStatus.MISSED, db.planDao().getById(planId)?.status)
    }

    @Test
    fun deleteSession_ofPlanWithoutStatus_throwsAndRollsBack() = runTest {
        val planId = db.planDao().insert(plan(status = PlanStatus.COMPLETED))
        db.sessionDao().insert(session(planId))
        val saved = db.sessionDao().getByPlanId(planId)!!

        val error = runCatching { repo.deleteSession(saved, planStatusAfter = null) }.exceptionOrNull()

        assertTrue("Lỗi thực tế: $error", error is IllegalArgumentException)
        assertNotNull(db.sessionDao().getByPlanId(planId))                        // buổi chạy vẫn còn
        assertEquals(PlanStatus.COMPLETED, db.planDao().getById(planId)?.status)  // kế hoạch không đổi
    }

    @Test
    fun deleteSession_freeRun_deletesOnly() = runTest {
        val sessionId = db.sessionDao().insert(session(planId = null))
        val saved = db.sessionDao().observeById(sessionId).first()!!

        repo.deleteSession(saved)

        assertNull(db.sessionDao().observeById(sessionId).first())
    }
}
