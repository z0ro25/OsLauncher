package com.ezla.oslauncher.themes.features.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesDialogChooseApplyBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// Dialog "Choose where to apply" (Figma bg9) dạng bottom sheet sát đáy. Chọn Lock/Home screen;
// Next trả kết quả qua Fragment Result (REQUEST_KEY) để Activity áp theme.
class ChooseWhereToApplyDialog : BottomSheetDialogFragment() {

    private var _binding: ThemesDialogChooseApplyBinding? = null
    private val binding get() = _binding!!

    private var lock = true
    private var home = true

    override fun getTheme() = R.style.ThemesBottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ThemesDialogChooseApplyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvChooseTitle.setText(R.string.themes_choose_where)
        binding.tvLock.setText(R.string.themes_lock_screen)
        binding.tvHome.setText(R.string.themes_home_screen)
        arguments?.getString(ARG_LOCK_PREVIEW)?.let {
            Glide.with(binding.imgLock).load(it).into(binding.imgLock)
        }
        arguments?.getString(ARG_HOME_PREVIEW)?.let {
            Glide.with(binding.imgHome).load(it).into(binding.imgHome)
        }
        render()

        binding.layoutLock.setOnClickListener { lock = !lock; render() }
        binding.layoutHome.setOnClickListener { home = !home; render() }
        binding.btnNext.setOnClickListener {
            if (!lock && !home) return@setOnClickListener
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY, bundleOf(KEY_LOCK to lock, KEY_HOME to home)
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render() {
        binding.imgLockCheck.setImageResource(
            if (lock) R.drawable.themes_ic_check_on else R.drawable.themes_ic_check_off
        )
        binding.imgHomeCheck.setImageResource(
            if (home) R.drawable.themes_ic_check_on else R.drawable.themes_ic_check_off
        )
    }

    companion object {
        const val REQUEST_KEY = "themes_choose_where_apply"
        const val KEY_LOCK = "apply_lock"
        const val KEY_HOME = "apply_home"

        private const val ARG_LOCK_PREVIEW = "lock_preview"
        private const val ARG_HOME_PREVIEW = "home_preview"

        fun newInstance(lockPreview: String?, homePreview: String?): ChooseWhereToApplyDialog =
            ChooseWhereToApplyDialog().apply {
                arguments = bundleOf(
                    ARG_LOCK_PREVIEW to lockPreview,
                    ARG_HOME_PREVIEW to homePreview
                )
            }
    }
}
