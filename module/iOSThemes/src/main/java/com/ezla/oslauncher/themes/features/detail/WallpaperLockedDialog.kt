package com.ezla.oslauncher.themes.features.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.features.loadCreditInto
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
        viewLifecycleOwner.loadCreditInto(binding.tvCoin)
        val price = requireArguments().getInt(ARG_PRICE)
        binding.tvPrice.text = price.toString()
        binding.tvUseCoins.text = getString(R.string.themes_use_n_coins, price)
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
        private const val ARG_PRICE = "price"

        // price = coins của background (API); qua arguments để sống sót khi recreate.
        fun newInstance(price: Int) = WallpaperLockedDialog().apply {
            arguments = bundleOf(ARG_PRICE to price)
        }
    }
}
