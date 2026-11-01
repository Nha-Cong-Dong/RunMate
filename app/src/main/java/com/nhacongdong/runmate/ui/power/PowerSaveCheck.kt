package com.nhacongdong.runmate.ui.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.ui.components.DialogCard
import com.nhacongdong.runmate.ui.components.PreviewBox
import com.nhacongdong.runmate.ui.components.ThemePreviews
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme

// Tiết kiệm pin có làm GPS tắt khi khoá màn hình không (BR-61).
// Android 9 trở lên xem chế độ vị trí khi tiết kiệm pin. Android 8.x chỉ cần tiết kiệm pin đang bật
fun Context.isPowerSaveWarningOn(): Boolean {
    val power = getSystemService(PowerManager::class.java) ?: return false
    if (!power.isPowerSaveMode) return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        power.locationPowerSaveMode != PowerManager.LOCATION_MODE_NO_CHANGE
    } else {
        true
    }
}

// Hộp thoại trước khi bắt đầu chạy ở S4 và S5 (DESIGN 10): "Vẫn chạy · Mở cài đặt", nút đậm ở bên phải
@Composable
fun PowerSaveDialog(show: Boolean, onContinue: () -> Unit, onDismiss: () -> Unit) {
    if (!show) return
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        PowerSaveCard(
            onOpenSettings = {
                onDismiss()
                context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
            },
            onContinue = onContinue,
        )
    }
}

// Nội dung hộp thoại tiết kiệm pin, tách riêng để xem trước được
@Composable
fun PowerSaveCard(onOpenSettings: () -> Unit, onContinue: () -> Unit) {
    DialogCard(
        title = stringResource(R.string.power_save_title),
        text = stringResource(R.string.power_save_message),
        confirm = { TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.action_open_settings)) } },
        dismiss = { TextButton(onClick = onContinue) { Text(stringResource(R.string.action_run_anyway)) } },
    )
}

// Theo dõi tiết kiệm pin khi đang ghi (BR-61, DESIGN 9 S6): cập nhật khi người dùng bật hoặc tắt.
// Hệ thống phát ACTION_POWER_SAVE_MODE_CHANGED khi tiết kiệm pin đổi
@Composable
fun rememberPowerSaveWarningOn(): Boolean {
    val context = LocalContext.current
    var on by remember { mutableStateOf(context.isPowerSaveWarningOn()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                on = receiverContext.isPowerSaveWarningOn()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return on
}

// Băng cảnh báo ở S6 khi tiết kiệm pin bật giữa chừng (DESIGN 9 S6): nền warningContainer, có nút đóng
@Composable
fun PowerSaveBanner(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusM))
            .background(RunMateTheme.colors.warningContainer)
            .padding(start = Dimens.space4, end = Dimens.space2, top = Dimens.space2, bottom = Dimens.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = RunMateTheme.colors.warning,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Dimens.space2))
        Text(
            stringResource(R.string.power_save_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClose) {
            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.cd_close))
        }
    }
}

@ThemePreviews
@Composable
private fun PowerSaveBannerPreview() = PreviewBox {
    PowerSaveBanner(onClose = {}, modifier = Modifier.padding(Dimens.space4))
}

@ThemePreviews
@Composable
private fun PowerSaveCardPreview() = PreviewBox {
    PowerSaveCard(onOpenSettings = {}, onContinue = {})
}
