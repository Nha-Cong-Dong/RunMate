package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.NhipBlue
import com.nhacongdong.runmate.ui.theme.NhipCyan
import com.nhacongdong.runmate.ui.theme.NhipLime
import com.nhacongdong.runmate.ui.theme.NhipOrange
import com.nhacongdong.runmate.ui.theme.NhipPink
import com.nhacongdong.runmate.ui.theme.NhipYellow
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import com.nhacongdong.runmate.ui.theme.tabular

// Thanh hoàn thành kế hoạch ở màn hình S8 (DESIGN 9 S8).
// Thanh ngang cao 8 dp bo tròn, nền ringTrack, phần đã đạt tô theo dải gradient nhipLinear.
// rate = null nghĩa là kỳ này không có kế hoạch nào -> nhãn "--" và dòng chữ "Chưa có kế hoạch trong kỳ này".
@Composable
fun PlanCompletionBar(
    rate: Int?,
    completed: Int,
    missed: Int,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val targetFraction = ((rate ?: 0).coerceIn(0, 100) / 100f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 400),
        label = "planCompletionProgress",
    )

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(NhipBlue, NhipCyan, NhipLime, NhipYellow, NhipOrange, NhipPink)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        // Hàng trên: Tiêu đề và nhãn tỷ lệ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.stats_plan_completion),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (rate != null) "$rate%" else "--",
                style = MaterialTheme.typography.labelLarge.tabular(),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Thanh tiến độ cao 8 dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(RunMateTheme.colors.ringTrack)
        ) {
            if (rate != null && animatedFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(gradientBrush)
                )
            }
        }

        // Hàng dưới: Chú thích số kế hoạch hoặc chưa có kế hoạch
        Text(
            text = if (rate != null) {
                stringResource(R.string.stats_completion_summary, completed, missed)
            } else {
                stringResource(R.string.stats_no_plans)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "Có kế hoạch · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Có kế hoạch · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlanCompletionBarPreview() = RunMateTheme {
    PlanCompletionBar(
        rate = 80,
        completed = 4,
        missed = 1,
        modifier = Modifier.padding(16.dp),
    )
}

@Preview(name = "Kỳ trống · Sáng", widthDp = 360, showBackground = true)
@Composable
private fun PlanCompletionBarEmptyPreview() = RunMateTheme {
    PlanCompletionBar(
        rate = null,
        completed = 0,
        missed = 0,
        modifier = Modifier.padding(16.dp),
    )
}
