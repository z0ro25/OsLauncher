package com.ezla.oslauncher.clock.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.clock.databinding.OsclockItemTextRowBinding

/** Hàng chữ 44dp; [checkAtStart]: tick trái + chữ lùi 48dp (chọn chuông) hay tick phải (chọn ngày). */
class TextRowAdapter(
    private val checkAtStart: Boolean,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<TextRowAdapter.VH>() {

    private var items: List<String> = emptyList()
    private var checked: (Int) -> Boolean = { false }

    fun submit(list: List<String>, isChecked: (Int) -> Boolean = { false }) {
        items = list
        checked = isChecked
        @Suppress("NotifyDataSetChanged") notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(OsclockItemTextRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val v = holder.v
        val d = v.root.resources.displayMetrics.density
        v.osclockRowText.text = items[position]
        val lp = v.osclockRowText.layoutParams as ViewGroup.MarginLayoutParams
        lp.marginStart = ((if (checkAtStart) 48 else 16) * d).toInt()
        v.osclockRowText.layoutParams = lp
        val on = checked(position)
        v.osclockRowCheckStart.visibility = if (checkAtStart && on) View.VISIBLE else View.INVISIBLE
        v.osclockRowCheckEnd.visibility = if (!checkAtStart && on) View.VISIBLE else View.GONE
        v.osclockRowSeparator.visibility = if (position == items.size - 1) View.INVISIBLE else View.VISIBLE
        v.root.setOnClickListener { holder.bindingAdapterPosition.takeIf { it >= 0 }?.let(onClick) }
    }

    class VH(val v: OsclockItemTextRowBinding) : RecyclerView.ViewHolder(v.root)
}
