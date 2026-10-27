package com.nhacongdong.runmate.data.local

import com.nhacongdong.runmate.data.plan
import com.nhacongdong.runmate.data.testDatabase
import com.nhacongdong.runmate.domain.model.GoalType
import com.nhacongdong.runmate.domain.model.PlanStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
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
        db = testDatabase()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertThenRead_keepsEnumAndNullWeather() = runTest {
        val id = db.planDao().insert(plan())

        val saved = db.planDao().getById(id)
        assertEquals(GoalType.DISTANCE, saved?.goalType)
        assertEquals(PlanStatus.PLANNED, saved?.status)
        assertNull(saved?.weather)
    }

    @Test
    fun insertWithWeather_readsBackSame() = runTest {
        val weather = WeatherSnapshot(
            weatherFetchedAt = 500, temperatureC = 28.5, precipitationProb = 40,
            windSpeedKmh = 12.0, uvIndex = 6.5
        )
        val id = db.planDao().insert(plan(weather = weather))

        assertEquals(weather, db.planDao().getById(id)?.weather)
    }

    @Test
    fun updateStatus_changesOnlyStatus() = runTest {
        val id = db.planDao().insert(plan())
        val before = db.planDao().getById(id)!!

        db.planDao().updateStatus(id, PlanStatus.MISSED)

        assertEquals(before.copy(status = PlanStatus.MISSED), db.planDao().getById(id))
    }

    @Test
    fun observeAll_emitsAgainAfterInsert() = runTest {
        // Room phát trên luồng riêng nên dùng Channel để chờ từng lần phát
        val emissions = Channel<List<RunPlan>>(Channel.UNLIMITED)
        backgroundScope.launch(Dispatchers.Default) {
            db.planDao().observeAll().collect { emissions.send(it) }
        }

        assertEquals(0, emissions.receive().size)   // lần phát đầu: bảng trống
        db.planDao().insert(plan())
        assertEquals(1, emissions.receive().size)   // phát lại sau khi insert
    }
}
