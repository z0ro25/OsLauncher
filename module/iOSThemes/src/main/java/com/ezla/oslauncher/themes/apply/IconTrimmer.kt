package com.ezla.oslauncher.themes.apply

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import java.io.File

// Ảnh icon của theme có lề trong suốt + bóng đổ nên khi launcher vẽ full ô thì THÂN icon nhỏ hơn
// app khác. Cắt sát thân icon, chỉ chừa lại KEEP_SHADOW phần bóng, rồi đưa về ô vuông.
object IconTrimmer {

    // Pixel có alpha >= BODY_ALPHA coi là thân icon; > SHADOW_ALPHA là còn bóng đổ.
    private const val BODY_ALPHA = 128
    private const val SHADOW_ALPHA = 8

    // Tỉ lệ khoảng bóng (từ mép thân tới mép bóng) được giữ lại mỗi phía.
    private const val KEEP_SHADOW = 0.35f

    // Ghi đè [file] bằng ảnh đã cắt; lỗi hoặc không tìm thấy thân icon thì giữ nguyên ảnh gốc.
    fun trim(file: File): Boolean = try {
        val src = BitmapFactory.decodeFile(file.absolutePath)
        val out = src?.let { crop(it) }
        if (out == null) {
            false
        } else {
            val tmp = File(file.parentFile, file.name + ".trim")
            tmp.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file.delete()
            tmp.renameTo(file)
        }
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }

    private fun crop(src: Bitmap): Bitmap? {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val body = bounds(pixels, w, h, BODY_ALPHA) ?: return null
        val shadow = bounds(pixels, w, h, SHADOW_ALPHA) ?: body

        // Thân icon + một phần bóng mỗi phía.
        val left = body.left - ((body.left - shadow.left) * KEEP_SHADOW).toInt()
        val top = body.top - ((body.top - shadow.top) * KEEP_SHADOW).toInt()
        val right = body.right + ((shadow.right - body.right) * KEEP_SHADOW).toInt()
        val bottom = body.bottom + ((shadow.bottom - body.bottom) * KEEP_SHADOW).toInt()

        // Đưa về ô vuông quanh tâm vùng cắt (launcher vẽ icon vào ô vuông).
        val side = maxOf(right - left, bottom - top)
        if (side <= 0 || side >= maxOf(w, h)) return null
        val cx = (left + right) / 2
        val cy = (top + bottom) / 2
        val sx = cx - side / 2
        val sy = cy - side / 2

        val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, -sx.toFloat(), -sy.toFloat(), null)
        return out
    }

    // Khung bao các pixel có alpha >= [minAlpha]; null nếu không có pixel nào.
    private fun bounds(pixels: IntArray, w: Int, h: Int, minAlpha: Int): Rect? {
        var l = w
        var t = h
        var r = -1
        var b = -1
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                if ((pixels[row + x] ushr 24) >= minAlpha) {
                    if (x < l) l = x
                    if (x > r) r = x
                    if (y < t) t = y
                    if (y > b) b = y
                }
            }
        }
        return if (r < 0) null else Rect(l, t, r + 1, b + 1)
    }
}
