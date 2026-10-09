package com.ezla.oslauncher.themes.data

import android.content.Context
import com.truongnt.ios.ioslite.common.util.PreferencesUtil

// Số dư xu TẠM (file prefs chung "com.ezla.oslauncher"), thay hệ thống xu thật đến khi có ví/API.
// Mọi màn của luồng Themes đọc chung để số dư hiển thị nhất quán.
object CoinStore {
    private const val KEY_BALANCE = "themes_coin_balance"
    private const val DEFAULT_BALANCE = 150

    fun balance(context: Context): Int =
        PreferencesUtil.getInt(context, KEY_BALANCE, DEFAULT_BALANCE)

    // Trừ [amount] nếu đủ; trả false khi số dư không đủ (không ghi gì).
    fun spend(context: Context, amount: Int): Boolean {
        val current = balance(context)
        if (current < amount) return false
        PreferencesUtil.putInt(context, KEY_BALANCE, current - amount)
        return true
    }
}
