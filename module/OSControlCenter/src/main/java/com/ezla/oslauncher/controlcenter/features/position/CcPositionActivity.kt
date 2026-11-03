package com.ezla.oslauncher.controlcenter.features.position

import android.content.res.ColorStateList
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.ezla.oslauncher.appbase.base.BaseActivity
import com.ezla.oslauncher.controlcenter.CcPrefs
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.data.CcEdge
import com.ezla.oslauncher.controlcenter.databinding.CcActivityPositionBinding
import com.ezla.oslauncher.controlcenter.dialog.ColorPickerDialog

/** Màn Position (Figma "Position"): chỉnh nháp, bấm Apply mới lưu để overlay cập nhật. */
class CcPositionActivity : BaseActivity<CcActivityPositionBinding>() {

    override val setViewBinding get() = CcActivityPositionBinding.inflate(layoutInflater)

    private lateinit var draft: CcPrefs.EdgeSettings

    override fun initView() {
        binding.header.tvHeaderTitle.setText(R.string.cc_position)
        binding.header.tvHeaderTitle.setTextColor(ContextCompat.getColor(this, R.color.cc_title))
        binding.header.btnBack.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.cc_title))
        binding.header.btnApply.visibility = View.VISIBLE
        draft = CcPrefs.edgeSettings(this)
        render()
        showBannerOverlay(binding.frBanner, binding.mainLayout, false)
    }

    override fun viewListener() {
        binding.header.btnBack.setOnClickListener { finish() }
        binding.header.btnApply.setOnClickListener {
            CcPrefs.saveEdgeSettings(this, draft)
            Toast.makeText(this, R.string.cc_applied, Toast.LENGTH_SHORT).show()
            finish()
        }
        binding.optLeft.setOnClickListener { select(CcEdge.LEFT) }
        binding.optTop.setOnClickListener { select(CcEdge.TOP) }
        binding.optBottom.setOnClickListener { select(CcEdge.BOTTOM) }
        binding.optRight.setOnClickListener { select(CcEdge.RIGHT) }
        binding.rowHide.setOnClickListener {
            draft = draft.copy(hide = !draft.hide)
            render()
        }
        binding.rowColor.setOnClickListener {
            ColorPickerDialog(this, draft.color) { c ->
                draft = draft.copy(color = c)
                render()
            }.show()
        }
        bindSlider(binding.sbWidth) { draft = draft.copy(width = it) }
        bindSlider(binding.sbLength) { draft = draft.copy(length = it) }
        bindSlider(binding.sbOpacity) { draft = draft.copy(opacity = it) }
        bindSlider(binding.sbOffset) { draft = draft.copy(offset = it) }
    }

    override fun dataObservable() {}

    private fun select(edge: CcEdge) {
        draft = draft.copy(edge = edge)
        render()
    }

    private fun bindSlider(sb: SeekBar, onChange: (Int) -> Unit) {
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) onChange(p)
            }

            override fun onStartTrackingTouch(s: SeekBar) = Unit
            override fun onStopTrackingTouch(s: SeekBar) = Unit
        })
    }

    private fun render() {
        val options = listOf(
            Triple(CcEdge.LEFT, binding.imgLeft, binding.lblLeft),
            Triple(CcEdge.TOP, binding.imgTop, binding.lblTop),
            Triple(CcEdge.BOTTOM, binding.imgBottom, binding.lblBottom),
            Triple(CcEdge.RIGHT, binding.imgRight, binding.lblRight)
        )
        options.forEach { (edge, img, lbl) ->
            val selected = edge == draft.edge
            img.isSelected = selected
            lbl.isSelected = selected
            lbl.setTextColor(ContextCompat.getColor(this,
                if (selected) R.color.cc_white else R.color.cc_text))
        }
        binding.colorDot.backgroundTintList = ColorStateList.valueOf(draft.color)
        binding.swHide.isChecked = draft.hide
        binding.sbWidth.progress = draft.width
        binding.sbLength.progress = draft.length
        binding.sbOpacity.progress = draft.opacity
        binding.sbOffset.progress = draft.offset
        // Ẩn vạch thì độ mờ không còn ý nghĩa: làm mờ thanh trượt cho dễ hiểu.
        binding.sbOpacity.alpha = if (draft.hide) 0.4f else 1f
    }
}
