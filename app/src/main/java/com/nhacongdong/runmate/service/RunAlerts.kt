package com.nhacongdong.runmate.service

import android.content.Context
import android.app.NotificationManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.ui.location.hasLocationPermission
import com.nhacongdong.runmate.ui.location.isLocationOn

// Báo khi buổi chạy bị tự tạm dừng lúc mở lại app vì app đã bị dừng (SPEC 10.2a): thông báo và rung một lần
object RunAlerts {
    private const val NOTIFICATION_ID = 2

    fun pausedAfterInterruption(context: Context) {
        val text = context.getString(
            when {
                !context.hasLocationPermission() -> R.string.notification_paused_no_permission
                !context.isLocationOn() -> R.string.notification_paused_location_off
                else -> R.string.notification_paused_app_stopped
            },
        )
        val notification = NotificationCompat.Builder(context, RunTrackingService.CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_directions_run)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        context.getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(400L, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
