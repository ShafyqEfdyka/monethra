package com.example.monethra

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val view = splashScreenView.view
            ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).apply {
                duration = 250L
                interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
                doOnEnd { splashScreenView.remove() }
                start()
            }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
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
                AuthViewModelFactory(authRepo),
            )[AuthViewModel::class.java]
        }

        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
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
                        val repo = (application as? MonethraApplication)?.authRepository
                        if (repo != null && repo.isSessionLoggedIn()) {
                            val lastActiveTimestamp = repo.getLastActiveTimestamp()
                            if (lastActiveTimestamp > 0L) {
                                val elapsed = System.currentTimeMillis() - lastActiveTimestamp
                                if (elapsed >= AuthRepository.SESSION_TIMEOUT_MS) {
                                    authViewModel?.logoutDueToTimeout() ?: CoroutineScope(Dispatchers.Main).launch {
                                        repo.logout()
                                    }
                                } else {
                                    authViewModel?.onUserInteraction()
                                }
                            } else {
                                authViewModel?.onUserInteraction()
                            }
                        }
                    }
                    else -> {}
                }
            },
        )

        setContent {
            MonethraTheme {
                MonethraApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            val app = application as? MonethraApplication
            app?.sessionManager?.clearSessionSync()
        }
    }
}
