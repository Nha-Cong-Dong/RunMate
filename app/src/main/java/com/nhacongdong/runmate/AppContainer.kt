package com.nhacongdong.runmate

import android.content.Context

// Nơi duy nhất tạo các phụ thuộc dùng chung (SPEC 8.4: tự tạo thủ công).
// Database, API và Repository sẽ được thêm vào đây ở các giai đoạn A–C.
class AppContainer(private val context: Context)
