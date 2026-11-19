package com.example.monethra.ui.onboarding

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monethra.MonethraApplication
import com.example.monethra.R
import com.example.monethra.data.repository.AuthRepository
import com.example.monethra.data.session.SessionManager
import com.example.monethra.ui.theme.MonethraPastelPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.hypot

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    backgroundBrush: Brush = remember { MonethraPastelPalette.getRandomThreeColorGradient() },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToMain: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as? MonethraApplication
    val userPreferencesRepo = app?.userPreferencesRepository
    val authRepo = app?.authRepository

    // Circular reveal progress: expands radially from 0f to 1f from the center behind the logo
    val revealProgress = remember { Animatable(0f) }

    // Wordmark entrance animation drivers
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkOffsetY = remember { Animatable(12f) }

    LaunchedEffect(Unit) {
        // Expand the 3-color pastel gradient circularly outward from behind the moth logo over 380ms
        launch {
            revealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            )
        }

        // Smoothly fade and slide the bottom "monethra" wordmark upward with slight delay
        launch {
            wordmarkAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, delayMillis = 120, easing = FastOutSlowInEasing),
            )
        }
        launch {
            wordmarkOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 280, delayMillis = 120, easing = FastOutSlowInEasing),
            )
        }

        // Hold for ~1.2 seconds while verifying session
        delay(1200L)

        val isCompleted = try {
            userPreferencesRepo?.isOnboardingCompleted?.first() ?: false
        } catch (_: Exception) {
            false
        }

        // Check in-memory process alive flag and 5-minute timeout gate
        val isSessionValid = if (!SessionManager.isProcessAlive) {
            false
        } else {
            val loggedIn = authRepo?.isSessionLoggedIn() == true
            if (!loggedIn) {
                false
            } else {
                val lastActive = authRepo.getLastActiveTimestamp()
                if (lastActive <= 0L) {
                    true
                } else {
                    val elapsed = System.currentTimeMillis() - lastActive
                    elapsed < AuthRepository.SESSION_TIMEOUT_MS
                }
            }
        }

        if (!isCompleted) {
            onNavigateToOnboarding()
        } else if (isSessionValid) {
            authRepo?.updateLastActiveTimestamp()
            onNavigateToMain()
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEAE6F8)),
    ) {
        // Gradient layer that blooms circularly outward from behind the center moth logo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat()
                    val currentRadius = maxRadius * revealProgress.value

                    val circlePath = Path().apply {
                        addOval(
                            androidx.compose.ui.geometry.Rect(
                                center = centerOffset,
                                radius = currentRadius,
                            ),
                        )
                    }

                    clipPath(circlePath) {
                        this@drawWithContent.drawContent()
                    }
                }
                .background(brush = backgroundBrush),
        )

        // Dead-center stationary moth logo (unified 172.dp with zero extra padding)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val baseModifier = Modifier.size(172.dp)

            val mothModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    baseModifier.sharedElement(
                        rememberSharedContentState(key = "monethra_moth_logo"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> tween(600, easing = FastOutSlowInEasing) },
                    )
                }
            } else {
                baseModifier
            }

            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Monethra Logo",
                contentScale = ContentScale.Fit,
                modifier = mothModifier,
            )
        }

        // Bottom "monethra" wordmark in darker muted pastel plum shade with entrance slide
        Text(
            text = "monethra",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = Color(0xFF3E3B6E),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp)
                .offset { IntOffset(0, wordmarkOffsetY.value.dp.roundToPx()) }
                .alpha(wordmarkAlpha.value),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen()
}
