package com.nhacongdong.runmate.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.database.SQLException
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.MainActivity
import com.nhacongdong.runmate.RunMateApplication
import com.nhacongdong.runmate.domain.RecordTrackPoint
import com.nhacongdong.runmate.ui.location.hasLocationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Service ghi GPS khi đang chạy (SPEC 10.2). Nhận lệnh bắt đầu, tạm dừng, tiếp tục qua intent.
// Mỗi điểm GPS đi qua RecordTrackPoint (BR-30, BR-31, BR-39) rồi mới vào database
class RunTrackingService : Service() {

    private lateinit var locationSource: TrackingLocationSource
    private lateinit var recordTrackPoint: RecordTrackPoint
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sessionId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createChannel()
        locationSource = TrackingLocationSource(this)
        recordTrackPoint = (application as RunMateApplication).container.recordTrackPoint
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hasPermission = hasLocationPermission()
        // Phải gọi startForeground trong vòng 5 giây sau khi service được khởi động
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(if (hasPermission) R.string.notification_recording else R.string.notification_no_permission),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        when (intent?.action) {
            ACTION_START -> {
                sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
                startLocation(hasPermission)
            }
            ACTION_PAUSE -> locationSource.stop()
            ACTION_RESUME -> startLocation(hasPermission)
            // Kết thúc buổi chạy: ngừng nhận GPS và dừng service (SPEC 6.3)
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    // Dừng nhận GPS và dừng service. Dùng khi kết thúc, hoặc khi buổi chạy không còn để ghi
    private fun stopRecording() {
        locationSource.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // Có quyền vị trí thì nhận GPS. Thiếu quyền thì không ghi quãng đường, service vẫn chạy để thời gian tiếp tục (SPEC 10.2a)
    private fun startLocation(hasPermission: Boolean) {
        if (!hasPermission) {
            updateNotification(R.string.notification_no_permission)
            return
        }
        try {
            locationSource.start { location ->
                scope.launch {
                    try {
                        recordTrackPoint(
                            sessionId = sessionId,
                            timestamp = location.time,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyM = location.accuracy,
                        )
                    } catch (e: SQLException) {
                        // Buổi chạy đã bị xoá hoặc không còn (lỗi FOREIGN KEY): không còn gì để ghi, nên dừng GPS và service
                        // thay vì để lỗi làm app đóng
                        stopRecording()
                    }
                }
            }
            updateNotification(R.string.notification_recording)
        } catch (e: SecurityException) {
            updateNotification(R.string.notification_no_permission)
        }
    }

    // Đổi chữ thông báo khi trạng thái quyền thay đổi
    private fun updateNotification(textRes: Int) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(textRes))
    }

    override fun onDestroy() {
        isRunning = false
        locationSource.stop()
        scope.cancel()
        super.onDestroy()
    }

    // Kênh thông báo hiện trong Cài đặt của điện thoại, người dùng có thể chỉnh ở đó
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(textRes: Int) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(getString(textRes))
        .setSmallIcon(R.drawable.ic_directions_run)
        .setOngoing(true)
        .setContentIntent(openRunPendingIntent())
        .build()

    // Bấm thông báo thì mở RunMate và sang S6 (SPEC 8.3)
    private fun openRunPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_RUN, true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        // Service đang chạy trong tiến trình này. Mở app mà cờ là false thì buổi dở đã mất service (SPEC 10.1)
        @Volatile
        var isRunning: Boolean = false
            private set

        const val CHANNEL_ID = "run_tracking"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.nhacongdong.runmate.action.START"
        const val ACTION_STOP = "com.nhacongdong.runmate.action.STOP"
        const val ACTION_PAUSE = "com.nhacongdong.runmate.action.PAUSE"
        const val ACTION_RESUME = "com.nhacongdong.runmate.action.RESUME"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_OPEN_RUN = "open_run"
    }
}
