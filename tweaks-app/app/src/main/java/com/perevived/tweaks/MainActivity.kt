package com.perevived.tweaks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.perevived.tweaks.ui.PETweaksApp
import com.perevived.tweaks.ui.theme.PETweaksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PETweaksTheme {
                PETweaksApp()
            }
        }
    }
}
