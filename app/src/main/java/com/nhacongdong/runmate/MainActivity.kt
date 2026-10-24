package com.nhacongdong.runmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nhacongdong.runmate.ui.RunMateApp
import com.nhacongdong.runmate.ui.theme.RunMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RunMateTheme {
                RunMateApp()
            }
        }
    }
}
