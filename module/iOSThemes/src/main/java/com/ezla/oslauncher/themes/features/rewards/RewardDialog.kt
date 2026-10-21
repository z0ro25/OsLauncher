package com.ezla.oslauncher.themes.features.rewards

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesDialogRewardBinding

// 2 biến thể Figma: CHECKIN ("Check-in Successful" + "+N Coins") và AFTER_ADS
// ("Congratulations!" + đã nhận N + mời xem thêm M). Chưa gắn vào luồng nào.
class RewardDialog(
    context: Context,
    private val type: Type,
    private val earned: Int,
    private val nextBonus: Int = 0,
    private val onWatchClaim: () -> Unit = {}
) : Dialog(context, R.style.ThemesDialog) {

    enum class Type { CHECKIN, AFTER_ADS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ThemesDialogRewardBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        val checkin = type == Type.CHECKIN
        binding.tvRewardTitle.setText(if (checkin) R.string.themes_checkin_success else R.string.themes_congrats)
        binding.tvRewardSub.visibility = if (checkin) View.GONE else View.VISIBLE
        binding.tvRewardSub.text = context.getString(R.string.themes_earned_coins, earned)
        binding.tvRewardAmount.visibility = if (checkin) View.VISIBLE else View.GONE
        binding.tvRewardAmount.text = context.getString(R.string.themes_coins_plus, earned)
        binding.tvRewardHint.visibility = if (checkin) View.GONE else View.VISIBLE
        binding.tvRewardHint.text = context.getString(R.string.themes_watch_to_earn, nextBonus)

        binding.imgRewardClose.setOnClickListener { dismiss() }
        binding.btnWatchClaim.setOnClickListener {
            dismiss()
            onWatchClaim()
        }

        // Check-in chỉ báo thành công rồi tự ẩn: không nút đóng, không mời xem ads (đó là của AFTER_ADS).
        if (checkin) {
            binding.imgRewardClose.visibility = View.GONE
            binding.btnWatchClaim.visibility = View.GONE
            handler.postDelayed({ if (isShowing) dismiss() }, CHECKIN_AUTO_DISMISS_MS)
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onStop() {
        handler.removeCallbacksAndMessages(null)
        super.onStop()
    }

    private companion object {
        // Cùng thời lượng CoinToastDialog (toast "+N Coins").
        const val CHECKIN_AUTO_DISMISS_MS = 1500L
    }
}
