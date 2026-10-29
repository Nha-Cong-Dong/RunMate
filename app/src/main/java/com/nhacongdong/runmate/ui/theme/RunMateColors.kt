package com.nhacongdong.runmate.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Màu ngoài Material 3 (DESIGN 4.2–4.4, 13.1). Đọc bằng RunMateTheme.colors
@Immutable
data class RunMateColors(
    val ringTrack: Color,
    val warning: Color,
    val warningContainer: Color,
    val success: Color,
    val textDisabled: Color,
)

val LightRunMateColors = RunMateColors(
    ringTrack = LightRingTrack,
    warning = LightWarning,
    warningContainer = LightWarningContainer,
    success = LightSuccess,
    textDisabled = LightTextDisabled,
)

val DarkRunMateColors = RunMateColors(
    ringTrack = DarkRingTrack,
    warning = DarkWarning,
    warningContainer = DarkWarningContainer,
    success = DarkSuccess,
    textDisabled = DarkTextDisabled,
)

val LocalRunMateColors = staticCompositionLocalOf { LightRunMateColors }

// Gradient "Nhịp" (DESIGN 2.1, 13.3)
val NhipStops = arrayOf(
    0.0f to NhipBlue,
    0.2f to NhipCyan,
    0.4f to NhipLime,
    0.6f to NhipYellow,
    0.8f to NhipOrange,
    1.0f to NhipPink,
)

// Vòng tròn: sweepGradient, xoay canvas -90° để bắt đầu từ 12 giờ
fun nhipSweep(center: Offset) = Brush.sweepGradient(*NhipStops, center = center)

// Hình chữ nhật: chéo từ dưới-trái lên trên-phải
fun nhipLinear(size: Size) = Brush.linearGradient(
    *NhipStops,
    start = Offset(0f, size.height),
    end = Offset(size.width, 0f),
)

// Màu tại vị trí t (0..1), dùng tô quỹ đạo theo pace (DESIGN 2.4)
fun nhipColorAt(t: Float): Color {
    val x = t.coerceIn(0f, 1f)
    val i = NhipStops.indexOfLast { it.first <= x }.coerceAtMost(NhipStops.lastIndex - 1)
    val (p0, c0) = NhipStops[i]
    val (p1, c1) = NhipStops[i + 1]
    return lerp(c0, c1, (x - p0) / (p1 - p0))
}
