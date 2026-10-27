package com.nhacongdong.runmate.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nhacongdong.runmate.domain.model.GoalType
import com.nhacongdong.runmate.domain.model.PlanStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// Test PlanDao trên JVM với database trong RAM (A6).
class PlanDaoTest {
    private lateinit var db: RunMateDatabase

    @Before
    fun setUp() {
        // Mỗi test một database mới, không ảnh hưởng nhau
        db = Room.inMemoryDatabaseBuilder<RunMateDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertThenRead_keepsEnumAndNullWeather() = runTest {
        val id = db.planDao().insert(
            RunPlan(
                startTime = 1_000, locationName = "Hồ Tây", latitude = 21.05, longitude = 105.82,
                goalType = GoalType.DISTANCE, goalValue = 5_000, status = PlanStatus.PLANNED,
                createdAt = 0, weather = null
            )
        )
        val saved = db.planDao().getById(id)
        assertEquals(GoalType.DISTANCE, saved?.goalType)
        assertEquals(PlanStatus.PLANNED, saved?.status)
        assertNull(saved?.weather)
    }
}
