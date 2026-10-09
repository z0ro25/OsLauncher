package com.ezla.oslauncher.themes.features.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesDialogSuccessSetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// Sheet "Successfully Set!" (Figma bg14) sau khi áp nền xong. Go To Home Screen -> về launcher;
// Explore more Wallpaper -> đóng sheet.
class SuccessfullySetDialog : BottomSheetDialogFragment() {

    private var _binding: ThemesDialogSuccessSetBinding? = null
    private val binding get() = _binding!!

    override fun getTheme() = R.style.ThemesBottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ThemesDialogSuccessSetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(ARG_PREVIEW)?.let {
            Glide.with(binding.imgSetPreview).load(it).into(binding.imgSetPreview)
        }
        binding.btnGoHome.setOnClickListener {
            dismiss()
            goToHomeScreen()
        }
        binding.btnExplore.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Về màn chính của launcher để user thấy nền vừa áp.
    private fun goToHomeScreen() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(home) }
    }

    companion object {
        private const val ARG_PREVIEW = "preview"

        fun newInstance(previewUrl: String?) = SuccessfullySetDialog().apply {
            arguments = bundleOf(ARG_PREVIEW to previewUrl)
        }
    }
}
