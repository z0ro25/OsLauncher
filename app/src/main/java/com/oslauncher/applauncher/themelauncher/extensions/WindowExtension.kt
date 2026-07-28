package com.oslauncher.applauncher.themelauncher.extensions

import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

private fun getLayoutListener(window: Window): ViewTreeObserver.OnGlobalLayoutListener? {
    if (layoutListener == null)
        layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (window.setFullScreenWallpaper()) return@OnGlobalLayoutListener
        }
    return layoutListener
}

fun Window.hideNavigation() {
    if (setFullScreenWallpaper()) return

    decorView.viewTreeObserver.addOnGlobalLayoutListener(getLayoutListener(this))
}

private fun Window.setFullScreenWallpaper(): Boolean {
    val windowInsetsController: WindowInsetsControllerCompat? =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            ViewCompat.getWindowInsetsController(decorView)
        } else {
            WindowInsetsControllerCompat(this, decorView)
        }

    if (windowInsetsController == null) {
        return true
    }
//    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

//    setFlags(
//        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
//        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
//    )
    windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
    windowInsetsController.hide(WindowInsetsCompat.Type.systemGestures())
    return false
}

fun Window.showNav() {
    val windowInsetsController: WindowInsetsControllerCompat?
    windowInsetsController = if (Build.VERSION.SDK_INT >= 30) {
        ViewCompat.getWindowInsetsController(decorView)
    } else WindowInsetsControllerCompat(this, decorView)

    if (windowInsetsController == null) {
        return
    }
    windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
    windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
    decorView.viewTreeObserver.removeOnGlobalLayoutListener(getLayoutListener(this))
}



