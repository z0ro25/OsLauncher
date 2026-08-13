package com.oslauncher.applauncher.themelauncher.Features.general.changeicon.data

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.amz.ios.launcher.LauncherAppState
import com.amz.ios.launcher.appoverride.AppOverrideStore
import com.amz.ios.launcher.compat.UserHandleCompat
import com.oslauncher.applauncher.themelauncher.Features.general.common.LauncherReloadScheduler
import java.io.File

/**
 * Lưu trữ cho tính năng đổi ICON app. NGUỒN SỰ THẬT DUY NHẤT = [AppOverrideStore] (Room app_override.db).
 *
 * Đổi logo CHỈ ghi cột `currentLogoPath` = đường dẫn file PNG custom; hiển thị luôn đọc `currentLogoPath`.
 * Engine đọc cùng Store tại cuối [com.amz.ios.launcher.IconCache.cacheLocked] nên logo sống trên cả
 * desktop + All Apps, dính sau reboot. Reset = đưa `currentLogoPath` về null (icon hệ thống).
 *
 * Sau khi ghi -> [LauncherReloadScheduler.scheduleReload] (debounce) để launcher nạp lại workspace/icon.
 * KHÔNG còn broadcast IconDB cũ (đã chuyển hẳn sang Store, hết ghi kép/race).
 *
 * Ảnh gallery là content:// -> COPY ra file PNG VUÔNG bo góc ổn định trong filesDir (engine chạy chung
 * process/UID nên decode được path này); path đó chính là `currentLogoPath` lưu vào Store.
 */
class ChangeAppIconRepository(context: Context) {

    private val appCtx = context.applicationContext

    init {
        AppOverrideStore.init(appCtx)
    }

    /**
     * Đặt icon custom từ ảnh gallery. Copy [source] ra file PNG ổn định rồi ghi path vào Store.
     * @return true nếu copy + ghi thành công.
     */
    fun setIconFromGallery(componentFlatten: String, source: Uri): Boolean {
        val file = copyToIconFile(componentFlatten, source) ?: return false
        AppOverrideStore.setCurrentLogoPath(componentFlatten, file.absolutePath)
        LauncherReloadScheduler.scheduleReload(componentFlatten)
        return true
    }

    /** Đưa icon về mặc định hệ thống: current logo về null + xóa file custom cũ. */
    fun resetIcon(componentFlatten: String) {
        AppOverrideStore.resetLogo(componentFlatten)
        iconFileFor(componentFlatten).takeIf { it.exists() }?.delete()
        LauncherReloadScheduler.scheduleReload(componentFlatten)
    }

    /**
     * Seed logo iOS (icon đã áp theme của engine) vào `currentLogoPath` cho những app đang hiển thị
     * icon theo style iOS ngoài desktop nhưng CHƯA có logo custom. Mục đích: màn list đổi tên/đổi icon
     * thấy logo iOS y hệt desktop thay vì icon Android gốc.
     *
     * Chỉ chạy khi CHƯA có file custom (không đè lên logo user đã tự đặt). Lấy bitmap theme qua
     * [com.amz.ios.launcher.IconCache.getThemeIconForComponent]; trả null với app theme mặc định/CTS
     * -> bỏ qua (giữ icon Android gốc). Ghi ra PNG rồi lưu path vào Store (đồng bộ chức năng đổi logo).
     *
     * LƯU Ý (user đã chấp nhận): seed xong = "đóng băng" logo app đó, đổi theme sau này KHÔNG đổi logo
     * cho tới khi user Reset.
     *
     * @return true nếu vừa seed logo iOS mới cho app này.
     */
    fun seedIosThemeIconIfNeeded(componentFlatten: String): Boolean {
        if (customIconFile(componentFlatten) != null) return false
        return try {
            val cn = ComponentName.unflattenFromString(componentFlatten) ?: return false
            val appState = LauncherAppState.getInstanceNoCreate() ?: return false
            val themeBitmap = appState.iconCache
                .getThemeIconForComponent(cn, UserHandleCompat.myUserHandle()) ?: return false
            val dest = iconFileFor(componentFlatten)
            dest.outputStream().use { output ->
                themeBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            AppOverrideStore.setCurrentLogoPath(componentFlatten, dest.absolutePath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** File icon custom đang lưu cho [componentFlatten] nếu đã đặt từ gallery, ngược lại null. */
    fun customIconFile(componentFlatten: String): File? =
        iconFileFor(componentFlatten).takeIf { it.exists() }

    /** File icon custom cho 1 component, tên an toàn (component có ký tự '/', '.'). */
    private fun iconFileFor(componentFlatten: String): File {
        val dir = File(appCtx.filesDir, ICON_DIR).apply { if (!exists()) mkdirs() }
        val safe = componentFlatten.replace(Regex("[^A-Za-z0-9]"), "_")
        return File(dir, "$safe.png")
    }

    /**
     * Copy ảnh content:// -> file PNG icon VUÔNG, bo góc, ĐÃ center-crop về cỡ icon.
     *
     * Vì sao phải VUÔNG + đúng cỡ: engine `setCustomAppIcon` gọi `Drawable.createFromPath` khi bind;
     * nếu PNG kích thước lẻ / ảnh chữ nhật lớn thì có pass createFromPath trả null -> engine fallback
     * icon HỆ THỐNG rồi ghi đè cache/DB -> desktop "nháy" từ icon mới về icon cũ (đúng bug user gặp).
     * Chuẩn hoá đầu ra thành bitmap VUÔNG cạnh [targetIconSize], ARGB_8888, bo góc -> createFromPath
     * luôn decode được -> hết nháy; đồng thời ảnh không méo (center-crop, không kéo giãn).
     *
     * Trả file, hoặc null nếu lỗi đọc/giải mã/ghi.
     */
    private fun copyToIconFile(componentFlatten: String, source: Uri): File? {
        return try {
            val target = targetIconSize()

            // Lần 1: chỉ đọc kích thước để tính inSampleSize (không cấp phát bitmap).
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appCtx.contentResolver.openInputStream(source).use { input ->
                if (input == null) return null
                BitmapFactory.decodeStream(input, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            // Lần 2: decode thu nhỏ gần cỡ đích (cạnh nhỏ vẫn >= target để center-crop không vỡ nét).
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, target)
            }
            val decoded = appCtx.contentResolver.openInputStream(source).use { input ->
                if (input == null) return null
                BitmapFactory.decodeStream(input, null, opts)
            } ?: return null

            // Center-crop -> vuông cạnh = target, bo góc, rồi ghi PNG.
            val squared = cropToRoundedSquare(decoded, target)
            val dest = iconFileFor(componentFlatten)
            dest.outputStream().use { output ->
                squared.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            decoded.recycle()
            squared.recycle()
            dest
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Cỡ icon đích của launcher (px). Fallback 192px nếu hệ thống không trả về. */
    private fun targetIconSize(): Int {
        val am = appCtx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val size = am?.launcherLargeIconSize ?: 0
        return if (size > 0) size else 192
    }

    /** inSampleSize (luỹ thừa 2) sao cho ảnh sau giải mã vẫn >= target ở cạnh nhỏ. */
    private fun calculateInSampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        var halfMin = minOf(width, height) / 2
        while (halfMin >= target) {
            sample *= 2
            halfMin /= 2
        }
        return sample
    }

    /**
     * Center-crop [src] về ảnh VUÔNG cạnh [size] và bo góc (scaleType = centerCrop):
     *  - lấy hình vuông lớn nhất ở GIỮA ảnh gốc (cạnh = min(w,h)) rồi vẽ khít [size]x[size]
     *    -> ảnh KHÔNG méo (không kéo giãn), phần thừa cạnh dài bị cắt đều 2 bên.
     *  - bo góc bằng [ROUND_CORNER_RATIO] cho khớp thẩm mỹ icon các app khác.
     * Trả bitmap mới VUÔNG, ARGB_8888.
     */
    private fun cropToRoundedSquare(src: Bitmap, size: Int): Bitmap {
        val side = minOf(src.width, src.height)
        val left = (src.width - side) / 2
        val top = (src.height - side) / 2
        val srcRect = Rect(left, top, left + side, top + side)
        val dstRect = RectF(0f, 0f, size.toFloat(), size.toFloat())

        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val radius = size * ROUND_CORNER_RATIO
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        // Vẽ ảnh (đã center-crop qua srcRect->dstRect) trong path bo góc.
        canvas.clipRoundRect(dstRect, radius)
        canvas.drawBitmap(src, srcRect, dstRect, paint)
        return out
    }

    private fun Canvas.clipRoundRect(rect: RectF, radius: Float) {
        val path = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
        clipPath(path)
    }

    companion object {
        private const val ICON_DIR = "custom_app_icons"
        /** Tỉ lệ bo góc theo cạnh (≈ icon iOS/launcher; engine icon_round_corner ~13dp/60dp). */
        private const val ROUND_CORNER_RATIO = 0.22f
    }
}
