package com.nielcode.kupass

import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider

/** Bounds the wait if a device reports a longer animation than the key has. */
private const val SPLASH_MAX_WAIT_MILLIS = 1000L
private const val SPLASH_FADE_MILLIS = 200L

/** Lets the key animation (Android 12+) finish when Kupass is ready sooner, then fades out. */
internal fun SplashScreen.waitForKeyAnimation() = setOnExitAnimationListener { splash ->
    splash.view
        .animate()
        .alpha(0f)
        .setStartDelay(splash.animationLeftMillis().coerceIn(0L, SPLASH_MAX_WAIT_MILLIS))
        .setDuration(SPLASH_FADE_MILLIS)
        .withEndAction(splash::remove)
}

private fun SplashScreenViewProvider.animationLeftMillis() =
    iconAnimationStartMillis + iconAnimationDurationMillis - System.currentTimeMillis()
