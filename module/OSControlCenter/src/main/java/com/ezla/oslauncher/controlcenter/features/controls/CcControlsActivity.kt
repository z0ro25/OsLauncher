package com.ezla.oslauncher.controlcenter.features.controls

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.appbase.base.BaseActivity
import com.ezla.oslauncher.controlcenter.CcPrefs
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.data.CcTile
import com.ezla.oslauncher.controlcenter.databinding.CcActivityControlsBinding
import com.ezla.oslauncher.controlcenter.databinding.CcItemAddTileBinding
import com.ezla.oslauncher.controlcenter.databinding.CcSheetAddTileBinding
import com.ezla.oslauncher.controlcenter.panel.ControlPanelPages
import com.ezla.oslauncher.controlcenter.skin.CcAccent
import com.ezla.oslauncher.controlcenter.skin.CcSkinSource
import com.ezla.oslauncher.controlcenter.system.CcWallpaper
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Custom Controls (Figma "Custom Controls"): sửa lưới ô trên bản sao panel, Apply mới lưu. */
class CcControlsActivity : BaseActivity<CcActivityControlsBinding>() {

    override val setViewBinding get() = CcActivityControlsBinding.inflate(layoutInflater)

    // Nền tối tràn cả status bar như Figma; header tự chừa chỗ.
    override val padStatusBar = false

    private lateinit var pages: ControlPanelPages
    private var savedTiles: List<CcTile> = emptyList()

    override fun initView() {
        savedTiles = CcPrefs.tiles(this)
        val h = binding.header
        h.tvHeaderTitle.setText(R.string.cc_controls)
        h.tvHeaderTitle.setTextColor(0xFFFFFFFF.toInt())
        h.btnBack.imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
        h.btnApply.visibility = View.VISIBLE
        binding.header.root.setPadding(binding.header.root.paddingLeft, statusBarHeight(),
            binding.header.root.paddingRight, 0)
        binding.header.root.layoutParams.height =
            resources.getDimensionPixelSize(R.dimen.cc_header_height) + statusBarHeight()

        pages = ControlPanelPages(this, binding.pager, editMode = true, host = object : ControlPanelPages.Host {
            override fun closePanel() = Unit
            override fun onPageChanged(page: Int) = renderDots(page)
            override fun onAddTileRequested() = showAddSheet()
        })
        // Chừa thêm chỗ cho header so với overlay (Figma: trang sửa bắt đầu ở y = 166dp).
        pages.pageTopOffsetDp = 166
        pages.applySkin(CcSkinSource.installed(this))
        pages.applyAccent(CcAccent.installed(this))
        renderDots(0)
        val blur = CcPrefs.isBlur(this)
        binding.vScrim.setBackgroundColor(ContextCompat.getColor(this,
            if (blur) R.color.cc_panel_scrim else R.color.cc_panel_scrim_no_blur))
        CcWallpaper.load(this, blur) { bmp -> if (bmp != null) binding.ivBg.setImageBitmap(bmp) }
    }

    override fun viewListener() {
        binding.header.btnBack.setOnClickListener { revertAndFinish() }
        // Back hệ thống = huỷ thay đổi, giống nút back trên header.
        onBackPressedDispatcher.addCallback(this) { revertAndFinish() }
        binding.header.btnApply.setOnClickListener {
            // Lưu đã diễn ra mỗi lần sửa (để panel sửa vẽ lại); Apply chỉ xác nhận giữ.
            Toast.makeText(this, R.string.cc_applied, Toast.LENGTH_SHORT).show()
            finish()
        }
        listOf(binding.dot0, binding.dot1, binding.dot2).forEachIndexed { i, dot ->
            dot.setOnClickListener { pages.showPage(i) }
        }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        pages.start()
    }

    override fun onPause() {
        pages.stop()
        super.onPause()
    }

    // Thoát không bấm Apply thì trả lại danh sách ô lúc mở màn.
    private fun revertAndFinish() {
        CcPrefs.saveTiles(this, savedTiles)
        finish()
    }

    private fun renderDots(page: Int) {
        listOf(binding.dot0, binding.dot1, binding.dot2).forEachIndexed { i, dot ->
            dot.setColorFilter(if (i == page) 0xFFFFFFFF.toInt() else ContextCompat.getColor(this, R.color.cc_page_dot_off))
        }
    }

    private fun showAddSheet() {
        val current = pages.currentTiles()
        if (current.size >= CcTile.MAX) {
            Toast.makeText(this, R.string.cc_controls_full, Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = BottomSheetDialog(this, R.style.CcBottomSheetDialog)
        val sb = CcSheetAddTileBinding.inflate(layoutInflater)
        val available = CcTile.values().filter { it !in current }
        sb.tvEmpty.visibility = if (available.isEmpty()) View.VISIBLE else View.GONE
        sb.rvTiles.layoutManager = LinearLayoutManager(this)
        sb.rvTiles.adapter = AddAdapter(available) { tile ->
            pages.addTile(tile)
            dialog.dismiss()
        }
        dialog.setContentView(sb.root)
        dialog.show()
    }

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    private class AddAdapter(private val items: List<CcTile>, private val onPick: (CcTile) -> Unit) :
        RecyclerView.Adapter<AddAdapter.H>() {
        class H(val b: CcItemAddTileBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            H(CcItemAddTileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(h: H, position: Int) {
            val t = items[position]
            h.b.ivIcon.setImageResource(t.icon)
            h.b.tvLabel.setText(t.label)
            h.b.root.setOnClickListener { onPick(t) }
        }
    }
}
