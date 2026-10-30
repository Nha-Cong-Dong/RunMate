package com.nhacongdong.runmate.data.local

import androidx.sqlite.SQLiteException
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.data.plan
import com.nhacongdong.runmate.data.point
import com.nhacongdong.runmate.data.session
import com.nhacongdong.runmate.data.testDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Test ràng buộc của bảng run_session và track_point (SPEC 7.7).
class SessionDaoTest {
    private lateinit var db: RunMateDatabase

    @Before
    fun setUp() {
        db = testDatabase()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun secondSessionForSamePlan_isRejected() = runTest {
        val planId = db.planDao().insert(plan())
        db.sessionDao().insert(session(planId))

        val error = runCatching { db.sessionDao().insert(session(planId)) }.exceptionOrNull()

        assertTrue("Lỗi thực tế: $error", error is SQLiteException)
    }

    @Test
    fun freeRunSessions_manyNullPlanIdAllowed() = runTest {
        val first = db.sessionDao().insert(session(planId = null))
        val second = db.sessionDao().insert(session(planId = null))

        assertNotEquals(first, second)
    }

    @Test
    fun deletePlan_keepsSessionAsFreeRun() = runTest {
        val planId = db.planDao().insert(plan())
        val sessionId = db.sessionDao().insert(session(planId))
        db.trackPointDao().insert(point(sessionId, timestamp = 1))
        db.trackPointDao().insert(point(sessionId, timestamp = 2))

        db.planDao().delete(db.planDao().getById(planId)!!)

        // BR-52: buổi chạy còn, thành chạy tự do, điểm GPS giữ nguyên
        val kept = db.sessionDao().observeById(sessionId).first()
        assertNotNull(kept)
        assertNull(kept?.planId)
        assertEquals(2, db.trackPointDao().getBySession(sessionId).size)
    }

    @Test
    fun getFirstWithStatus_returnsOnlyActive() = runTest {
        db.sessionDao().insert(session(planId = null, status = SessionStatus.FINISHED))
        val activeId = db.sessionDao().insert(session(planId = null, status = SessionStatus.PAUSED))

        val active = db.sessionDao().getFirstWithStatus(
            listOf(SessionStatus.RECORDING, SessionStatus.PAUSED)
        )

        assertEquals(activeId, active?.id)
    }
}
