package com.ezla.oslauncher.themes.features.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.data.CoinStore
import com.ezla.oslauncher.themes.databinding.ThemesDialogWallpaperLockedBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// Sheet "Wallpaper locked" (Figma bg8) dạng bottom sheet. Trả kết quả qua Fragment Result:
// ACTION_USE_COINS (trả xu để mở khoá) hoặc ACTION_PREMIUM (mở màn IAP — callback ở Activity).
class WallpaperLockedDialog : BottomSheetDialogFragment() {

    private var _binding: ThemesDialogWallpaperLockedBinding? = null
    private val binding get() = _binding!!

    override fun getTheme() = R.style.ThemesBottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ThemesDialogWallpaperLockedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvCoin.text = CoinStore.balance(requireContext()).toString()
        binding.tvPrice.text = PRICE.toString()
        binding.tvUseCoins.text = getString(R.string.themes_use_n_coins, PRICE)
        binding.btnUseCoins.setOnClickListener { result(ACTION_USE_COINS) }
        binding.btnPremium.setOnClickListener { result(ACTION_PREMIUM) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun result(action: String) {
        parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(KEY_ACTION to action))
        dismiss()
    }

    companion object {
        const val REQUEST_KEY = "themes_wallpaper_locked"
        const val KEY_ACTION = "action"
        const val ACTION_USE_COINS = "use_coins"
        const val ACTION_PREMIUM = "premium"

        // Giá mở khoá tạm hardcode theo Figma tới khi server trả giá cho wallpaper.
        const val PRICE = 50

        fun newInstance() = WallpaperLockedDialog()
    }
}
