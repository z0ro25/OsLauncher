package com.ezla.oslauncher.themes.features.controlcenter

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ezla.oslauncher.controlcenter.skin.CcAccent
import com.ezla.oslauncher.controlcenter.skin.CcPanelPreview
import com.ezla.oslauncher.controlcenter.skin.CcSkinSlot
import com.ezla.oslauncher.controlcenter.skin.CcSkinSource
import com.ezla.oslauncher.themes.Models.CcSkinItem

// Gắn panel Control Center dựng tĩnh vào [host] (thu nhỏ theo bề ngang host) rồi tải skin bằng Glide.
// Holder bị tái dùng giữa chừng: [token] đổi nên kết quả tải muộn của thẻ cũ bị bỏ.
class CcPreviewBinder(private val host: FrameLayout) {

    private val preview = CcPanelPreview(host.context)
    private val targets = mutableListOf<CustomTarget<Drawable>>()
    private var token = 0

    init {
        host.addView(preview.view, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        host.addOnLayoutChangeListener { _, l, t, r, b, _, _, _, _ -> fit(r - l, b - t) }
    }

    // [colors] = màu "đang bật" của theme (1-3 mã hex, rỗng = màu gốc). [onDone] báo true khi mọi
    // ảnh tải xong (rỗng = xong ngay), false nếu có ảnh lỗi.
    fun bind(skins: List<CcSkinItem>, colors: List<String> = emptyList(), onDone: (Boolean) -> Unit = {}) {
        val current = ++token
        clearTargets()
        preview.setSkin(null)
        preview.setAccent(CcAccent.parse(colors))
        val bySlot = skins.mapNotNull { s -> CcSkinSlot.fromName(s.name)?.let { it to s.imageUrl } }
            .distinctBy { it.first }
        if (bySlot.isEmpty()) {
            onDone(true)
            return
        }
        val loaded = mutableMapOf<CcSkinSlot, Drawable>()
        var remaining = bySlot.size
        var failed = false
        val glide = Glide.with(host)
        bySlot.forEach { (slot, url) ->
            val target = object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    if (current != token) return
                    loaded[slot] = resource
                    finishOne()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    if (current != token) return
                    failed = true
                    finishOne()
                }

                override fun onLoadCleared(placeholder: Drawable?) = Unit

                private fun finishOne() {
                    if (--remaining > 0) return
                    preview.setSkin(CcSkinSource.of(host.context, loaded))
                    onDone(!failed)
                }
            }
            targets += target
            glide.load(url).into(target)
        }
    }

    private fun clearTargets() {
        val glide = Glide.with(host)
        targets.forEach { glide.clear(it) }
        targets.clear()
    }

    // Panel rộng cố định (300dp): co đều về bề ngang host, canh giữa theo cả 2 chiều.
    private fun fit(w: Int, h: Int) {
        val v = preview.view
        val pw = v.width.takeIf { it > 0 } ?: return
        val ph = v.height.takeIf { it > 0 } ?: return
        val scale = minOf(w / pw.toFloat(), h / ph.toFloat())
        v.pivotX = 0f
        v.pivotY = 0f
        v.scaleX = scale
        v.scaleY = scale
        v.translationX = (w - pw * scale) / 2f
        v.translationY = (h - ph * scale) / 2f
    }
}
