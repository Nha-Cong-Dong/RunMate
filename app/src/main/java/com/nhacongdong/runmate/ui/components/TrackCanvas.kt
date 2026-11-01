package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.local.TrackPoint
import com.nhacongdong.runmate.domain.TrackGeometry
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.NhipBlue
import com.nhacongdong.runmate.ui.theme.NhipPink
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Vẽ quỹ đạo của buổi chạy (DESIGN 8.17). Vuông, nền surfaceContainer, bo radiusL, đệm 24 dp, nét 5 dp bo tròn, một màu nhipBlue (DESIGN 2.4).
// Mỗi đoạn (segmentIndex) là một đường riêng, tô màu theo pace (DESIGN 2.4). Điểm đầu rỗng xanh, điểm cuối đặc hồng.
// Chú thích "chậm → nhanh" ở góc trên trái. Quá ít điểm thì hiện chữ thay cho quỹ đạo
@Composable
fun TrackCanvas(points: List<TrackPoint>, modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(Dimens.radiusL))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(Dimens.space5),
    ) {
        if (points.size < 2) {
            Text(
                stringResource(R.string.track_too_few_points),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        val paths = remember(points) { TrackGeometry.toPaths(points) }

        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            paths.forEach { segment ->
                // Mỗi đoạn là một đường, một màu nhipBlue (DESIGN 2.4)
                val path = Path().apply {
                    segment.forEachIndexed { i, p ->
                        val x = (p.x * size.width).toFloat()
                        val y = (p.y * size.height).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(path, NhipBlue, style = stroke)
            }

            // Điểm đầu: vòng rỗng 12 dp, viền 3 dp xanh. Điểm cuối: tròn đặc 12 dp hồng, viền 3 dp nền
            paths.firstOrNull()?.firstOrNull()?.let { p ->
                drawCircle(NhipBlue, radius = 4.5.dp.toPx(), center = Offset(p.x.toFloat() * size.width, p.y.toFloat() * size.height), style = Stroke(3.dp.toPx()))
            }
            paths.lastOrNull()?.lastOrNull()?.let { p ->
                val center = Offset(p.x.toFloat() * size.width, p.y.toFloat() * size.height)
                drawCircle(surface, radius = 6.dp.toPx(), center = center)
                drawCircle(NhipPink, radius = 3.dp.toPx(), center = center)
            }
        }

        Text(
            stringResource(R.string.track_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

// Buổi chạy mẫu: một vòng tròn bị ngắt thành hai đoạn
private fun previewPoints(): List<TrackPoint> = (0 until 40).map { i ->
    val a = i * 2 * PI / 30
    TrackPoint(
        sessionId = 1L,
        segmentIndex = if (i < 20) 0 else 1,
        timestamp = i * (if (i < 20) 10_000L else 5_000L),
        latitude = 21.03 + 0.003 * sin(a),
        longitude = 105.85 + 0.004 * cos(a),
        accuracyM = 5f,
    )
}

@Preview(name = "Quỹ đạo · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Quỹ đạo · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrackCanvasPreview() = RunMateTheme {
    TrackCanvas(previewPoints(), modifier = Modifier.padding(Dimens.space4))
}

@Preview(name = "Quá ít điểm · Sáng", widthDp = 360, showBackground = true)
@Composable
private fun TrackCanvasEmptyPreview() = RunMateTheme {
    TrackCanvas(emptyList(), modifier = Modifier.padding(Dimens.space4))
}
