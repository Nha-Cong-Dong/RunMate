package com.nhacongdong.runmate.ui.run

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.ui.components.PreviewZone
import com.nhacongdong.runmate.ui.components.previewPlan
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import com.nhacongdong.runmate.ui.theme.RunRingStroke
import java.time.ZoneId

// Thẻ gợi ý chạy theo kế hoạch trên S5 (BR-44, DESIGN 9 S5). Viền nét đứt 1,5 dp textSecondary, nền surface.
// Chạy theo kế hoạch thì mở S4 của kế hoạch đó. Bỏ qua thì ở lại S5
@Composable
fun SuggestionCard(
    plan: RunPlan,
    onRunPlan: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val dash = PathEffect.dashPathEffect(
        floatArrayOf(with(density) { RunRingStroke.dashOn.toPx() }, with(density) { RunRingStroke.dashOff.toPx() }),
    )
    val strokeWidth = with(density) { RunRingStroke.outline.toPx() }
    val cornerRadius = with(density) { Dimens.radiusL.toPx() }

    Column(
        modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(Dimens.radiusL))
            .drawDashedBorder(colors.onSurfaceVariant, strokeWidth, dash, cornerRadius)
            .padding(Dimens.space4),
        verticalArrangement = Arrangement.spacedBy(Dimens.space3),
    ) {
        Text(
            stringResource(
                R.string.suggestion_text,
                RunFormat.formatTime(plan.startTime, zone),
                RunFormat.formatGoal(plan.goalType, plan.goalValue),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
            Button(
                onClick = onRunPlan,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
            ) {
                Text(stringResource(R.string.suggestion_run_plan), style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.suggestion_dismiss), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// Vẽ viền nét đứt bằng drawBehind, không dùng Modifier.border vì border không hỗ trợ nét đứt
private fun Modifier.drawDashedBorder(
    color: androidx.compose.ui.graphics.Color,
    strokeWidth: Float,
    pathEffect: PathEffect,
    cornerRadius: Float,
): Modifier = this.then(
    Modifier.drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = strokeWidth, pathEffect = pathEffect),
        )
    }
)

@Preview(name = "Gợi ý · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Gợi ý · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SuggestionCardPreview() = RunMateTheme {
    SuggestionCard(
        plan = previewPlan(com.nhacongdong.runmate.data.model.PlanStatus.PLANNED, hour = 6, goalType = GoalType.DISTANCE, goalValue = 5_000),
        onRunPlan = {},
        onDismiss = {},
        zone = PreviewZone,
        modifier = Modifier.padding(Dimens.space4),
    )
}
