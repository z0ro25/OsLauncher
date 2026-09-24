package com.ezla.oslauncher.beautylauncher.Features.subs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.beautylauncher.databinding.ViewholderIapAppIconBinding

// Dải icon app minh hoạ ở màn IAP. Danh sách (icon, tên) do màn IAP truyền vào — adapter
// không tự gắn asset để không phụ thuộc drawable chưa có.
class IapAppIconAdapter(private val items: List<IapAppIcon>) :
    RecyclerView.Adapter<IapAppIconAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(
        ViewholderIapAppIconBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.ivAppIcon.setImageResource(item.iconRes)
        holder.binding.tvAppName.text = item.label
    }

    class VH(val binding: ViewholderIapAppIconBinding) : RecyclerView.ViewHolder(binding.root)
}
