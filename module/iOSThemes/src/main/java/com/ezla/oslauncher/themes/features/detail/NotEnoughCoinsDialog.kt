package com.ezla.oslauncher.themes.features.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesDialogNotEnoughCoinsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// Sheet "Not Enough Coins" (Figma bg7) khi số dư không đủ mở theme. Trả kết quả qua Fragment Result:
// ACTION_GET_COIN (mở màn Rewards) hoặc ACTION_UNLOCK_ALL (mở IAP — callback ở Activity).
class NotEnoughCoinsDialog : BottomSheetDialogFragment() {

    private var _binding: ThemesDialogNotEnoughCoinsBinding? = null
    private val binding get() = _binding!!

    override fun getTheme() = R.style.ThemesBottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ThemesDialogNotEnoughCoinsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnGetCoin.setOnClickListener { result(ACTION_GET_COIN) }
        binding.btnUnlockAll.setOnClickListener { result(ACTION_UNLOCK_ALL) }
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
        const val REQUEST_KEY = "themes_not_enough_coins"
        const val KEY_ACTION = "action"
        const val ACTION_GET_COIN = "get_coin"
        const val ACTION_UNLOCK_ALL = "unlock_all"

        fun newInstance() = NotEnoughCoinsDialog()
    }
}
