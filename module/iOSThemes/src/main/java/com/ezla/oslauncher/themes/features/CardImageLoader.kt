package com.ezla.oslauncher.themes.features

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

// Gom kết quả nhiều ảnh của 1 thẻ (nền + icon): [onDone] gọi đúng 1 lần — false ngay ở ảnh lỗi
// đầu tiên, true khi đủ [total] ảnh ok. total = 0 -> coi như xong luôn, không hiện vòng quay.
class CardImageTracker(private var total: Int, private val onDone: (Boolean) -> Unit) {
    var done = total <= 0
        private set

    fun report(ok: Boolean) {
        if (done) return
        if (!ok || --total <= 0) {
            done = true
            onDone(ok)
        }
    }
}

// Load ảnh thẻ list và báo kết quả (true = có ảnh) để adapter tắt vòng quay / gỡ thẻ lỗi.
// Báo qua post: Glide có thể gọi đồng bộ ngay trong onBind, lúc đó RecyclerView cấm notifyItemRemoved.
fun ImageView.loadCardImage(model: Any?, onResult: (Boolean) -> Unit) {
    Glide.with(this).load(model).listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean
        ): Boolean {
            post { onResult(false) }
            return false
        }

        override fun onResourceReady(
            resource: Drawable?, model: Any?, target: Target<Drawable>?,
            dataSource: DataSource?, isFirstResource: Boolean
        ): Boolean {
            post { onResult(true) }
            return false
        }
    }).into(this)
}
