package com.nhacongdong.runmate.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Khoảng cách theo lưới 4 dp (DESIGN 6.1) và bo góc (DESIGN 6.2)
object Dimens {
    val space1 = 4.dp      // giữa số và đơn vị
    val space2 = 8.dp      // giữa các phần tử nhỏ trong một nhóm
    val space3 = 12.dp     // giữa các thẻ trong danh sách
    val space4 = 16.dp     // lề màn hình, đệm trong thẻ
    val space5 = 24.dp     // giữa các khối lớn
    val space6 = 32.dp     // giữa các phần của S6
    val space7 = 48.dp     // khoảng trống trên cùng cho trạng thái trống

    val radiusM = 12.dp
    val radiusL = 16.dp
    val radiusXL = 28.dp
}

// Cỡ của RunRing (DESIGN 3.1). Đường kính ngoài = lõi + 2 × (vòng + khe)
data class RunRingSize(
    val outer: Dp,
    val ring: Dp,
    val gap: Dp,
    val core: Dp,
    val track: Dp,       // nét quỹ đạo trong lõi
    val badge: Dp,
)

object RunRingSizes {
    val S = RunRingSize(outer = 44.dp, ring = 2.5.dp, gap = 2.dp, core = 35.dp, track = 1.75.dp, badge = 16.dp)
    val M = RunRingSize(outer = 52.dp, ring = 3.dp, gap = 2.5.dp, core = 41.dp, track = 2.dp, badge = 18.dp)

    // Hình minh hoạ của EmptyState (DESIGN 8.19 chỉ quy định 72 dp, nét đứt). Các số còn lại giữ như cỡ M
    val L = RunRingSize(outer = 72.dp, ring = 3.dp, gap = 2.5.dp, core = 61.dp, track = 2.dp, badge = 18.dp)
}

// Nét của vòng PLANNED, MISSED và nét đứt (DESIGN 3.2, 13.4)
object RunRingStroke {
    val outline = 1.5.dp
    val dashOn = 4.dp
    val dashOff = 3.dp
    val badgeBorder = 2.dp       // viền huy hiệu, màu background (DESIGN 3.2)
    const val SEGMENT_GAP_DEG = 6f
    const val MAX_SEGMENTS = 4
}
