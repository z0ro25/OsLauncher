package com.ezla.oslauncher.themes.features.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.databinding.ThemesDialogSuccessSetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.imageview.ShapeableImageView

// Sheet "Successfully Set!" (Figma Unlock icon|background/Successfully Set). Biến thể icon: phủ lưới
// 3x2 icon lên ảnh nền + câu "new icon". Go To Home Screen -> về launcher; Explore more -> đóng sheet.
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
        val args = requireArguments()
        args.getString(ARG_PREVIEW)?.let {
            Glide.with(binding.imgSetPreview).load(it).into(binding.imgSetPreview)
        }
        if (args.getBoolean(ARG_ICON)) {
            binding.tvSuccessSub.setText(R.string.themes_icon_ready)
            bindIconGrid(args.getStringArrayList(ARG_ICONS).orEmpty())
        }
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

    // Lưới 3 cột x 2 hàng icon 53dp bo 14, khe 13dp (Figma).
    private fun bindIconGrid(icons: List<String>) {
        val grid = binding.gridSetIcons
        grid.removeAllViews()
        if (icons.isEmpty()) return
        grid.visibility = View.VISIBLE
        val res = resources
        val size = res.getDimensionPixelSize(R.dimen.themes_success_icon_size)
        val gap = res.getDimensionPixelSize(R.dimen.themes_success_icon_gap)
        icons.take(MAX_ICONS).forEachIndexed { index, url ->
            val icon = ShapeableImageView(requireContext()).apply {
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                shapeAppearanceModel = shapeAppearanceModel.withCornerSize(
                    res.getDimension(R.dimen.themes_success_icon_radius)
                )
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            val col = index % GRID_COLUMNS
            val row = index / GRID_COLUMNS
            grid.addView(icon, GridLayout.LayoutParams().apply {
                width = size
                height = size
                if (col > 0) leftMargin = gap
                if (row > 0) topMargin = gap
            })
            Glide.with(icon).load(url).into(icon)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Về màn chính của launcher để user thấy thay đổi vừa áp.
    private fun goToHomeScreen() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(home) }
    }

    companion object {
        private const val ARG_PREVIEW = "preview"
        private const val ARG_ICON = "icon"
        private const val ARG_ICONS = "icons"
        private const val GRID_COLUMNS = 3
        private const val MAX_ICONS = 6

        fun newInstance(
            previewUrl: String?,
            forIcon: Boolean = false,
            iconUrls: List<String> = emptyList()
        ) = SuccessfullySetDialog().apply {
            arguments = bundleOf(
                ARG_PREVIEW to previewUrl,
                ARG_ICON to forIcon,
                ARG_ICONS to ArrayList(iconUrls.take(MAX_ICONS))
            )
        }
    }
}
