package com.nhacongdong.runmate.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

// Gửi lệnh tới RunTrackingService: bắt đầu, tạm dừng, tiếp tục, kết thúc (SPEC 10.2).
// Màn hình và ViewModel gọi các hàm này, không tự tạo Intent
object RunServiceControl {

    // Bắt đầu service ở chế độ foreground, đưa mã buổi chạy vào để ghi điểm đúng buổi
    fun start(context: Context, sessionId: Long) {
        val intent = intent(context, RunTrackingService.ACTION_START)
            .putExtra(RunTrackingService.EXTRA_SESSION_ID, sessionId)
        ContextCompat.startForegroundService(context, intent)
    }

    fun pause(context: Context) = context.startService(intent(context, RunTrackingService.ACTION_PAUSE))

    // Gửi mã buổi chạy kèm lệnh tiếp tục, vì service có thể vừa được bật lại và chưa nhớ buổi nào
    fun resume(context: Context, sessionId: Long) = context.startService(
        intent(context, RunTrackingService.ACTION_RESUME)
            .putExtra(RunTrackingService.EXTRA_SESSION_ID, sessionId),
    )

    fun stop(context: Context) = context.startService(intent(context, RunTrackingService.ACTION_STOP))

    private fun intent(context: Context, action: String) =
        Intent(context, RunTrackingService::class.java).setAction(action)
}
