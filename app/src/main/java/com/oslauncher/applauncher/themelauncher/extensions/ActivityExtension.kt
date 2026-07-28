package com.oslauncher.applauncher.themelauncher.extensions

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

//Start activity có intent,bundle bình thường = startActivity
inline fun <reified T : Any> Context.launchActivity(
    options: Bundle? = null,
    noinline init: Intent.() -> Unit = {}
) {
    val intent = newIntent<T>(this)
    intent.init()
    startActivity(intent, options)
}

//tạo mới intent từ class
inline fun <reified T : Any> newIntent(context: Context): Intent =
    Intent(context, T::class.java)

//start activity dùng registerActivityForResult
fun AppCompatActivity.launchActivityForResult(
    callback: (ActivityResult) -> Unit
) {
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        callback.invoke(result)
    }
}

//đổi màu status bar
fun AppCompatActivity.changeStatusBarColor(@ColorRes color: Int, lightStatusBar: Boolean = false) {
    if (window != null) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, color)
    }
    if (lightStatusBar)
        this.lightStatusBar()
}

//set light statusBar
fun AppCompatActivity.lightStatusBar() {
    val decorView: View? = this.window?.decorView
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val wic = decorView?.windowInsetsController
        wic?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
    } else
        decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
}



