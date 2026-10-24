package com.nhacongdong.runmate.spike

// Code chạy thử 3.3b: Foreground Service ghi GPS khi tắt màn hình.
// Chỉ nằm ở nhánh spike/gps, không gộp vào main.

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.nhacongdong.runmate.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

data class GpsSpikeStats(
    val running: Boolean = false,
    val received: Int = 0,
    val kept: Int = 0,
    val filteredAccuracy: Int = 0,
    val filteredJump: Int = 0,
    val maxGapSec: Double = 0.0,
    val lastPoint: String = "",
    val filePath: String = "",
    val batteryAtStart: Int = -1,
    val powerSave: String = "",
)

// Số liệu dùng chung giữa Service và màn hình (chỉ sống khi tiến trình app còn sống)
object GpsSpikeState {
    val stats = MutableStateFlow(GpsSpikeStats())
}

class GpsSpikeService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var logFile: File
    private var lastReceived: Location? = null
    private var lastKept: Location? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach(::onLocation)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) start()
        // Không tự khởi động lại khi bị hệ thống tắt (SPEC 10.1)
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission") // màn hình chỉ cho bấm Bắt đầu khi đã có quyền vị trí
    private fun start() {
        if (GpsSpikeState.stats.value.running) return
        startInForeground()

        val dir = getExternalFilesDir(null) ?: filesDir
        logFile = File(dir, "gps_${System.currentTimeMillis()}.csv")
        logFile.appendText("thoi_gian_ms,vi_do,kinh_do,sai_so_m,ket_qua\n")
        val battery = batteryPercent()
        logEvent("BAT_DAU,pin=$battery%,${powerSaveInfo()}")
        GpsSpikeState.stats.value = GpsSpikeStats(
            running = true,
            filePath = logFile.absolutePath,
            batteryAtStart = battery,
            powerSave = powerSaveInfo(),
        )

        fused = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS).build()
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Chạy thử GPS", NotificationManager.IMPORTANCE_LOW)
        )
        // Bấm vào thông báo thì mở lại app
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("RunMate đang ghi GPS (chạy thử)")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun onLocation(loc: Location) {
        val prev = lastReceived
        lastReceived = loc
        val gapSec = if (prev == null) 0.0 else (loc.time - prev.time) / 1000.0
        val result = when {
            loc.accuracy > MAX_ACCURACY_M -> "loai_sai_so" // BR-30
            isJump(loc) -> "loai_nhay"                      // BR-31
            else -> {
                lastKept = loc
                "giu"
            }
        }
        logFile.appendText("${loc.time},${loc.latitude},${loc.longitude},${loc.accuracy},$result\n")
        GpsSpikeState.stats.update { s ->
            s.copy(
                received = s.received + 1,
                kept = s.kept + if (result == "giu") 1 else 0,
                filteredAccuracy = s.filteredAccuracy + if (result == "loai_sai_so") 1 else 0,
                filteredJump = s.filteredJump + if (result == "loai_nhay") 1 else 0,
                maxGapSec = max(s.maxGapSec, gapSec),
                lastPoint = "${formatTime(loc.time)} · sai số ${loc.accuracy.roundToInt()} m",
            )
        }
    }

    // Tốc độ từ điểm giữ trước tới điểm này vượt 12 m/s thì coi là nhảy bất thường
    private fun isJump(loc: Location): Boolean {
        val prev = lastKept ?: return false
        val dtSec = (loc.time - prev.time) / 1000.0
        if (dtSec <= 0) return false
        return prev.distanceTo(loc) / dtSec > MAX_SPEED_MPS
    }

    // Được gọi khi người dùng vuốt app khỏi danh sách đa nhiệm
    override fun onTaskRemoved(rootIntent: Intent?) {
        logEvent("VUOT_APP_KHOI_DA_NHIEM")
        super.onTaskRemoved(rootIntent)
    }

    // Không được gọi khi bị buộc dừng, nên file sẽ thiếu dòng KET_THUC
    override fun onDestroy() {
        if (::fused.isInitialized) fused.removeLocationUpdates(callback)
        if (::logFile.isInitialized) {
            val s = GpsSpikeState.stats.value
            logEvent(
                "KET_THUC,pin=${batteryPercent()}%,${powerSaveInfo()},nhan=${s.received},giu=${s.kept}," +
                    "khoang_trong_lon_nhat=${s.maxGapSec}s"
            )
        }
        GpsSpikeState.stats.update { it.copy(running = false) }
        super.onDestroy()
    }

    private fun logEvent(event: String) {
        logFile.appendText("${System.currentTimeMillis()},SU_KIEN,$event\n")
    }

    // Tiết kiệm pin có bật không, và khi bật thì vị trí bị giới hạn kiểu nào
    private fun powerSaveInfo(): String {
        val pm = getSystemService(PowerManager::class.java)
        val locationMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            when (val mode = pm.locationPowerSaveMode) {
                PowerManager.LOCATION_MODE_NO_CHANGE -> "NO_CHANGE"
                PowerManager.LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF -> "GPS_DISABLED_WHEN_SCREEN_OFF"
                PowerManager.LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF -> "ALL_DISABLED_WHEN_SCREEN_OFF"
                PowerManager.LOCATION_MODE_FOREGROUND_ONLY -> "FOREGROUND_ONLY"
                PowerManager.LOCATION_MODE_THROTTLE_REQUESTS_WHEN_SCREEN_OFF -> "THROTTLE_REQUESTS_WHEN_SCREEN_OFF"
                else -> "KHAC($mode)"
            }
        } else {
            "KHONG_HO_TRO"
        }
        return "tiet_kiem_pin=${pm.isPowerSaveMode},che_do_vi_tri=$locationMode"
    }

    private fun batteryPercent(): Int =
        getSystemService(BatteryManager::class.java)
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    private fun formatTime(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
            .truncatedTo(ChronoUnit.SECONDS).toString()

    companion object {
        const val ACTION_START = "com.nhacongdong.runmate.spike.START"
        private const val CHANNEL_ID = "gps_spike"
        private const val NOTIFICATION_ID = 1
        private const val INTERVAL_MS = 2_000L     // BR-32
        private const val MAX_ACCURACY_M = 30f     // BR-30
        private const val MAX_SPEED_MPS = 12f      // BR-31
    }
}
