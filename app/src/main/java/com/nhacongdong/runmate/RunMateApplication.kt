package com.nhacongdong.runmate

import android.app.Application

class RunMateApplication : Application() {

    // Tạo một lần khi app khởi động, dùng chung cho toàn app
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
