package com.nhacongdong.runmate.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.domain.DayRingState
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.NhipStops
import com.nhacongdong.runmate.ui.theme.RunRingSize
import com.nhacongdong.runmate.ui.theme.RunRingSizes
import com.nhacongdong.runmate.ui.theme.RunRingStroke
import com.nhacongdong.runmate.ui.theme.nhipSweep

// Vòng chạy (DESIGN 3). Trang trí: người gọi tự mô tả cho trình đọc màn hình (DESIGN 8.3, 12).
// thumbnail: quỹ đạo đã chuẩn hoá về 0..1. Chưa có thì lõi hiện biểu tượng directions_run (DESIGN 3.1)
@Composable
fun RunRing(
    state: DayRingState,
    size: RunRingSize,
    modifier: Modifier = Modifier,
    thumbnail: List<Offset>? = null,
) {
    val reducedMotion = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "runRing")
    // Vòng xoay 360° mỗi 4 giây, chấm nhấp nháy chu kỳ 1,2 giây (DESIGN 11)
    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing)), label = "spin",
    )
    val blink by transition.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "blink",
    )
    val running = state is DayRingState.Running
    val angle = if (running && !reducedMotion) spin else 0f
    val dotAlpha = if (running && !reducedMotion) blink else 1f

    val colors = MaterialTheme.colorScheme
    val hasCore = state is DayRingState.Ran || running

    Box(modifier.size(size.outer), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            when (state) {
                is DayRingState.Ran -> drawGradientRing(size, state.runCount, angle)
                DayRingState.Running -> drawGradientRing(size, 1, angle)
                DayRingState.Planned -> drawOutline(colors.onSurfaceVariant, dashed = true)
                DayRingState.Missed -> drawOutline(colors.outline, dashed = false)
                DayRingState.Empty -> Unit
            }
            if (hasCore) drawCircle(colors.surfaceContainer, radius = size.core.toPx() / 2)
            if (state is DayRingState.Ran && thumbnail != null && thumbnail.size >= 2) {
                drawThumbnail(thumbnail, size, colors.onSurface)
            }
            if (running) {
                drawCircle(brush = nhipSweep(center), radius = 5.dp.toPx(), alpha = dotAlpha)
            }
        }

        if (state is DayRingState.Ran && (thumbnail == null || thumbnail.size < 2)) {
            Icon(
                painterResource(R.drawable.ic_directions_run), contentDescription = null,
                tint = colors.onSurface, modifier = Modifier.size(size.core * 0.55f),
            )
        }

        when (state) {
            is DayRingState.Ran -> {
                if (state.runCount > 1) {
                    Badge(size.badge, colors.primary, Modifier.align(Alignment.TopEnd)) {
                        Text(
                            "${state.runCount}", style = MaterialTheme.typography.labelSmall,
                            color = colors.onPrimary, textAlign = TextAlign.Center,
                        )
                    }
                }
                if (state.allGoalsReached) {
                    Badge(size.badge, colors.primary, Modifier.align(Alignment.BottomEnd)) {
                        Icon(
                            painterResource(R.drawable.ic_check), contentDescription = null,
                            tint = colors.onPrimary, modifier = Modifier.size(size.badge * 0.7f),
                        )
                    }
                }
            }
            DayRingState.Missed -> Badge(size.badge, colors.error, Modifier.align(Alignment.BottomEnd)) {
                Text("!", style = MaterialTheme.typography.labelSmall, color = colors.onError)
            }
            else -> Unit
        }
    }
}

// Huy hiệu tròn, viền 2 dp màu nền để tách khỏi vòng (DESIGN 3.2)
@Composable
private fun Badge(size: Dp, color: Color, modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(size)
            .border(RunRingStroke.badgeBorder, MaterialTheme.colorScheme.background, CircleShape)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// Vòng gradient đầy hoặc chia đoạn bằng nhau, tối đa 4 đoạn, khe 6° (DESIGN 3.2, 13.4, D4)
private fun DrawScope.drawGradientRing(ringSize: RunRingSize, runCount: Int, angle: Float) {
    val width = ringSize.ring.toPx()
    val radius = size.minDimension / 2 - width / 2
    rotate(-90f + angle) {
        if (runCount <= 1) {
            drawCircle(brush = nhipSweep(center), radius = radius, style = Stroke(width))
        } else {
            val segments = runCount.coerceAtMost(RunRingStroke.MAX_SEGMENTS)
            val gap = RunRingStroke.SEGMENT_GAP_DEG
            val sweep = 360f / segments - gap
            repeat(segments) { i ->
                drawArc(
                    brush = nhipSweep(center),
                    startAngle = i * (sweep + gap) + gap / 2,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width, cap = StrokeCap.Round),
                )
            }
        }
    }
}

// Vòng nét đứt (PLANNED) hoặc nét liền xám (MISSED), 1,5 dp (DESIGN 3.2)
private fun DrawScope.drawOutline(color: Color, dashed: Boolean) {
    val width = RunRingStroke.outline.toPx()
    val effect = if (dashed) {
        PathEffect.dashPathEffect(floatArrayOf(RunRingStroke.dashOn.toPx(), RunRingStroke.dashOff.toPx()))
    } else null
    drawCircle(color, radius = size.minDimension / 2 - width / 2, style = Stroke(width, pathEffect = effect))
}

// Quỹ đạo thu nhỏ trong lõi, giữ tỷ lệ, đệm 6 dp (DESIGN 3.1)
private fun DrawScope.drawThumbnail(points: List<Offset>, ringSize: RunRingSize, color: Color) {
    val box = ringSize.core.toPx() - 2 * 6.dp.toPx()
    val origin = Offset(center.x - box / 2, center.y - box / 2)
    val path = Path().apply {
        points.forEachIndexed { i, p ->
            val x = origin.x + p.x * box
            val y = origin.y + p.y * box
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
    drawPath(path, color, style = Stroke(ringSize.track.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@ThemePreviews
@Composable
private fun RunRingPreview() = PreviewBox {
    val states = listOf(
        DayRingState.Ran(runCount = 1, allGoalsReached = true, thumbnailSessionId = 1),
        DayRingState.Ran(runCount = 3, allGoalsReached = false, thumbnailSessionId = 1),
        DayRingState.Running,
        DayRingState.Planned,
        DayRingState.Missed,
        DayRingState.Empty,
    )
    val loop = listOf(Offset(0.2f, 0.8f), Offset(0.1f, 0.3f), Offset(0.5f, 0.1f), Offset(0.9f, 0.4f), Offset(0.7f, 0.9f), Offset(0.2f, 0.8f))
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        listOf(RunRingSizes.S, RunRingSizes.M).forEach { ringSize ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                states.forEach { RunRing(it, ringSize) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
            RunRing(states.first(), RunRingSizes.M, thumbnail = loop)
            RunRing(DayRingState.Planned, RunRingSizes.L)
        }
    }
}
