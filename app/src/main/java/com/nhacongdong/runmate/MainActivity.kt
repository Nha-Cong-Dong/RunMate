package com.nhacongdong.runmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nhacongdong.runmate.spike.ApiSpikeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RunMateApp()
        }
    }
}

@Composable
fun RunMateApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ApiSpikeScreen()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RunMateAppPreview() {
    RunMateApp()
}
