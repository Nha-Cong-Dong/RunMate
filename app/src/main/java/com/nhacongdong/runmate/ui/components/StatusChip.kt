package com.nhacongdong.runmate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunRingStroke
import com.nhacongdong.runmate.ui.theme.nhipSweep

// Nhãn trạng thái kế hoạch (DESIGN 8.8). Luôn có chữ, không chỉ có màu
@Composable
fun StatusChip(status: PlanStatus, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val chipHeight = 24.dp
    val border = 1.dp

    val base = modifier.height(chipHeight)
    val styled = when (status) {
        // Viền nét đứt, cùng ngôn ngữ với vòng nét đứt
        PlanStatus.PLANNED -> base.drawBehind {
            val w = border.toPx()
            drawRoundRect(
                color = colors.onSurfaceVariant,
                topLeft = Offset(w / 2, w / 2),
                size = Size(size.width - w, size.height - w),
                cornerRadius = CornerRadius(size.height / 2),
                style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(RunRingStroke.dashOn.toPx(), RunRingStroke.dashOff.toPx()))),
            )
        }
        PlanStatus.IN_PROGRESS -> base.border(border, colors.outline, CircleShape)
        PlanStatus.COMPLETED -> base.background(colors.primary, CircleShape)
        PlanStatus.MISSED -> base.border(border, colors.error, CircleShape)
        PlanStatus.CANCELLED -> base.background(colors.surfaceContainerHigh, CircleShape)
    }
    val textColor = when (status) {
        PlanStatus.PLANNED, PlanStatus.IN_PROGRESS -> colors.onSurface
        PlanStatus.COMPLETED -> colors.onPrimary
        PlanStatus.MISSED -> colors.error
        PlanStatus.CANCELLED -> colors.onSurfaceVariant
    }

    Row(
        styled.padding(horizontal = Dimens.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.space1),
    ) {
        if (status == PlanStatus.IN_PROGRESS) {
            Canvas(Modifier.size(6.dp)) { drawCircle(brush = nhipSweep(center)) }
        }
        if (status == PlanStatus.COMPLETED) {
            // Biểu tượng trong chip 20 dp (DESIGN 7)
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
        }
        Text(
            stringResource(statusLabel(status)), style = MaterialTheme.typography.labelSmall, color = textColor,
            maxLines = 1, softWrap = false,
        )
    }
}

fun statusLabel(status: PlanStatus): Int = when (status) {
    PlanStatus.PLANNED -> R.string.status_planned
    PlanStatus.IN_PROGRESS -> R.string.status_in_progress
    PlanStatus.COMPLETED -> R.string.status_completed
    PlanStatus.MISSED -> R.string.status_missed
    PlanStatus.CANCELLED -> R.string.status_cancelled
}

@ThemePreviews
@Composable
private fun StatusChipPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
        PlanStatus.entries.forEach { StatusChip(it) }
    }
}
