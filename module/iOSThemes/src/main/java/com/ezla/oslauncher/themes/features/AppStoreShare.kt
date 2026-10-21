package com.ezla.oslauncher.themes.features

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import com.ezla.oslauncher.themes.R

// Nút share ở Wallpaper / Icon detail: chia sẻ app trên store (tên app + link Play), như Share của Settings.
fun Activity.shareAppOnStore() {
    val appName = applicationInfo.loadLabel(packageManager).toString()
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, appName)
        putExtra(Intent.EXTRA_TEXT, "$appName\nhttps://play.google.com/store/apps/details?id=$packageName")
    }
    try {
        startActivity(Intent.createChooser(intent, getString(R.string.themes_cd_share)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, R.string.themes_no_app_to_open, Toast.LENGTH_SHORT).show()
    }
}
