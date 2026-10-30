package com.nhacongdong.runmate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nhacongdong.runmate.R

// Inter đóng gói trong res/font, chạy không cần mạng (DESIGN 5.1)
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

private fun inter(size: Int, line: Int, weight: FontWeight, spacing: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = spacing.em,
)

private val Default = Typography()

// Thang chữ (DESIGN 5.2). Các cỡ DESIGN không dùng vẫn đổi sang Inter để không lẫn font hệ thống
val RunMateTypography = Typography(
    displayLarge = inter(96, 96, FontWeight.ExtraBold, -0.04),
    displayMedium = inter(56, 60, FontWeight.ExtraBold, -0.03),
    displaySmall = Default.displaySmall.copy(fontFamily = Inter),
    headlineLarge = Default.headlineLarge.copy(fontFamily = Inter),
    headlineMedium = inter(28, 34, FontWeight.Bold, -0.02),
    headlineSmall = inter(22, 28, FontWeight.Bold, -0.02),
    titleLarge = inter(22, 28, FontWeight.ExtraBold, -0.03),
    titleMedium = inter(16, 22, FontWeight.SemiBold),
    titleSmall = Default.titleSmall.copy(fontFamily = Inter),
    bodyLarge = inter(16, 24, FontWeight.Normal),
    bodyMedium = inter(14, 20, FontWeight.Normal),
    bodySmall = Default.bodySmall.copy(fontFamily = Inter),
    labelLarge = inter(14, 20, FontWeight.SemiBold),
    labelMedium = inter(12, 16, FontWeight.Medium),
    labelSmall = inter(11, 14, FontWeight.SemiBold, 0.02),
)

// Mọi con số dùng chữ số rộng bằng nhau để không nhảy khi cập nhật (DESIGN 5.1)
fun TextStyle.tabular(): TextStyle = merge(TextStyle(fontFeatureSettings = "tnum"))
