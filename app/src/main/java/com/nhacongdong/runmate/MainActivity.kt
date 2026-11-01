package com.nhacongdong.runmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.nhacongdong.runmate.service.RunTrackingService
import com.nhacongdong.runmate.ui.RunMateApp
import com.nhacongdong.runmate.ui.theme.RunMateTheme

class MainActivity : ComponentActivity() {

    // Bấm vào thông báo của service thì mở S6 (SPEC 8.3, DESIGN 9 S6)
    private val openRunRequest = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openRunRequest.value = wantsRun(intent)
        setContent {
            RunMateTheme {
                RunMateApp(
                    openRun = openRunRequest.value,
                    onOpenRunHandled = {
                        // Xoá cờ để xoay màn hình hoặc tạo lại Activity không mở S6 lần nữa
                        intent?.removeExtra(RunTrackingService.EXTRA_OPEN_RUN)
                        openRunRequest.value = false
                    },
                )
            }
        }
    }

    // App đang mở mà bấm thông báo thì Activity nhận intent mới qua đây (launchMode singleTop)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (wantsRun(intent)) openRunRequest.value = true
    }

    private fun wantsRun(intent: Intent?): Boolean =
        intent?.getBooleanExtra(RunTrackingService.EXTRA_OPEN_RUN, false) == true
}
