package com.nhacongdong.runmate.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Bảng xem nhanh theme (DESIGN 4, 5). Chỉ dùng trong Android Studio, không xuất hiện trong app.
@Composable
private fun ThemeSheet() {
    val scheme = MaterialTheme.colorScheme
    val colors = RunMateTheme.colors
    Column(
        modifier = Modifier.background(scheme.background).padding(Dimens.space4),
        verticalArrangement = Arrangement.spacedBy(Dimens.space3),
    ) {
        Text("RunMate", style = MaterialTheme.typography.titleLarge, color = scheme.onBackground)

        // Dải gradient "Nhịp" và vòng chạy
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space4)) {
            Canvas(Modifier.size(width = 200.dp, height = 24.dp)) {
                drawRoundRect(brush = nhipLinear(size), cornerRadius = CornerRadius(12.dp.toPx()))
            }
            Canvas(Modifier.size(RunRingSizes.M.outer)) {
                val width = RunRingSizes.M.ring.toPx()
                rotate(-90f) {
                    drawCircle(
                        brush = nhipSweep(center),
                        radius = size.minDimension / 2 - width / 2,
                        style = Stroke(width),
                    )
                }
            }
        }

        // Màu trung tính và ngữ nghĩa
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
            listOf(
                scheme.surfaceContainer, scheme.surfaceContainerHigh, scheme.outlineVariant, scheme.outline,
                scheme.onSurfaceVariant, scheme.onSurface, scheme.primary,
            ).forEach { Swatch(it, scheme.outline) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
            listOf(colors.warning, colors.warningContainer, scheme.error, scheme.errorContainer, colors.success, colors.ringTrack)
                .forEach { Swatch(it, scheme.outline) }
        }

        // Thang chữ
        val type = MaterialTheme.typography
        listOf(
            "displayMedium 10,24" to type.displayMedium.tabular(),
            "headlineSmall 06:00" to type.headlineSmall.tabular(),
            "titleLarge RunMate" to type.titleLarge,
            "titleMedium Hồ Tây" to type.titleMedium,
            "bodyLarge Nội dung chính" to type.bodyLarge,
            "bodyMedium Mục tiêu 5 km" to type.bodyMedium,
            "labelLarge Xem kết quả" to type.labelLarge,
            "labelMedium T2 T3 T4" to type.labelMedium,
            "labelSmall ĐÃ LÊN KẾ HOẠCH" to type.labelSmall,
        ).forEach { (text, style: TextStyle) -> Text(text, style = style, color = scheme.onBackground) }
        Text("textSecondary: nhãn, chú thích", style = type.bodyMedium, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun Swatch(color: Color, border: Color) {
    Box(
        Modifier
            .size(32.dp)
            .background(color, RoundedCornerShape(Dimens.radiusM))
            .border(1.dp, border, RoundedCornerShape(Dimens.radiusM))
    )
}

@Preview(name = "Sáng", widthDp = 360)
@Composable
private fun ThemeLightPreview() = RunMateTheme(darkTheme = false) { ThemeSheet() }

@Preview(name = "Tối", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemeDarkPreview() = RunMateTheme(darkTheme = true) { ThemeSheet() }
