package com.oslauncher.applauncher.themelauncher.Features.general.hiddenapp.adapters

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.oslauncher.applauncher.themelauncher.databinding.ViewholderHiddenAppBinding
import com.amz.ios.launcher.ItemInfo

class HiddenAdapter(val context: Context, val listItem: ArrayList<ItemInfo>) :
    Adapter<ViewHolder>() {
    var onRemoveHiddenApp: ((ItemInfo) -> Unit)? = null
    override fun onCreateViewHolder(parent: ViewGroup, post: Int): ViewHolder {
        return HiddenAppViewHolder(
            ViewholderHiddenAppBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = listItem.size

    override fun onBindViewHolder(holder: ViewHolder, posittion: Int) {
        val data = listItem[posittion]
        val vhd = holder as HiddenAppViewHolder
        vhd.binding.apply {
            tvAppName.text = data.title
            val appLogo = getApplicationIcon(data.getPackageName())
            ivHiddenApp.setImageDrawable(appLogo)

            ivRemoveHidden.setOnClickListener {
                onRemoveHiddenApp?.invoke(data)
            }
        }
    }

    private fun getApplicationIcon(packageName: String): Drawable {
        val pm: PackageManager = context.packageManager
        val applicationInfo = pm.getApplicationInfo(packageName, 0)
        return pm.getApplicationIcon(applicationInfo)
    }

    class HiddenAppViewHolder(val binding: ViewholderHiddenAppBinding) : ViewHolder(binding.root)
}