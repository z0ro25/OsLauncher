package com.ezla.oslauncher.themes.features.controlcenter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.Models.CcSkinItem
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesDialogSuccessSetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// Sheet "Successfully Set!" sau khi áp skin Control Center (Figma Unlock Control Center/Successfully Set):
// cùng layout sheet của nền/icon, ảnh preview = nền theme + panel xem trước skin.
class ControlCenterSuccessDialog : BottomSheetDialogFragment() {

    private var _binding: ThemesDialogSuccessSetBinding? = null
    private val binding get() = _binding!!

    override fun getTheme() = R.style.ThemesBottomSheet

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ThemesDialogSuccessSetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        args.getString(ARG_PREVIEW)?.let { Glide.with(binding.imgSetPreview).load(it).into(binding.imgSetPreview) }
        val names = args.getStringArrayList(ARG_NAMES).orEmpty()
        val urls = args.getStringArrayList(ARG_URLS).orEmpty()
        binding.frSetCcPreview.visibility = View.VISIBLE
        CcPreviewBinder(binding.frSetCcPreview).bind(names.zip(urls) { n, u -> CcSkinItem(n, u) })
        binding.tvSuccessSub.setText(R.string.themes_cc_ready)
        binding.btnExplore.setText(R.string.themes_explore_more_cc)
        binding.btnGoHome.setOnClickListener {
            dismiss()
            goToHomeScreen()
        }
        binding.btnExplore.setOnClickListener { dismiss() }
    }

    // Ảnh preview nhô lên trên mép sheet: mở hẳn (không ở trạng thái thu gọn) để không bị cắt.
    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun goToHomeScreen() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(home) }
    }

    companion object {
        private const val ARG_PREVIEW = "preview"
        private const val ARG_NAMES = "names"
        private const val ARG_URLS = "urls"

        fun newInstance(previewUrl: String?, skins: List<CcSkinItem>) = ControlCenterSuccessDialog().apply {
            arguments = bundleOf(
                ARG_PREVIEW to previewUrl,
                ARG_NAMES to ArrayList(skins.map { it.name }),
                ARG_URLS to ArrayList(skins.map { it.imageUrl })
            )
        }
    }
}
