package com.ezla.oslauncher.themes.features.rewards

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesDialogCoinToastBinding

// Toast "+N Coins" (Figma "After Rewarded Ads 01"), tự đóng sau [durationMs]. Chưa gắn vào luồng.
class CoinToastDialog(
    context: Context,
    private val coins: Int,
    private val durationMs: Long = 1500L
) : Dialog(context, R.style.ThemesDialog) {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ThemesDialogCoinToastBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        binding.tvToastCoins.text = context.getString(R.string.themes_coins_plus, coins)
        handler.postDelayed({ if (isShowing) dismiss() }, durationMs)
    }

    override fun onStop() {
        handler.removeCallbacksAndMessages(null)
        super.onStop()
    }
}
