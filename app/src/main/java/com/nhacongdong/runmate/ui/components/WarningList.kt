package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.local.WeatherSnapshot
import com.nhacongdong.runmate.domain.WeatherWarning
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import kotlin.math.roundToInt

// Danh sách cảnh báo thời tiết (DESIGN 8.11). Hiện tất cả cảnh báo. Không có cảnh báo thì không hiện gì
@Composable
fun WarningList(
    snapshot: WeatherSnapshot,
    warnings: List<WeatherWarning>,
    modifier: Modifier = Modifier,
) {
    if (warnings.isEmpty()) return
    Column(
        modifier
            .fillMaxWidth()
            .background(RunMateTheme.colors.warningContainer, RoundedCornerShape(Dimens.radiusL))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        warnings.forEach { warning ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                Icon(
                    painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = RunMateTheme.colors.warning,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    warningText(warning, snapshot),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// Chữ kèm số liệu, ví dụ "Có thể mưa (70%)"
@Composable
private fun warningText(warning: WeatherWarning, snapshot: WeatherSnapshot): String = when (warning) {
    WeatherWarning.RAIN -> stringResource(R.string.warning_rain, snapshot.precipitationProb)
    WeatherWarning.HOT -> stringResource(R.string.warning_hot, snapshot.temperatureC.roundToInt())
    WeatherWarning.WINDY -> stringResource(R.string.warning_windy, snapshot.windSpeedKmh.roundToInt())
    WeatherWarning.UV -> stringResource(R.string.warning_uv, snapshot.uvIndex.roundToInt())
}

@Composable
private fun WarningListPreviewScreen(warnings: List<WeatherWarning>) = RunMateTheme {
    WarningList(
        snapshot = WeatherSnapshot(1L, 36.0, 70, 32.0, 9.0),
        warnings = warnings,
        modifier = Modifier.padding(Dimens.space4),
    )
}

@Preview(name = "Một cảnh báo · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Một cảnh báo · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WarningListOnePreview() = WarningListPreviewScreen(listOf(WeatherWarning.RAIN))

@Preview(name = "Bốn cảnh báo · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Bốn cảnh báo · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WarningListAllPreview() = WarningListPreviewScreen(
    listOf(WeatherWarning.RAIN, WeatherWarning.HOT, WeatherWarning.WINDY, WeatherWarning.UV),
)
