package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

// Test đổi toạ độ GPS thành đường vẽ (DESIGN 8.17)
class TrackGeometryTest {

    private fun point(lat: Double, lon: Double, segment: Int = 0, time: Long = 0L) = TrackPoint(
        sessionId = 1L,
        segmentIndex = segment,
        timestamp = time,
        latitude = lat,
        longitude = lon,
        accuracyM = 5f,
    )

    @Test
    fun noPoints_noPaths() {
        assertTrue(TrackGeometry.toPaths(emptyList()).isEmpty())
    }

    @Test
    fun samePoint_centered() {
        val paths = TrackGeometry.toPaths(listOf(point(21.0, 105.8), point(21.0, 105.8, time = 1L)))
        assertEquals(1, paths.size)
        assertTrue(paths[0].all { it.x == 0.5 && it.y == 0.5 })
    }

    // Mỗi segmentIndex là một đường riêng (BR-33)
    @Test
    fun segmentsStayApart() {
        val paths = TrackGeometry.toPaths(
            listOf(
                point(21.00, 105.80, segment = 0, time = 0L),
                point(21.01, 105.81, segment = 0, time = 1L),
                point(21.02, 105.82, segment = 1, time = 2L),
                point(21.03, 105.83, segment = 1, time = 3L),
            ),
        )
        assertEquals(2, paths.size)
        assertEquals(listOf(2, 2), paths.map { it.size })
    }

    // Mọi điểm nằm trong khung vuông 0..1, và ít nhất một trục chạm hai cạnh
    @Test
    fun allPointsInsideUnitSquare_andOneAxisFills() {
        val points = (0 until 30).map { i ->
            point(21.0 + 0.003 * kotlin.math.sin(i * 0.3), 105.8 + 0.005 * kotlin.math.cos(i * 0.3), time = i.toLong())
        }
        val all = TrackGeometry.toPaths(points).flatten()
        assertTrue(all.all { it.x in 0.0..1.0 && it.y in 0.0..1.0 })
        val xSpan = all.maxOf { it.x } - all.minOf { it.x }
        val ySpan = all.maxOf { it.y } - all.minOf { it.y }
        assertEquals(1.0, max(xSpan, ySpan), 1e-9)
    }

    // Giữ tỷ lệ: ở xích đạo 0,01° Đông và 0,01° Bắc phải có độ dài bằng nhau
    @Test
    fun eastAndNorthHaveSameScale() {
        val east = TrackGeometry.toPaths(listOf(point(0.0, 0.0), point(0.0, 0.01, time = 1L))).flatten()
        val north = TrackGeometry.toPaths(listOf(point(0.0, 0.0), point(0.01, 0.0, time = 1L))).flatten()
        val eastSpanX = abs(east.maxOf { it.x } - east.minOf { it.x })
        val northSpanY = abs(north.maxOf { it.y } - north.minOf { it.y })
        assertEquals(1.0, eastSpanX, 1e-9)
        assertEquals(1.0, northSpanY, 1e-9)
    }

    // Bắc ở trên: điểm có vĩ độ lớn hơn thì y nhỏ hơn (gần đỉnh khung)
    @Test
    fun northIsUp() {
        val paths = TrackGeometry.toPaths(listOf(point(21.0, 105.8), point(21.01, 105.8, time = 1L)))
        val south = paths[0][0]
        val north = paths[0][1]
        assertTrue(north.y < south.y)
    }

    private fun max(a: Double, b: Double) = kotlin.math.max(a, b)
}
