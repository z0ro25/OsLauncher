package com.ezla.oslauncher.calculator

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.calculator.databinding.CalcItemHistoryBinding
import com.ezla.oslauncher.calculator.databinding.CalcItemHistoryHeaderBinding

/** Danh sách lịch sử nhóm theo ngày (Hôm nay / Hôm qua / Trước đó); chế độ sửa hiện ô chọn tròn. */
class HistoryAdapter(
    private val onItemClick: (HistoryStore.Entry) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        data class Header(val titleRes: Int) : Row()
        data class Item(val entry: HistoryStore.Entry) : Row()
    }

    private var rows: List<Row> = emptyList()
    val selected = LinkedHashSet<Long>()

    var editMode = false
        set(value) {
            field = value
            if (!value) selected.clear()
            notifyDataSetChanged()
            onSelectionChanged(selected.size)
        }

    fun submit(entries: List<HistoryStore.Entry>) {
        val out = ArrayList<Row>()
        var lastGroup = -1
        entries.forEach {
            val g = groupOf(it.time)
            if (g != lastGroup) {
                out.add(Row.Header(g))
                lastGroup = g
            }
            out.add(Row.Item(it))
        }
        rows = out
        selected.retainAll(entries.map { it.id }.toSet())
        notifyDataSetChanged()
        onSelectionChanged(selected.size)
    }

    private fun groupOf(time: Long): Int = when {
        DateUtils.isToday(time) -> R.string.calc_today
        DateUtils.isToday(time + DateUtils.DAY_IN_MILLIS) -> R.string.calc_yesterday
        else -> R.string.calc_older
    }

    override fun getItemCount() = rows.size

    override fun getItemViewType(position: Int) = if (rows[position] is Row.Header) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(CalcItemHistoryHeaderBinding.inflate(inf, parent, false))
        } else {
            ItemVH(CalcItemHistoryBinding.inflate(inf, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderVH).b.calcHeaderTitle.setText(row.titleRes)
            is Row.Item -> (holder as ItemVH).bind(row.entry)
        }
    }

    private class HeaderVH(val b: CalcItemHistoryHeaderBinding) : RecyclerView.ViewHolder(b.root)

    private inner class ItemVH(val b: CalcItemHistoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: HistoryStore.Entry) {
            b.calcItemExpression.text = e.expression
            b.calcItemResult.text = e.result
            b.calcItemCheck.visibility = if (editMode) View.VISIBLE else View.GONE
            b.calcItemCheck.setImageResource(
                if (e.id in selected) R.drawable.calc_ic_selected else R.drawable.calc_ic_unselected
            )
            b.root.setOnClickListener {
                if (editMode) {
                    if (!selected.remove(e.id)) selected.add(e.id)
                    notifyItemChanged(bindingAdapterPosition)
                    onSelectionChanged(selected.size)
                } else {
                    onItemClick(e)
                }
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }
}
