package com.example.monethra

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.example.monethra.navigation.MonethraApp
import com.example.monethra.ui.theme.MonethraTheme
import kotlinx.coroutines.flow.flowOf

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as? MonethraApplication
        setContent {
            val themeFlow = app?.financeRepository?.themePreference ?: flowOf("SYSTEM")
            val themePreference by themeFlow.collectAsState(initial = "SYSTEM")
            val darkTheme = when (themePreference) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            MonethraTheme(darkTheme = darkTheme) {
                MonethraApp()
            }
        }
    }
}
