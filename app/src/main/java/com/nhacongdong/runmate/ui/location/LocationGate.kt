package com.nhacongdong.runmate.ui.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.nhacongdong.runmate.R

// Hộp thoại khi thiếu quyền vị trí hoặc GPS tắt (DESIGN 10, SPEC 9.2)
enum class LocationDialog { PERMISSION, LOCATION_OFF }

// Trạng thái cổng kiểm tra vị trí: start kiểm tra rồi mới chạy onReady. dialog là hộp thoại đang hiện, nếu có
class LocationGate(
    val start: () -> Unit,
    val dialog: LocationDialog?,
    val dismiss: () -> Unit,
)

// Dùng ở S4 và S5 cho "Bắt đầu chạy" (DESIGN 10). Chưa có quyền thì hệ thống hỏi. Từ chối hoặc GPS tắt thì hiện hộp thoại
@Composable
fun rememberLocationGate(onReady: () -> Unit): LocationGate {
    val context = LocalContext.current
    val currentOnReady by rememberUpdatedState(onReady)
    var dialog by rememberSaveable { mutableStateOf<LocationDialog?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        when {
            granted.values.any { it } && context.isLocationOn() -> currentOnReady()
            granted.values.any { it } -> dialog = LocationDialog.LOCATION_OFF
            else -> dialog = LocationDialog.PERMISSION
        }
    }
    val permissions = remember { arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) }

    return LocationGate(
        start = {
            when {
                !context.hasLocationPermission() -> launcher.launch(permissions)
                !context.isLocationOn() -> dialog = LocationDialog.LOCATION_OFF
                else -> currentOnReady()
            }
        },
        dialog = dialog,
        dismiss = { dialog = null },
    )
}

// Vẽ hộp thoại quyền hoặc bật vị trí. permissionMessage và locationOffMessage là chữ riêng của từng màn hình
@Composable
fun LocationDialogs(
    dialog: LocationDialog?,
    @StringRes permissionMessage: Int,
    @StringRes locationOffMessage: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    when (dialog) {
        LocationDialog.PERMISSION -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.location_permission_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(stringResource(permissionMessage), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss()
                    context.startActivity(appSettingsIntent(context))
                }) { Text(stringResource(R.string.action_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_later)) }
            },
            tonalElevation = 0.dp,
        )
        LocationDialog.LOCATION_OFF -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.location_off_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(stringResource(locationOffMessage), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss()
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text(stringResource(R.string.action_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
            tonalElevation = 0.dp,
        )
        null -> Unit
    }
}

// Chỉ cần một trong hai quyền (SPEC 8.3)
fun Context.hasLocationPermission(): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

// GPS của máy có đang bật không (minSdk 26 nên dùng LocationManagerCompat)
fun Context.isLocationOn(): Boolean =
    LocationManagerCompat.isLocationEnabled(getSystemService(LocationManager::class.java))

// Trang cài đặt quyền của chính app
private fun appSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
