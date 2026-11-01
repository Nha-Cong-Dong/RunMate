package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.NhipDiagonal
import com.nhacongdong.runmate.ui.theme.RunMateTheme

// Biểu tượng trên nút Tiếp tục đặt trên nền gradient, cố định màu #111111 để đủ tương phản (DESIGN 9 S6, mục 12)
private val OnGradient = Color(0xFF111111)

// Nhóm nút điều khiển S6 (DESIGN 9 S6). Đang ghi: Tạm dừng ở giữa, Kết thúc bên trái.
// Đang tạm dừng: nút giữa đổi thành Tiếp tục, Kết thúc giữ nguyên
@Composable
fun RunControls(
    status: SessionStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        RunButton(
            label = stringResource(R.string.run_finish),
            size = 64.dp,
            icon = R.drawable.ic_stop,
            onClick = onFinish,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        if (status == SessionStatus.PAUSED) {
            CenterRunButton(
                label = stringResource(R.string.run_resume),
                icon = R.drawable.ic_play_arrow,
                onClick = onResume,
                filledGradient = true,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            CenterRunButton(
                label = stringResource(R.string.run_pause),
                icon = R.drawable.ic_pause,
                onClick = onPause,
                filledGradient = false,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

// Nút giữa 96 dp. Tạm dừng: vòng gradient 4 dp, nền surface. Tiếp tục: nền gradient đầy
@Composable
private fun CenterRunButton(
    label: String,
    icon: Int,
    onClick: () -> Unit,
    filledGradient: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.size(width = 96.dp, height = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.space1),
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(NhipDiagonal)
                .clickable(onClick = onClick)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (filledGradient) Color.Transparent else MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = if (filledGradient) OnGradient else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp),
            )
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// Nút Kết thúc 64 dp, nền surfaceContainerHigh
@Composable
private fun RunButton(
    label: String,
    size: androidx.compose.ui.unit.Dp,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.space1)) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(name = "Đang ghi · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Đang ghi · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RunControlsRecordingPreview() = RunMateTheme {
    RunControls(status = SessionStatus.RECORDING, onPause = {}, onResume = {}, onFinish = {}, modifier = Modifier.padding(Dimens.space4))
}

@Preview(name = "Tạm dừng · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Tạm dừng · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RunControlsPausedPreview() = RunMateTheme {
    RunControls(status = SessionStatus.PAUSED, onPause = {}, onResume = {}, onFinish = {}, modifier = Modifier.padding(Dimens.space4))
}
