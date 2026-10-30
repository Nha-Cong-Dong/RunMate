package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.local.RunMateDatabase
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.plan
import com.nhacongdong.runmate.data.testDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// Test cập nhật MISSED khi mở app (SPEC 6.3, BR-09).
class RefreshMissedPlansTest {
    private val minute = 60_000L
    private val start = 1_000L          // giờ bắt đầu của plan() trong TestData

    private lateinit var db: RunMateDatabase
    private lateinit var refreshMissedPlans: RefreshMissedPlans

    @Before
    fun setUp() {
        db = testDatabase()
        refreshMissedPlans = RefreshMissedPlans(PlanRepository(db.planDao()))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun plannedOverdue_becomesMissed() = runTest {
        val id = db.planDao().insert(plan())

        refreshMissedPlans(now = start + 60 * minute)

        assertEquals(PlanStatus.MISSED, db.planDao().getById(id)?.status)
    }

    @Test
    fun plannedWithinGrace_staysPlanned() = runTest {
        val id = db.planDao().insert(plan())

        refreshMissedPlans(now = start + 20 * minute)

        assertEquals(PlanStatus.PLANNED, db.planDao().getById(id)?.status)
    }

    @Test
    fun otherStatuses_untouched() = runTest {
        val completed = db.planDao().insert(plan(status = PlanStatus.COMPLETED))
        val cancelled = db.planDao().insert(plan(status = PlanStatus.CANCELLED))

        refreshMissedPlans(now = start + 60 * minute)

        assertEquals(PlanStatus.COMPLETED, db.planDao().getById(completed)?.status)
        assertEquals(PlanStatus.CANCELLED, db.planDao().getById(cancelled)?.status)
    }
}
