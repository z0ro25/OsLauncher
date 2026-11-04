package com.ezla.oslauncher.controlcenter.dialog

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.data.CcApps
import com.ezla.oslauncher.controlcenter.data.CcSlot
import com.ezla.oslauncher.controlcenter.data.CcTile
import com.ezla.oslauncher.controlcenter.databinding.CcItemAddTileBinding
import com.ezla.oslauncher.controlcenter.databinding.CcSheetAddTileBinding
import com.ezla.oslauncher.controlcenter.panel.TileGridDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Sheet thêm tiện ích (Figma "Custom Controls" 2036:1196): ô hệ thống đã bị xoá + app cài trên máy,
// lọc theo Search. Ô đã có trên panel ([current]) bị ẩn. Chọn 1 ô thì gọi [onPick] rồi đóng.
class AddTileSheet(
    context: Context,
    private val scope: LifecycleCoroutineScope,
    private val current: List<CcSlot>,
    private val onPick: (CcSlot) -> Unit
) : BottomSheetDialog(context, R.style.CcBottomSheetDialog) {

    private data class Entry(val slot: CcSlot, val label: String)

    private val b = CcSheetAddTileBinding.inflate(LayoutInflater.from(context))
    private var all: List<Entry> = emptyList()
    private val adapter = EntryAdapter { slot ->
        onPick(slot)
        dismiss()
    }

    init {
        setContentView(b.root)
        val res = context.resources
        b.rvTiles.layoutManager = GridLayoutManager(context, SPAN)
        b.rvTiles.addItemDecoration(TileGridDecoration(
            res.getDimensionPixelSize(R.dimen.cc_panel_width), res.getDimensionPixelSize(R.dimen.cc_tile), 0, SPAN
        ))
        b.rvTiles.adapter = adapter
        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) = Unit
            override fun afterTextChanged(s: Editable?) = render()
        })
        load()
    }

    // Mở cao gần hết màn như Figma (sheet bắt đầu ngay dưới header), không ở trạng thái thu gọn.
    override fun onStart() {
        super.onStart()
        val sheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val h = (context.resources.displayMetrics.heightPixels * SHEET_RATIO).toInt()
        sheet.layoutParams = sheet.layoutParams.apply { height = h }
        behavior.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun load() {
        val app = context.applicationContext
        val system = CcTile.values().filter { CcSlot.System(it) !in current }
            .map { Entry(CcSlot.System(it), context.getString(it.label)) }
        scope.launch {
            val apps = withContext(Dispatchers.IO) {
                CcApps.launchable(app).map { Entry(CcSlot.App(it.packageName), it.label) }
                    .filter { it.slot !in current }
            }
            all = system + apps
            b.pbLoading.visibility = View.GONE
            render()
        }
    }

    private fun render() {
        val q = b.etSearch.text?.toString()?.trim().orEmpty()
        val list = if (q.isEmpty()) all else all.filter { it.label.contains(q, ignoreCase = true) }
        adapter.items = list
        b.tvEmpty.visibility = if (b.pbLoading.visibility != View.VISIBLE && list.isEmpty()) View.VISIBLE else View.GONE
        b.tvEmpty.setText(if (q.isEmpty()) R.string.cc_controls_all_added else R.string.cc_search_empty)
    }

    private class EntryAdapter(private val onPick: (CcSlot) -> Unit) : RecyclerView.Adapter<EntryAdapter.H>() {
        class H(val b: CcItemAddTileBinding) : RecyclerView.ViewHolder(b.root)

        var items: List<Entry> = emptyList()
            set(v) {
                field = v
                notifyDataSetChanged()
            }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            H(CcItemAddTileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(h: H, position: Int) {
            val e = items[position]
            val ctx = h.b.root.context
            when (val s = e.slot) {
                is CcSlot.System -> h.b.ccTile.setIcon(s.tile.icon)
                is CcSlot.App -> h.b.ccTile.setAppIcon(CcApps.icon(ctx, s.packageName))
            }
            h.b.ccTile.contentDescription = e.label
            h.b.tvLabel.text = e.label
            h.b.ccTile.setOnClickListener { onPick(e.slot) }
        }
    }

    private companion object {
        const val SPAN = 4
        // Figma: sheet cao 648/800 màn.
        const val SHEET_RATIO = 0.81f
    }
}
