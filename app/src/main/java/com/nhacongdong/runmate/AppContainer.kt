package com.nhacongdong.runmate

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.SessionRepository
import com.nhacongdong.runmate.data.local.RunMateDatabase
import com.nhacongdong.runmate.domain.RefreshMissedPlans

// Nơi duy nhất tạo các phụ thuộc dùng chung (SPEC 8.4: tự tạo thủ công).
// Database, API và Repository sẽ được thêm vào đây ở các giai đoạn A–C.
class AppContainer(private val context: Context) {

    // Chỉ mở database khi có màn hình cần tới (SPEC 7, 8.4)
    val database: RunMateDatabase by lazy {
        Room.databaseBuilder<RunMateDatabase>(context, "runmate.db")
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    val planRepository by lazy { PlanRepository(database.planDao()) }

    val sessionRepository by lazy {
        SessionRepository(database, database.sessionDao(), database.trackPointDao(), database.planDao())
    }

    val refreshMissedPlans by lazy { RefreshMissedPlans(planRepository) }
}
