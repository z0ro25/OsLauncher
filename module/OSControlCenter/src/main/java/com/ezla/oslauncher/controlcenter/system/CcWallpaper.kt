package com.ezla.oslauncher.controlcenter.system

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import com.ezla.oslauncher.controlcenter.R
import com.truongnt.ios.ioslite.common.util.GaussBlurUtil
import java.io.File
import java.util.concurrent.Executors

// Nền panel = hình nền launcher đã làm mờ (yêu cầu: luôn là wallpaper, kể cả khi đang ở app khác).
// Android 14+ thường chặn đọc wallpaper (trả ảnh xám) -> dùng bản tốt gần nhất, rồi ảnh mặc định.
object CcWallpaper {

    private const val CACHE_BLUR = "cc_wallpaper_blur.jpg"
    private const val CACHE_SHARP = "cc_wallpaper_sharp.jpg"
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var blurred: Bitmap? = null
    @Volatile private var sharp: Bitmap? = null

    fun cachedBlur(): Bitmap? = blurred
    fun cachedSharp(): Bitmap? = sharp

    // Đọc + blur ở luồng nền, trả kết quả về main. [blur] = false trả ảnh gốc (công tắc Blur tắt).
    fun load(context: Context, blur: Boolean, onReady: (Bitmap?) -> Unit) {
        val memo = if (blur) blurred else sharp
        if (memo != null && !memo.isRecycled) {
            onReady(memo)
            return
        }
        val app = context.applicationContext
        io.execute {
            ensureLoaded(app)
            val result = if (blur) blurred else sharp
            main.post { onReady(result) }
        }
    }

    // Gọi khi hệ thống báo đổi hình nền: bỏ cache để lần mở panel sau đọc lại.
    fun invalidate(context: Context) {
        val app = context.applicationContext
        io.execute {
            blurred = null
            sharp = null
            ensureLoaded(app)
        }
    }

    private fun ensureLoaded(app: Context) {
        if (blurred != null && sharp != null) return
        val live = readSystemWallpaper(app)
        if (live != null) {
            val s = scaleToScreen(app, live)
            sharp = s
            blurred = blurOf(s)
            save(app, CACHE_SHARP, s)
            blurred?.let { save(app, CACHE_BLUR, it) }
            return
        }
        // Không đọc được: lấy bản đã lưu lần trước; chưa có thì ảnh nền mặc định của Figma.
        val cachedSharp = loadFile(app, CACHE_SHARP)
        val base = cachedSharp ?: BitmapFactory.decodeResource(app.resources, R.drawable.cc_img_default_bg)
        sharp = base
        blurred = loadFile(app, CACHE_BLUR) ?: base?.let { blurOf(it) }
    }

    private fun readSystemWallpaper(app: Context): Bitmap? = try {
        val wm = WallpaperManager.getInstance(app)
        if (wm.wallpaperInfo != null) null
        else (wm.drawable as? BitmapDrawable)?.bitmap?.takeIf { !it.isRecycled && !isNearlyUniform(it) }
    } catch (t: Throwable) {
        null
    }

    private fun scaleToScreen(app: Context, src: Bitmap): Bitmap {
        val dm = app.resources.displayMetrics
        val w = dm.widthPixels.coerceAtLeast(1)
        val h = dm.heightPixels.coerceAtLeast(1)
        // Cắt giữa theo tỉ lệ màn hình rồi thu nhỏ, tránh méo hình.
        val ratio = w.toFloat() / h
        var cw = src.width
        var ch = (cw / ratio).toInt()
        if (ch > src.height) {
            ch = src.height
            cw = (ch * ratio).toInt()
        }
        val x = (src.width - cw) / 2
        val y = (src.height - ch) / 2
        val crop = Bitmap.createBitmap(src, x, y, cw.coerceAtLeast(1), ch.coerceAtLeast(1))
        val targetW = (w / 2).coerceAtLeast(1)
        return Bitmap.createScaledBitmap(crop, targetW, (targetW / ratio).toInt().coerceAtLeast(1), true)
    }

    private fun blurOf(src: Bitmap): Bitmap? = runCatching {
        val small = Bitmap.createScaledBitmap(src, (src.width / 4).coerceAtLeast(1),
            (src.height / 4).coerceAtLeast(1), true)
        GaussBlurUtil.doBlur(small, 18, true)
    }.getOrNull()

    // Ảnh xám đơn sắc = placeholder hệ thống trả khi chặn đọc (cùng ngưỡng với BlurScreenLayout).
    private fun isNearlyUniform(bmp: Bitmap): Boolean {
        val n = 8
        val min = IntArray(3) { 255 }
        val max = IntArray(3)
        for (i in 0 until n) for (j in 0 until n) {
            val c = bmp.getPixel(((i + 0.5f) / n * bmp.width).toInt().coerceAtMost(bmp.width - 1),
                ((j + 0.5f) / n * bmp.height).toInt().coerceAtMost(bmp.height - 1))
            val ch = intArrayOf((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)
            for (k in 0..2) {
                if (ch[k] < min[k]) min[k] = ch[k]
                if (ch[k] > max[k]) max[k] = ch[k]
            }
        }
        return (0..2).maxOf { max[it] - min[it] } < 24
    }

    private fun save(app: Context, name: String, bmp: Bitmap) {
        runCatching {
            File(app.cacheDir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        }
    }

    private fun loadFile(app: Context, name: String): Bitmap? = runCatching {
        val f = File(app.cacheDir, name)
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }.getOrNull()
}
