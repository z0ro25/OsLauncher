package com.amz.ios.launcher.appoverride

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Facade + cache in-memory cho [AppOverrideDatabase]. NGUỒN SỰ THẬT DUY NHẤT tên/logo custom.
 *
 * Vì sao có cache: engine ([com.amz.ios.launcher.IconCache]) đọc tên/logo NGAY lúc dựng icon — không
 * được hit DB mỗi lần vẽ. Toàn bộ bảng (nhỏ) được nạp 1 lần vào [cache] (ConcurrentHashMap); bitmap
 * logo memoize theo path trong [bitmapMemo]. Engine chỉ đọc map/bitmap đã memo -> rẻ.
 *
 * Luồng thread:
 *  - ĐỌC (engine): [getCurrentName]/[getCurrentLogoBitmap] gọi trên worker-thread loader của engine
 *    (đã là background) -> [ensureLoaded] chặn đọc DB 1 lần ở đó là an toàn.
 *  - GHI (app): mọi hàm ghi cập nhật [cache] NGAY trên thread gọi (không đụng DB) để lần reload kế
 *    (debounce 250ms qua LauncherReloadScheduler) đọc được giá trị mới; phần ghi DB đẩy sang [io]
 *    (single-thread) nên KHÔNG chạm DB trên main-thread.
 */
object AppOverrideStore {

    private val io = Executors.newSingleThreadExecutor()
    private val cache = ConcurrentHashMap<String, AppOverrideEntity>()
    private val bitmapMemo = ConcurrentHashMap<String, Bitmap>()

    @Volatile
    private var dao: AppOverrideDao? = null

    @Volatile
    private var loaded = false

    /** Khởi tạo lazy (idempotent). Chỉ lấy DAO, KHÔNG đọc DB ở đây. */
    fun init(context: Context) {
        if (dao == null) {
            synchronized(this) {
                if (dao == null) {
                    dao = AppOverrideDatabase.get(context.applicationContext).dao()
                }
            }
        }
    }

    /** Nạp toàn bộ bảng vào cache 1 lần. An toàn gọi từ background thread (đọc DB). */
    private fun ensureLoaded() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val d = dao ?: return
            try {
                for (e in d.getAll()) cache[e.componentFlatten] = e
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            loaded = true
        }
    }

    // ---- ĐỌC (engine) ---------------------------------------------------------------------

    /** Tên đang áp dụng cho [flatten], hoặc null nếu chưa có row / rỗng (engine dùng nhãn gốc). */
    fun getCurrentName(flatten: String): String? {
        ensureLoaded()
        val name = cache[flatten]?.currentName
        return if (name.isNullOrEmpty()) null else name
    }

    /** Bitmap logo custom đang áp dụng cho [flatten] (memoize theo path), hoặc null nếu chưa đặt. */
    fun getCurrentLogoBitmap(flatten: String): Bitmap? {
        ensureLoaded()
        val path = cache[flatten]?.currentLogoPath ?: return null
        bitmapMemo[path]?.let { return it }
        return try {
            BitmapFactory.decodeFile(path)?.also { bitmapMemo[path] = it }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    // ---- GHI (app) ------------------------------------------------------------------------

    /** Tạo row lần đầu (current = default, logo null). Không làm gì nếu đã có. Chỉ chạy trên [io]. */
    fun ensureSeeded(flatten: String, packageName: String, defaultName: String) {
        io.execute {
            ensureLoaded()
            if (cache.containsKey(flatten)) return@execute
            val e = AppOverrideEntity(flatten, packageName, defaultName, defaultName, null, null)
            cache[flatten] = e
            try {
                dao?.upsert(e)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    /** Đặt tên đang áp dụng. Cập map ngay (nếu có), ghi DB trên [io]. */
    fun setCurrentName(flatten: String, name: String) {
        cache[flatten]?.let { cache[flatten] = it.copy(currentName = name) }
        io.execute {
            ensureLoaded()
            val cur = cache[flatten] ?: return@execute
            cache[flatten] = cur.copy(currentName = name)
            try {
                dao?.updateCurrentName(flatten, name)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    /** Đưa tên về default. */
    fun resetName(flatten: String) {
        cache[flatten]?.let { cache[flatten] = it.copy(currentName = it.defaultName) }
        io.execute {
            ensureLoaded()
            val cur = cache[flatten] ?: return@execute
            cache[flatten] = cur.copy(currentName = cur.defaultName)
            try {
                dao?.updateCurrentName(flatten, cur.defaultName)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    /** Đặt path logo custom đang áp dụng. Cập map ngay + xoá memo path, ghi DB trên [io]. */
    fun setCurrentLogoPath(flatten: String, path: String) {
        bitmapMemo.remove(path)
        cache[flatten]?.let { cache[flatten] = it.copy(currentLogoPath = path) }
        io.execute {
            ensureLoaded()
            val cur = cache[flatten] ?: return@execute
            cache[flatten] = cur.copy(currentLogoPath = path)
            try {
                dao?.updateCurrentLogoPath(flatten, path)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    /** Đưa logo về default (null = icon hệ thống). */
    fun resetLogo(flatten: String) {
        cache[flatten]?.let {
            it.currentLogoPath?.let { p -> bitmapMemo.remove(p) }
            cache[flatten] = it.copy(currentLogoPath = null)
        }
        io.execute {
            ensureLoaded()
            val cur = cache[flatten] ?: return@execute
            cur.currentLogoPath?.let { bitmapMemo.remove(it) }
            cache[flatten] = cur.copy(currentLogoPath = null)
            try {
                dao?.updateCurrentLogoPath(flatten, null)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}
