package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import com.nhacongdong.runmate.ui.theme.nhipSweep

// Vòng tiến độ mục tiêu (DESIGN 8.9). Phần đã đạt là cung gradient bắt đầu từ 12 giờ, đầu cung bo tròn.
// progress ≥ 1 thì vòng đầy, không vẽ vòng thứ hai. Con số bên trong do nơi gọi truyền vào (content)
@Composable
fun GoalRing(
    progress: Double,
    size: Dp,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val track = RunMateTheme.colors.ringTrack
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            // Nền vòng: phần chưa đạt
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            // Phần đã đạt: cung gradient, tối đa cả vòng
            val sweep = (progress.coerceIn(0.0, 1.0) * 360.0).toFloat()
            if (sweep > 0f) {
                drawArc(
                    brush = nhipSweep(center),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

@Preview(name = "Nửa chặng · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Nửa chặng · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GoalRingHalfPreview() = RunMateTheme {
    GoalRing(progress = 0.5, size = 280.dp, strokeWidth = 10.dp)
}

@Preview(name = "Vượt mục tiêu · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Vượt mục tiêu · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GoalRingFullPreview() = RunMateTheme {
    GoalRing(progress = 1.2, size = 280.dp, strokeWidth = 10.dp)
}
