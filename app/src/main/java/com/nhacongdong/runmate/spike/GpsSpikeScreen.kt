package com.nhacongdong.runmate.spike

// Màn hình chạy thử 3.3b: xin quyền, bắt đầu và dừng ghi GPS, xem số liệu.
// Chỉ nằm ở nhánh spike/gps, không gộp vào main.

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private fun requiredPermissions(): Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

private fun isGranted(context: Context, permission: String) =
    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private fun permissionSummary(context: Context): String =
    requiredPermissions().joinToString("\n") { p ->
        "${p.substringAfterLast('.')}: ${if (isGranted(context, p)) "đã cấp" else "CHƯA cấp"}"
    }

@Composable
fun GpsSpikeScreen() {
    val context = LocalContext.current
    val stats by GpsSpikeState.stats.collectAsState()
    var permissionText by remember { mutableStateOf(permissionSummary(context)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionText = permissionSummary(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(permissionText)

        Button(onClick = { launcher.launch(requiredPermissions()) }) {
            Text("Xin quyền vị trí và thông báo")
        }

        Button(
            enabled = !stats.running && isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION),
            onClick = {
                context.startForegroundService(
                    Intent(context, GpsSpikeService::class.java).setAction(GpsSpikeService.ACTION_START)
                )
            },
        ) { Text("Bắt đầu ghi GPS") }

        Button(
            enabled = stats.running,
            onClick = { context.stopService(Intent(context, GpsSpikeService::class.java)) },
        ) { Text("Dừng") }

        Text(
            """
            Đang ghi: ${if (stats.running) "Có" else "Không"}
            Điểm nhận: ${stats.received}
            Điểm giữ: ${stats.kept}
            Bị lọc vì sai số > 30 m (BR-30): ${stats.filteredAccuracy}
            Bị lọc vì nhảy > 12 m/s (BR-31): ${stats.filteredJump}
            Khoảng trống lớn nhất giữa 2 điểm: ${"%.1f".format(stats.maxGapSec)} giây
            Điểm cuối: ${stats.lastPoint}
            Pin lúc bắt đầu: ${stats.batteryAtStart}%
            Tiết kiệm pin lúc bắt đầu: ${stats.powerSave}
            File log: ${stats.filePath}
            """.trimIndent()
        )
    }
}
