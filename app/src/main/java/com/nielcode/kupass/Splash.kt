package com.nielcode.kupass

import android.app.Activity
import android.content.res.Resources
import android.os.Build
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider

/** Bounds the wait if a device reports a longer animation than the key has. */
private const val SPLASH_MAX_WAIT_MILLIS = 1000L

/**
 * Lets the key animation (Android 12+) finish when Kupass is ready sooner, then removes the splash
 * screen at once. No fade: the key is a separate surface that a fading splash view leaves on screen
 * until the end, over the vault.
 */
internal fun SplashScreen.waitForKeyAnimation() = setOnExitAnimationListener { splash ->
    splash.view.postDelayed(
        splash::remove,
        splash.animationLeftMillis().coerceIn(0L, SPLASH_MAX_WAIT_MILLIS),
    )
}

private fun SplashScreenViewProvider.animationLeftMillis() =
    iconAnimationStartMillis + iconAnimationDurationMillis - System.currentTimeMillis()

/**
 * The system draws the splash screen before Kupass runs, so it is told now which background the
 * next launch needs. Below Android 12 there are no dynamic colors and nothing to tell.
 */
internal fun Activity.matchNextSplashScreen(dynamicColor: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        splashScreen.setSplashScreenTheme(
            if (dynamicColor) Resources.ID_NULL else R.style.Theme_Kupass_SplashScreen_Static
        )
    }
}
