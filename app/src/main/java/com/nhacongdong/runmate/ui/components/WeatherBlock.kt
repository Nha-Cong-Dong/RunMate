package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.local.WeatherSnapshot
import com.nhacongdong.runmate.domain.WeatherRules
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import java.time.ZoneId
import kotlin.math.roundToInt

// Khối thời tiết (DESIGN 8.10): lưới 4 ô, dòng dự báo và cập nhật, nút Lấy lại khi cần.
// Chỉ hiển thị. Việc lấy dữ liệu do ViewModel làm
@Composable
fun WeatherBlock(
    snapshot: WeatherSnapshot?,
    forecastHour: Long,
    zone: ZoneId,
    isRefreshing: Boolean,
    offline: Boolean,
    showRefresh: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.surfaceContainer, RoundedCornerShape(Dimens.radiusL))
            .padding(Dimens.space4),
        verticalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        when {
            isRefreshing -> Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                repeat(4) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(colors.surfaceContainerHigh, RoundedCornerShape(Dimens.radiusM)),
                    )
                }
            }
            snapshot != null -> Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                WeatherTile(
                    icon = R.drawable.ic_thermostat,
                    value = "${snapshot.temperatureC.roundToInt()}°C",
                    label = stringResource(R.string.weather_label_temperature),
                    warn = snapshot.temperatureC >= WeatherRules.HOT_CELSIUS,
                    modifier = Modifier.weight(1f),
                )
                WeatherTile(
                    icon = R.drawable.ic_rainy,
                    value = "${snapshot.precipitationProb}%",
                    label = stringResource(R.string.weather_label_rain),
                    warn = snapshot.precipitationProb >= WeatherRules.RAIN_PERCENT,
                    modifier = Modifier.weight(1f),
                )
                WeatherTile(
                    icon = R.drawable.ic_air,
                    value = "${snapshot.windSpeedKmh.roundToInt()}",
                    label = stringResource(R.string.weather_label_wind),
                    warn = snapshot.windSpeedKmh >= WeatherRules.WIND_KMH,
                    modifier = Modifier.weight(1f),
                )
                WeatherTile(
                    icon = R.drawable.ic_wb_sunny,
                    value = "${snapshot.uvIndex.roundToInt()}",
                    label = stringResource(R.string.weather_label_uv),
                    warn = snapshot.uvIndex >= WeatherRules.UV_INDEX,
                    modifier = Modifier.weight(1f),
                )
            }
            else -> Text(
                stringResource(R.string.weather_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }

        if (snapshot != null) {
            Text(
                stringResource(
                    R.string.weather_forecast_for,
                    RunFormat.formatTime(forecastHour, zone),
                    RunFormat.formatTime(snapshot.weatherFetchedAt, zone),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }
        if (offline) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.space1)) {
                Icon(
                    painterResource(R.drawable.ic_cloud_off),
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(R.string.weather_offline),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (showRefresh && !isRefreshing) {
            // Nút viền (DESIGN 8.10): viền outlineStrong, chữ textPrimary, bo tròn, cao 48 dp
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, colors.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
            ) {
                Text(stringResource(R.string.weather_refresh), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// Một ô: biểu tượng 20 dp, số headlineSmall, nhãn labelMedium. Vượt ngưỡng thì đổi màu warning và có chấm nhỏ
@Composable
private fun WeatherTile(
    icon: Int,
    value: String,
    label: String,
    warn: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val tint = if (warn) RunMateTheme.colors.warning else colors.onSurfaceVariant
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.space1)) {
        Box(Modifier.size(20.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            if (warn) {
                Box(
                    Modifier
                        .size(6.dp)
                        .align(Alignment.TopEnd)
                        .background(RunMateTheme.colors.warning, CircleShape),
                )
            }
        }
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.merge(TextStyle(fontFeatureSettings = "tnum")),
            color = if (warn) RunMateTheme.colors.warning else colors.onSurface,
            maxLines = 1,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 1)
    }
}

private val PreviewSnapshot = WeatherSnapshot(
    weatherFetchedAt = 1_793_400_000_000L, temperatureC = 36.0, precipitationProb = 70,
    windSpeedKmh = 12.0, uvIndex = 9.0,
)

@Composable
private fun WeatherBlockPreviewScreen(
    snapshot: WeatherSnapshot?,
    isRefreshing: Boolean = false,
    offline: Boolean = false,
    showRefresh: Boolean = false,
) = RunMateTheme {
    WeatherBlock(
        snapshot = snapshot,
        forecastHour = 1_793_404_800_000L,
        zone = PreviewZone,
        isRefreshing = isRefreshing,
        offline = offline,
        showRefresh = showRefresh,
        onRefresh = {},
        modifier = Modifier.padding(Dimens.space4),
    )
}

@Preview(name = "Có dữ liệu · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Có dữ liệu · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WeatherBlockDataPreview() = WeatherBlockPreviewScreen(snapshot = PreviewSnapshot)

@Preview(name = "Quá 3 giờ, có nút · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Quá 3 giờ, có nút · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WeatherBlockRefreshPreview() = WeatherBlockPreviewScreen(snapshot = PreviewSnapshot, showRefresh = true)

@Preview(name = "Không có mạng · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Không có mạng · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WeatherBlockOfflinePreview() =
    WeatherBlockPreviewScreen(snapshot = PreviewSnapshot, offline = true, showRefresh = true)

@Preview(name = "Chưa có dữ liệu · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Chưa có dữ liệu · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WeatherBlockEmptyPreview() = WeatherBlockPreviewScreen(snapshot = null, showRefresh = true)

@Preview(name = "Đang tải · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Đang tải · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WeatherBlockLoadingPreview() = WeatherBlockPreviewScreen(snapshot = null, isRefreshing = true)
