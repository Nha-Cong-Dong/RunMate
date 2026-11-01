package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.TrackPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Test ghi điểm GPS vào kho giả (SPEC 5.3, BR-30, BR-31, BR-39)
class RecordTrackPointTest {
    private val t0 = 1_000_000L
    private val lat100m = 0.0009

    // Kho giả trong bộ nhớ, ghi lại các điểm đã lưu
    private class FakeStore : TrackPointStore {
        val saved = mutableListOf<TrackPoint>()

        override suspend fun lastPoint(sessionId: Long): TrackPoint? =
            saved.filter { it.sessionId == sessionId }.maxByOrNull { it.timestamp }

        override suspend fun addPoint(point: TrackPoint) {
            saved += point
        }
    }

    private val drops = GpsDropCounter()

    private fun record(store: FakeStore, lat: Double, timeOffsetMs: Long, accuracy: Float = 5f) = runBlocking {
        RecordTrackPoint(store, drops)(sessionId = 1, timestamp = t0 + timeOffsetMs, latitude = lat, longitude = 0.0, accuracyM = accuracy)
    }

    @Test
    fun threeDroppedInRow_isWeakGps_andKeptPointResetsCount() {
        val store = FakeStore()
        record(store, 0.0, 0)
        repeat(3) { record(store, lat100m, 10_000L + it * 1_000, accuracy = 50f) }
        assertEquals(3, drops.consecutive.value)
        assertTrue(GpsDropCounter.isWeak(drops.consecutive.value))
        assertTrue(record(store, lat100m, 20_000))
        assertEquals(0, drops.consecutive.value)
    }

    @Test
    fun firstPoint_savedInSegmentZero() {
        val store = FakeStore()
        assertTrue(record(store, 0.0, 0))
        assertEquals(listOf(0), store.saved.map { it.segmentIndex })
    }

    @Test
    fun inaccuratePoint_notSaved() {
        val store = FakeStore()
        record(store, 0.0, 0)
        assertFalse(record(store, lat100m, 10_000, accuracy = 50f))
        assertEquals(1, store.saved.size)
    }

    @Test
    fun jumpWithinGap_notSaved() {
        // 500 m trong 10 giây là 50 m/s, không mở đoạn mới nên bị bỏ
        val store = FakeStore()
        record(store, 0.0, 0)
        assertFalse(record(store, lat100m * 5, 10_000))
        assertEquals(1, store.saved.size)
    }

    @Test
    fun pointAfterLongGap_savedInNewSegment() {
        val store = FakeStore()
        record(store, 0.0, 0)
        record(store, lat100m, 10_000)
        assertTrue(record(store, lat100m * 6, 60_000))   // sau 50 giây: đoạn mới
        assertEquals(listOf(0, 0, 1), store.saved.map { it.segmentIndex })
    }

    @Test
    fun consecutivePoints_sameSegment() {
        val store = FakeStore()
        record(store, 0.0, 0)
        record(store, lat100m, 10_000)
        record(store, lat100m * 2, 20_000)
        assertEquals(listOf(0, 0, 0), store.saved.map { it.segmentIndex })
    }
}
