package com.nhacongdong.runmate.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase

// Database runmate.db gồm 3 bảng (SPEC 7). Đổi cấu trúc bảng thì tăng version và viết Migration.
@Database(entities = [RunPlan::class, RunSession::class, TrackPoint::class], version = 1)
abstract class RunMateDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
    abstract fun sessionDao(): SessionDao
    abstract fun trackPointDao(): TrackPointDao
}
