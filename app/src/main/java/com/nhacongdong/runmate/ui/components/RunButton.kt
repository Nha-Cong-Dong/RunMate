package com.nhacongdong.runmate.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.ui.theme.nhipSweep

private val RingWidth = 3.dp
private val RingGap = 3.dp

// Nút Chạy ở giữa thanh dưới (DESIGN 8.1): vòng gradient 3 dp, khe 3 dp màu surface, lõi inverse.
// Cùng cấu tạo với vòng chạy: "bấm vào đây sẽ tạo ra vòng màu".
// Giai đoạn E: trạng thái đang có buổi chạy dở (vòng xoay, chấm tròn, mở thẳng S6)
@Composable
fun RunButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Button)
            .drawBehind {
                val width = RingWidth.toPx()
                rotate(-90f) {
                    drawCircle(nhipSweep(center), radius = size.minDimension / 2 - width / 2, style = Stroke(width))
                }
            }
            .padding(RingWidth)
            .border(RingGap, colors.surface, CircleShape)
            .padding(RingGap)
            .background(colors.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_play_arrow),
            contentDescription = stringResource(R.string.tab_run),
            tint = colors.onPrimary,
        )
    }
}

@ThemePreviews
@Composable
private fun RunButtonPreview() = PreviewBox { RunButton(onClick = {}) }
