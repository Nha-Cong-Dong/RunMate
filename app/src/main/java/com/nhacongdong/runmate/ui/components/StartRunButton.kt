package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.NhipDiagonal
import com.nhacongdong.runmate.ui.theme.RunMateTheme

// Nút Bắt đầu chạy (DESIGN 8.15). Viền gradient 2 dp, nền inverse. Khi tắt thì có dòng giải thích bên dưới
@Composable
fun StartRunButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    disabledReason: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(if (enabled) NhipDiagonal else SolidColor(colors.outlineVariant))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(2.dp)
                .clip(CircleShape)
                .background(if (enabled) colors.primary else colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(
                        painterResource(icon),
                        contentDescription = null,
                        tint = if (enabled) colors.onPrimary else RunMateTheme.colors.textDisabled,
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) colors.onPrimary else RunMateTheme.colors.textDisabled,
                    maxLines = 1,
                )
            }
        }
        if (!enabled && disabledReason != null) {
            Text(disabledReason, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

@Preview(name = "Bật · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Bật · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StartRunButtonEnabledPreview() = RunMateTheme {
    StartRunButton(
        label = stringResource(R.string.action_start_run),
        enabled = true,
        onClick = {},
        icon = R.drawable.ic_play_arrow,
        modifier = Modifier.padding(Dimens.space4),
    )
}

@Preview(name = "Tắt · Sáng", widthDp = 360, showBackground = true)
@Composable
private fun StartRunButtonDisabledPreview() = RunMateTheme {
    StartRunButton(
        label = stringResource(R.string.action_start_run),
        enabled = false,
        onClick = {},
        icon = R.drawable.ic_play_arrow,
        disabledReason = stringResource(R.string.blocked_edit_completed),
        modifier = Modifier.padding(Dimens.space4),
    )
}
