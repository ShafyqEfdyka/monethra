package com.example.monethra

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import com.example.monethra.data.repository.AuthRepository
import com.example.monethra.data.session.SessionManager
import com.example.monethra.navigation.MonethraApp
import com.example.monethra.ui.theme.MonethraTheme
import com.example.monethra.viewmodel.AuthViewModel
import com.example.monethra.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private var authViewModel: AuthViewModel? = null

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            authViewModel?.onUserInteraction()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as? MonethraApplication
        val sessionManager = app?.sessionManager
        val authRepo = app?.authRepository

        // Cold App Launch: check if app was completely closed/killed
        if (!SessionManager.isProcessAlive) {
            sessionManager?.clearSessionSync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    authRepo?.logout()
                    sessionManager?.clearSession()
                } catch (_: Exception) {}
            }
            SessionManager.isProcessAlive = true
        }

        if (authRepo != null) {
            authViewModel = ViewModelProvider(
                this,
                AuthViewModelFactory(authRepo)
            )[AuthViewModel::class.java]
        }

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // Moving to background (ignore configuration changes like device rotation)
                    if (!isChangingConfigurations) {
                        val repo = (application as? MonethraApplication)?.authRepository
                        if (repo?.isSessionLoggedIn() == true) {
                            val now = System.currentTimeMillis()
                            repo.saveLastActiveTimestamp(now)
                            (application as? MonethraApplication)?.sessionManager?.saveLastActiveTimestampSync(now)
                        }
                    }
                }
                Lifecycle.Event.ON_START -> {
                    // Returning to foreground or cold launch
                    val repo = (application as? MonethraApplication)?.authRepository
                    if (repo != null && repo.isSessionLoggedIn()) {
                        val lastActiveTimestamp = repo.getLastActiveTimestamp()
                        if (lastActiveTimestamp > 0L) {
                            val elapsed = System.currentTimeMillis() - lastActiveTimestamp
                            if (elapsed >= AuthRepository.SESSION_TIMEOUT_MS) {
                                // Session expired (>= 5 minutes): clear session and navigate to LoginScreen
                                authViewModel?.logoutDueToTimeout() ?: CoroutineScope(Dispatchers.Main).launch {
                                    repo.logout()
                                }
                            } else {
                                // Within 5 minutes: keep user logged in and start fresh 5-minute foreground timer
                                authViewModel?.onUserInteraction()
                            }
                        } else {
                            authViewModel?.onUserInteraction()
                        }
                    }
                }
                else -> {}
            }
        })

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

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            val app = application as? MonethraApplication
            app?.sessionManager?.clearSessionSync()
            SessionManager.isProcessAlive = false
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    app?.authRepository?.logout()
                    app?.sessionManager?.clearSession()
                } catch (_: Exception) {}
            }
        }
    }
}
