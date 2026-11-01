package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.TrackPoint
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.tan

// Điểm đã chuẩn hoá về khung vuông 0..1 để vẽ (DESIGN 8.17)
data class NormPoint(val x: Double, val y: Double)

// Đổi toạ độ GPS thành đường vẽ. Hàm thuần, không phụ thuộc Android
object TrackGeometry {

    // Vĩ độ ngoài khoảng này thì chiếu Mercator không xác định (DESIGN 8.17)
    private const val MAX_LAT = 85.0

    // Mỗi segmentIndex là một đường riêng, không nối với nhau (BR-33). Giữ thứ tự thời gian trong mỗi đoạn.
    // Chiếu Mercator đơn giản nên giữ tỷ lệ Bắc–Nam và Đông–Tây, bắc ở trên, căn giữa trong khung vuông.
    fun toPaths(points: List<TrackPoint>): List<List<NormPoint>> {
        if (points.isEmpty()) return emptyList()

        val projected = points.map { it.segmentIndex to project(it.latitude, it.longitude) }
        val minX = projected.minOf { it.second.first }
        val maxX = projected.maxOf { it.second.first }
        val minY = projected.minOf { it.second.second }
        val maxY = projected.maxOf { it.second.second }
        // Cạnh của khung vuông bằng cạnh dài hơn, nên hai trục dùng cùng một tỷ lệ
        val span = max(maxX - minX, maxY - minY)
        // Mọi điểm trùng nhau thì đặt ở giữa khung
        if (span == 0.0) {
            return points.groupBy { it.segmentIndex }.values.map { segment -> segment.map { NormPoint(0.5, 0.5) } }
        }
        val offsetX = (span - (maxX - minX)) / 2
        val offsetY = (span - (maxY - minY)) / 2

        return projected
            .map { (segment, p) ->
                segment to NormPoint((p.first - minX + offsetX) / span, (maxY - p.second + offsetY) / span)
            }
            .groupBy({ it.first }, { it.second })
            .values
            .toList()
    }

    // Toạ độ Mercator (x, y) theo radian, y hướng lên bắc
    private fun project(latitude: Double, longitude: Double): Pair<Double, Double> {
        val lat = latitude.coerceIn(-MAX_LAT, MAX_LAT)
        val x = Math.toRadians(longitude)
        val y = ln(tan(PI / 4 + Math.toRadians(lat) / 2))
        return x to y
    }
}
