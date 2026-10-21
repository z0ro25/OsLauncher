package com.ezla.oslauncher.themes.features.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesDialogApplyThemeBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// Sheet "Apply Theme" (Figma Install/Apply Theme): xác nhận áp cả theme lên Lock + Home.
// Apply Now trả kết quả qua Fragment Result (REQUEST_KEY) để Activity áp.
class ApplyThemeDialog : BottomSheetDialogFragment() {

    private var _binding: ThemesDialogApplyThemeBinding? = null
    private val binding get() = _binding!!

    override fun getTheme() = R.style.ThemesBottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ThemesDialogApplyThemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(ARG_LOCK)?.let { Glide.with(binding.imgApplyLock).load(it).into(binding.imgApplyLock) }
        arguments?.getString(ARG_HOME)?.let { Glide.with(binding.imgApplyHome).load(it).into(binding.imgApplyHome) }
        binding.imgApplyClose.setOnClickListener { dismiss() }
        binding.btnApplyNow.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle())
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "themes_apply_theme"
        private const val ARG_LOCK = "lock_preview"
        private const val ARG_HOME = "home_preview"

        fun newInstance(lockPreview: String?, homePreview: String?) = ApplyThemeDialog().apply {
            arguments = bundleOf(ARG_LOCK to lockPreview, ARG_HOME to homePreview)
        }
    }
}
