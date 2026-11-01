package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.NhipDiagonal
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import com.nhacongdong.runmate.ui.theme.tabular

// Băng "Đang có buổi chạy" dưới lịch tuần ở S1 (DESIGN 8.4). Bấm thì mở S6
@Composable
fun ActiveRunBanner(
    status: SessionStatus,
    distanceM: Double,
    durationMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recording = status == SessionStatus.RECORDING
    val reduced = rememberReducedMotion()
    // Chấm gradient nhấp nháy khi đang ghi, tắt khi người dùng tắt hoạt ảnh (DESIGN 11)
    val blink by rememberInfiniteTransition(label = "activeRunBlink").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1_000), RepeatMode.Reverse),
        label = "activeRunBlink",
    )
    val text = stringResource(
        if (recording) R.string.active_run_recording else R.string.active_run_paused,
        RunFormat.formatDistanceKm(distanceM),
        RunFormat.formatDuration(durationMs / 1_000),
    )

    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.inverseSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .alpha(if (recording && !reduced) blink else 1f)
                .clip(CircleShape)
                .background(if (recording) NhipDiagonal else SolidColor(MaterialTheme.colorScheme.onSurfaceVariant)),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.tabular(),
            color = MaterialTheme.colorScheme.inverseOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = Dimens.space3),
        )
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.inverseOnSurface,
        )
    }
}

@Preview(name = "Đang ghi · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Đang ghi · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActiveRunBannerRecordingPreview() = RunMateTheme {
    ActiveRunBanner(SessionStatus.RECORDING, 3_200.0, 1_120_000L, onClick = {}, modifier = Modifier.padding(Dimens.space4))
}

@Preview(name = "Tạm dừng · Sáng", widthDp = 360, showBackground = true)
@Composable
private fun ActiveRunBannerPausedPreview() = RunMateTheme {
    ActiveRunBanner(SessionStatus.PAUSED, 3_200.0, 1_120_000L, onClick = {}, modifier = Modifier.padding(Dimens.space4))
}
