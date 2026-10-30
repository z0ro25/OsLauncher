package com.ezla.oslauncher.calculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.ezla.oslauncher.calculator.databinding.CalcSheetHistoryBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// Sheet lịch sử, 4 trạng thái Figma: xem "Edit|Done"; sửa "back|Clear All"; đã chọn
// "back|Delete (n)"; rỗng "back|Done" + icon "No history".
class HistorySheet : BottomSheetDialogFragment() {

    interface Host {
        fun onHistoryPicked(result: String)
    }

    private var _b: CalcSheetHistoryBinding? = null
    private val b get() = _b!!
    private lateinit var store: HistoryStore
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = CalcSheetHistoryBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onStart() {
        super.onStart()
        // Fragment inflate với container null nên layout_height 360dp bị bỏ -> ép lại, nếu không
        // RecyclerView (0dp) co về 0 và chỉ còn header. Mở thẳng full, không dừng ở peek.
        val sheetHeight = (SHEET_HEIGHT_DP * resources.displayMetrics.density).toInt()
        _b?.root?.layoutParams?.let {
            it.height = sheetHeight
            _b?.root?.layoutParams = it
        }
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            peekHeight = sheetHeight
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = HistoryStore(requireContext())
        adapter = HistoryAdapter(
            onItemClick = { e ->
                (activity as? Host)?.onHistoryPicked(e.result)
                dismiss()
            },
            onSelectionChanged = { renderHeader() }
        )
        b.calcSheetList.adapter = adapter
        // Sheet 360dp tính cả vùng thanh điều hướng (Figma y 441..800): list chỉ né bằng padding đáy.
        val listPad = b.calcSheetList.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            b.calcSheetList.updatePadding(bottom = listPad + insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }

        b.calcSheetEdit.setOnClickListener { adapter.editMode = true }
        b.calcSheetBack.setOnClickListener {
            if (adapter.editMode) adapter.editMode = false else dismiss()
        }
        b.calcSheetAction.setOnClickListener { onActionClick() }
        reload()
    }

    private fun onActionClick() {
        val empty = adapter.itemCount == 0
        when {
            empty || !adapter.editMode -> dismiss()
            adapter.selected.isEmpty() -> {
                store.clear()
                adapter.editMode = false
                reload()
            }
            else -> {
                store.delete(adapter.selected.toSet())
                adapter.selected.clear()
                reload()
            }
        }
    }

    private fun reload() {
        adapter.submit(store.load())
        val empty = adapter.itemCount == 0
        if (empty && adapter.editMode) adapter.editMode = false
        b.calcSheetList.visibility = if (empty) View.GONE else View.VISIBLE
        b.calcSheetEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        renderHeader()
    }

    private fun renderHeader() {
        val ctx = context ?: return
        val empty = adapter.itemCount == 0
        val edit = adapter.editMode
        // Bên trái: "Edit" chỉ ở chế độ xem có dữ liệu; còn lại là back.
        b.calcSheetEdit.visibility = if (!edit && !empty) View.VISIBLE else View.GONE
        b.calcSheetBack.visibility = if (edit || empty) View.VISIBLE else View.GONE
        when {
            empty || !edit -> {
                b.calcSheetAction.setText(R.string.calc_done)
                b.calcSheetAction.setTextColor(ContextCompat.getColor(ctx, R.color.calc_text_primary))
            }
            adapter.selected.isEmpty() -> {
                b.calcSheetAction.setText(R.string.calc_clear_all)
                b.calcSheetAction.setTextColor(ContextCompat.getColor(ctx, R.color.calc_danger))
            }
            else -> {
                b.calcSheetAction.text = getString(R.string.calc_delete_count, adapter.selected.size)
                b.calcSheetAction.setTextColor(ContextCompat.getColor(ctx, R.color.calc_danger))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    companion object {
        const val TAG = "calc_history"
        private const val SHEET_HEIGHT_DP = 360
    }
}
