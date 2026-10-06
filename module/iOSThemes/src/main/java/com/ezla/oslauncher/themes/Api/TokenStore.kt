package com.ezla.oslauncher.themes.Api

import android.content.Context
import com.truongnt.ios.ioslite.common.config.SharePrefUtils

// access_token từ add-device, lưu ở prefs chung (SharePrefUtils) để sống qua các lần mở app.
object TokenStore {
    private const val KEY_ACCESS_TOKEN = "themes_access_token"

    fun get(context: Context): String? =
        SharePrefUtils.getString(context, KEY_ACCESS_TOKEN, "").takeIf { !it.isNullOrEmpty() }

    fun save(context: Context, token: String) {
        SharePrefUtils.putString(context, KEY_ACCESS_TOKEN, token)
    }

    fun clear(context: Context) {
        SharePrefUtils.putString(context, KEY_ACCESS_TOKEN, "")
    }
}
