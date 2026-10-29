package com.nhacongdong.runmate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.tabular
import java.time.ZoneId

// Thẻ kế hoạch chưa chạy: PLANNED, IN_PROGRESS, MISSED, CANCELLED (DESIGN 8.5). Bấm cả thẻ mở S4
@Composable
fun PlanCard(
    plan: RunPlan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (plan.status == PlanStatus.CANCELLED) 0.6f else 1f),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
    ) {
        Row(
            Modifier.heightIn(min = 72.dp).padding(Dimens.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                RunFormat.formatTime(plan.startTime, zone),
                style = type.headlineSmall.tabular(),
                color = colors.onSurface,
                maxLines = 1,
                softWrap = false,
                // Tối thiểu 56 dp, nới ra nếu cỡ chữ lớn hơn, không bao giờ ngắt dòng giờ (DESIGN 8.5, 14)
                modifier = Modifier.widthIn(min = 56.dp),
            )
            Column(Modifier.weight(1f).padding(horizontal = Dimens.space2)) {
                Text(
                    plan.locationName, style = type.titleMedium, color = colors.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.plan_goal, RunFormat.formatGoal(plan.goalType, plan.goalValue)),
                    style = type.bodyMedium, color = colors.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(plan.status)
        }
    }
}

@ThemePreviews
@Composable
private fun PlanCardPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        PlanCard(previewPlan(PlanStatus.PLANNED, hour = 19), onClick = {}, zone = PreviewZone)
        PlanCard(previewPlan(PlanStatus.IN_PROGRESS, hour = 6), onClick = {}, zone = PreviewZone)
        PlanCard(
            previewPlan(PlanStatus.MISSED, hour = 5, goalType = GoalType.DURATION, goalValue = 2_700),
            onClick = {}, zone = PreviewZone,
        )
        PlanCard(previewPlan(PlanStatus.CANCELLED, hour = 18), onClick = {}, zone = PreviewZone)
        PlanCard(
            previewPlan(PlanStatus.PLANNED, hour = 17, location = "Công viên Thống Nhất, Hai Bà Trưng, Hà Nội"),
            onClick = {}, zone = PreviewZone,
        )
    }
}
