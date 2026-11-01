package com.nhacongdong.runmate.ui.notification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.ui.components.DialogCard
import com.nhacongdong.runmate.ui.components.PreviewBox
import com.nhacongdong.runmate.ui.components.ThemePreviews

// Quyền thông báo (SPEC 8.3, từ Android 13). Thiếu quyền thì service vẫn ghi GPS, nhưng thông báo của nó bị ẩn.
// denied = true khi người dùng từ chối, để màn hình hỏi lại bằng hộp thoại
class NotificationGate(
    val start: () -> Unit,
    val denied: Boolean,
    val dismiss: () -> Unit,
)

// Dùng ở S4 và S5 trước khi bắt đầu chạy. Đã có quyền hoặc Android 12 trở xuống thì chạy onReady ngay
@Composable
fun rememberNotificationGate(onReady: () -> Unit): NotificationGate {
    val context = LocalContext.current
    val currentOnReady by rememberUpdatedState(onReady)
    var denied by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) currentOnReady() else denied = true
    }

    return NotificationGate(
        start = {
            if (context.hasNotificationPermission()) currentOnReady() else launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        denied = denied,
        dismiss = { denied = false },
    )
}

// Hộp thoại khi thông báo đang tắt (SPEC 9.2): mở cài đặt, hoặc chạy tiếp mà không có thông báo
@Composable
fun NotificationDeniedDialog(show: Boolean, onContinue: () -> Unit, onDismiss: () -> Unit) {
    if (!show) return
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        NotificationDeniedCard(
            onOpenSettings = {
                onDismiss()
                context.startActivity(appNotificationSettings(context))
            },
            onContinue = onContinue,
        )
    }
}

// Nội dung hộp thoại thông báo tắt, tách riêng để xem trước được
@Composable
fun NotificationDeniedCard(onOpenSettings: () -> Unit, onContinue: () -> Unit) {
    DialogCard(
        title = stringResource(R.string.notification_denied_title),
        text = stringResource(R.string.notification_denied_message),
        confirm = { TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.action_open_settings)) } },
        dismiss = { TextButton(onClick = onContinue) { Text(stringResource(R.string.action_run_anyway)) } },
    )
}

// Từ Android 13 cần quyền thông báo. Bản cũ hơn luôn có
private fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

// Trang cài đặt thông báo của chính app
private fun appNotificationSettings(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

@ThemePreviews
@Composable
private fun NotificationDeniedCardPreview() = PreviewBox {
    NotificationDeniedCard(onOpenSettings = {}, onContinue = {})
}
