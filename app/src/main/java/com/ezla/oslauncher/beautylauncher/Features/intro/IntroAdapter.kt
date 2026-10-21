package com.ezla.oslauncher.beautylauncher.Features.intro

import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ezla.oslauncher.beautylauncher.R
import com.ezla.oslauncher.beautylauncher.databinding.ItemSlideLayoutBinding
import com.ezla.oslauncher.beautylauncher.databinding.ViewholderNativeFullOnbBinding
import com.ezla.oslauncher.beautylauncher.model.IntroModel
import com.truongnt.ios.ioslite.common.config.AppAds

class IntroAdapter(val context: AppCompatActivity, val introItems: List<IntroModel>) :
    Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == 0) IntroViewHolder(
            ItemSlideLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ).also { b ->
                // Gắn 1 lần lúc tạo holder (không ở onBind) để listener không chồng lên khi tái sử dụng.
                b.imLogoSlide.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                    fitWidthBottom(v as ImageView)
                }
            }
        ) else NativeFullOnbViewHolder(
            ViewholderNativeFullOnbBinding.inflate(context.layoutInflater, parent, false)
        )
    }

    override fun getItemCount(): Int = introItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = introItems[position]
        if (data.position == "native_full") {
            (holder as NativeFullOnbViewHolder).apply {
                // Adapter nhận Context; SDK cần Activity nên phải dò ra. IntroActivity truyền
                // chính nó vào nên bình thường luôn ra Activity.
                AppAds.showNativeFill(
                    context,
                    "native_onb_full",
                    binding.ntFullContainer,
                    R.layout.layout_native_onb_full,
                    com.truongnt.ios.ioslite.common.R.layout.shimmer_native_app_libs
                ) { }
            }

        } else {
            val viewholder = holder as IntroViewHolder
            viewholder.binding.apply {
                data.bmID?.let { imLogoSlide.setImageResource(it) }
                fitWidthBottom(imLogoSlide)
            }
        }

    }

    // Phủ kín view (như centerCrop) nhưng neo ĐÁY: phần dư chỉ cắt ở trên, nội dung đáy ảnh luôn hiện đủ.
    private fun fitWidthBottom(iv: ImageView) {
        val d = iv.drawable ?: return
        val vw = iv.width.toFloat()
        val vh = iv.height.toFloat()
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        if (vw <= 0f || vh <= 0f || dw <= 0f || dh <= 0f) return
        val scale = maxOf(vw / dw, vh / dh)
        iv.imageMatrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate((vw - dw * scale) / 2f, vh - dh * scale)
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (introItems[position].position == "native_full") 1 else 0

    private class IntroViewHolder(val binding: ItemSlideLayoutBinding) : ViewHolder(binding.root)
    private class NativeFullOnbViewHolder(val binding: ViewholderNativeFullOnbBinding) :
        ViewHolder(binding.root)
}
