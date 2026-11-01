package com.nhacongdong.runmate.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.ui.theme.nhipSweep

private val RingWidth = 3.dp
private val RingGap = 3.dp

// Nút Chạy ở giữa thanh dưới (DESIGN 8.1): vòng gradient 3 dp, khe 3 dp màu surface, lõi inverse.
// Cùng cấu tạo với vòng chạy: "bấm vào đây sẽ tạo ra vòng màu".
// running = đang có buổi chạy dở (RECORDING hoặc PAUSED): vòng xoay chậm, biểu tượng đổi thành chấm tròn (DESIGN 8.1, 11)
@Composable
fun RunButton(onClick: () -> Unit, modifier: Modifier = Modifier, running: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val reduced = rememberReducedMotion()
    // Gradient xoay 360° mỗi 4 giây, tuyến tính, chỉ khi đang có buổi dở và không tắt hoạt ảnh.
    // Coroutine bị huỷ khi điều kiện đổi, nên không chạy ngầm lúc không có buổi chạy
    val spinning = running && !reduced
    val angle = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (spinning) {
            while (true) {
                angle.animateTo(360f, tween(4_000, easing = LinearEasing))
                angle.snapTo(0f)
            }
        } else {
            angle.snapTo(0f)
        }
    }
    val description = stringResource(if (running) R.string.cd_run_active else R.string.tab_run)
    Box(
        modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = description }
            .drawBehind {
                val width = RingWidth.toPx()
                rotate(-90f + angle.value) {
                    drawCircle(nhipSweep(center), radius = size.minDimension / 2 - width / 2, style = Stroke(width))
                }
            }
            .padding(RingWidth)
            .border(RingGap, colors.surface, CircleShape)
            .padding(RingGap)
            .background(colors.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (running) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(colors.onPrimary))
        } else {
            Icon(
                painterResource(R.drawable.ic_play_arrow),
                contentDescription = null,
                tint = colors.onPrimary,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun RunButtonPreview() = PreviewBox { RunButton(onClick = {}) }
