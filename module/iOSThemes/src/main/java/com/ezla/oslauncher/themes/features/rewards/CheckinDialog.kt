package com.ezla.oslauncher.themes.features.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesDialogCheckinBinding
import kotlinx.coroutines.launch

// Dialog check-in hằng ngày (lần mở Themes đầu tiên mỗi ngày): thẻ Daily Check-in + Claim reward.
// Nhận OK -> đóng, báo REQUEST_KEY (xu + số dư) cho màn gọi; đóng bằng X thì hôm nay chưa tính.
class CheckinDialog : DialogFragment() {

    private var _binding: ThemesDialogCheckinBinding? = null
    private val binding get() = _binding!!
    private var claiming = false

    override fun getTheme() = R.style.ThemesDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ThemesDialogCheckinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Bấm ô hôm nay trong dialog cũng nhận như nút Claim.
        CheckinBinder.bindGrid(binding.checkinCard.rvCheckin) { if (it.isToday) claim() }
        binding.btnClaim.setOnClickListener { claim() }
        binding.imgCheckinClose.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun claim() {
        if (claiming) return
        claiming = true
        binding.btnClaim.isEnabled = false
        val ctx = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val result = CheckinBinder.claimToday(ctx)
            claiming = false
            if (result == null) {
                _binding?.btnClaim?.isEnabled = true
                return@launch
            }
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY, bundleOf(KEY_COINS to result.first, KEY_BALANCE to result.second)
            )
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "themes_daily_checkin"
        const val KEY_COINS = "coins"
        const val KEY_BALANCE = "balance"
        const val TAG = "daily_checkin"
    }
}
