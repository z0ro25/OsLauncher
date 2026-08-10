package com.oslauncher.applauncher.themelauncher.Features.general.applibrary.data

import android.content.Context
import android.content.Intent
import com.amz.ios.launcher.Launcher
import com.amz.ios.launcher.ota.IOSOtaHandler
import com.oslauncher.applauncher.themelauncher.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Lưu trữ cho tính năng App Library (4 màn), ĐỒNG BỘ 2 CHIỀU category với App Library grid của engine.
 *
 * Hai kho tách vai trò:
 *  1. Category (8 mục cố định + Other) = KHO ENGINE, prefs [Launcher.PREF_KEY] ("com.aisoft.iosLauncher"),
 *     key = component.flatten, value int (Games=1..Productivity=8, Others=9; 0 = chưa gán tay -> engine
 *     tự suy từ ApplicationInfo.category). KHÔNG sửa file engine — chỉ ghi prefs + phát broadcast
 *     [IOSOtaHandler.ACTION_UPDATE_CATEGORY] để launcher đang chạy cập nhật IconDB + rebind grid ngay.
 *  2. Folder custom (grid engine không có khái niệm này) = prefs RIÊNG [PREFS] "applib_settings":
 *     - [KEY_FOLDERS]     : JSON array folder [{id,name,order}].
 *     - [KEY_ASSIGNMENTS] : JSON object {component_flatten -> folderId} (CHỈ chứa app gán folder custom).
 *
 * App gán folder custom được đặt engine = [CATEGORY_OTHERS] (9) nên nằm ở nhóm "Other" bên grid.
 */
class AppLibraryRepository(context: Context) {

    private val appCtx = context.applicationContext
    private val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val enginePrefs = appCtx.getSharedPreferences(Launcher.PREF_KEY, Context.MODE_PRIVATE)

    // ------------------------------------------------------------------
    // Folder CRUD (prefs riêng)
    // ------------------------------------------------------------------

    /** Danh sách folder custom, đã sort theo order tăng dần. */
    fun getFolders(): MutableList<AppLibraryFolder> {
        val raw = prefs.getString(KEY_FOLDERS, null) ?: return mutableListOf()
        val list = mutableListOf<AppLibraryFolder>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    AppLibraryFolder(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        order = o.optInt("order", i)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list.sortBy { it.order }
        return list
    }

    private fun saveFolders(folders: List<AppLibraryFolder>) {
        val arr = JSONArray()
        folders.forEachIndexed { index, f ->
            arr.put(
                JSONObject()
                    .put("id", f.id)
                    .put("name", f.name)
                    .put("order", index)
            )
        }
        prefs.edit().putString(KEY_FOLDERS, arr.toString()).apply()
    }

    /** True nếu đã tồn tại folder trùng tên (không phân biệt hoa/thường). */
    fun isFolderNameExists(name: String): Boolean =
        getFolders().any { it.name.equals(name.trim(), ignoreCase = true) }

    /** Thêm folder mới ở cuối danh sách. Trả về folder vừa tạo. */
    fun addFolder(name: String): AppLibraryFolder {
        val folders = getFolders()
        val folder = AppLibraryFolder(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            order = folders.size
        )
        folders.add(folder)
        saveFolders(folders)
        return folder
    }

    fun renameFolder(id: String, newName: String) {
        val folders = getFolders()
        folders.firstOrNull { it.id == id }?.name = newName.trim()
        saveFolders(folders)
    }

    /**
     * Xóa folder + gỡ mọi assignment đang trỏ tới nó. App liên quan giữ engine = Others (9) nên vẫn
     * nằm ở nhóm "Other" bên grid, còn màn App Library hiển thị nhãn "Other".
     */
    fun deleteFolder(id: String) {
        val folders = getFolders().filterNot { it.id == id }
        saveFolders(folders)

        val map = readFolderAssignments()
        val cleaned = JSONObject()
        map.keys().forEach { key ->
            val target = map.getString(key)
            if (target != id) cleaned.put(key, target)
        }
        prefs.edit().putString(KEY_ASSIGNMENTS, cleaned.toString()).apply()
    }

    /** Lưu lại thứ tự sau khi kéo (theo đúng thứ tự id truyền vào). */
    fun reorderFolders(orderedIds: List<String>) {
        val byId = getFolders().associateBy { it.id }
        val reordered = orderedIds.mapNotNull { byId[it] }
        saveFolders(reordered)
    }

    // ------------------------------------------------------------------
    // Folder assignment (prefs riêng) — CHỈ lưu app gán folder custom
    // ------------------------------------------------------------------

    private fun readFolderAssignments(): JSONObject {
        val raw = prefs.getString(KEY_ASSIGNMENTS, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    /** folderId app đang gán, hoặc null nếu không gán folder custom nào. */
    private fun getFolderAssignment(componentFlatten: String): String? {
        val v = readFolderAssignments().optString(componentFlatten, "")
        return v.ifEmpty { null }
    }

    private fun putFolderAssignment(componentFlatten: String, folderId: String) {
        val map = readFolderAssignments()
        map.put(componentFlatten, folderId)
        prefs.edit().putString(KEY_ASSIGNMENTS, map.toString()).apply()
    }

    private fun removeFolderAssignment(componentFlatten: String) {
        val map = readFolderAssignments()
        if (map.has(componentFlatten)) {
            map.remove(componentFlatten)
            prefs.edit().putString(KEY_ASSIGNMENTS, map.toString()).apply()
        }
    }

    // ------------------------------------------------------------------
    // Category engine (đồng bộ 2 chiều)
    // ------------------------------------------------------------------

    /** Category gán tay ở kho engine (0 = chưa gán -> engine tự suy từ ApplicationInfo.category). */
    private fun getEngineCategory(componentFlatten: String): Int =
        try {
            enginePrefs.getInt(componentFlatten, 0)
        } catch (e: Exception) {
            0
        }

    /**
     * Ghi category vào kho engine + phát broadcast để launcher đang chạy cập nhật ngay.
     * Ghi prefs trực tiếp đảm bảo bền kể cả khi launcher chưa khởi tạo (broadcast bị bỏ qua);
     * khi grid load lần sau vẫn đọc đúng từ prefs.
     */
    private fun setEngineCategory(componentFlatten: String, category: Int) {
        enginePrefs.edit().putInt(componentFlatten, category).apply()
        val intent = Intent(IOSOtaHandler.ACTION_UPDATE_CATEGORY).apply {
            setPackage(appCtx.packageName)
            putExtra(IOSOtaHandler.EXTRA_COMPONENT_NAME, componentFlatten)
            putExtra(IOSOtaHandler.EXTRA_APP_CATEGORY, category)
        }
        appCtx.sendBroadcast(intent)
    }

    // ------------------------------------------------------------------
    // Resolve target + gán (dùng chung cho tick / nhãn / lưu)
    // ------------------------------------------------------------------

    /**
     * Target hiển thị/tick hiện tại của app. Ưu tiên folder custom, sau đó tới category engine
     * (gán tay hoặc auto-suy từ [appCategory] = ApplicationInfo.category, -1..7).
     */
    fun resolveTarget(componentFlatten: String, appCategory: Int): String {
        // 1) Folder custom còn tồn tại?
        val folderId = getFolderAssignment(componentFlatten)
        if (folderId != null && getFolders().any { it.id == folderId }) return folderId
        // 2) Category engine (gán tay > auto-suy).
        val raw = getEngineCategory(componentFlatten)
        val eff = if (raw != 0) raw else appCategory + 1  // -1 -> 0 (Others)
        if (eff in 1..8) {
            val ids = appCtx.resources.getStringArray(R.array.applib_fixed_category_ids)
            return ids[eff - 1]
        }
        return TARGET_OTHER
    }

    /**
     * Gán app vào 1 target: "other" | folderId(uuid) | id category ("cat_*"). Đồng bộ engine.
     *  - category cố định -> engine = index+1 (grid nhảy đúng nhóm).
     *  - folder custom     -> engine = Others (9) + lưu folderId ở prefs riêng.
     *  - other             -> engine = Others (9) + gỡ folder.
     */
    fun assign(componentFlatten: String, targetId: String, appCategory: Int) {
        val ids = appCtx.resources.getStringArray(R.array.applib_fixed_category_ids)
        when {
            targetId == TARGET_OTHER -> {
                removeFolderAssignment(componentFlatten)
                setEngineCategory(componentFlatten, CATEGORY_OTHERS)
            }
            ids.contains(targetId) -> {
                removeFolderAssignment(componentFlatten)
                setEngineCategory(componentFlatten, ids.indexOf(targetId) + 1)
            }
            else -> {
                putFolderAssignment(componentFlatten, targetId)
                setEngineCategory(componentFlatten, CATEGORY_OTHERS)
            }
        }
    }

    // ------------------------------------------------------------------
    // Nhãn hiển thị
    // ------------------------------------------------------------------

    /** Nhãn folder/category của app để hiển thị ở màn App Library. */
    fun labelFor(componentFlatten: String, appCategory: Int): String =
        labelForTarget(resolveTarget(componentFlatten, appCategory))

    /** Nhãn hiển thị của 1 targetId bất kỳ. Target không còn tồn tại -> "Other". */
    fun labelForTarget(targetId: String): String {
        if (targetId == TARGET_OTHER) return appCtx.getString(R.string.other)
        // Category cố định?
        val ids = appCtx.resources.getStringArray(R.array.applib_fixed_category_ids)
        val labels = appCtx.resources.getStringArray(R.array.applib_fixed_categories)
        val idx = ids.indexOf(targetId)
        if (idx >= 0) return labels[idx]
        // Folder custom?
        val folder = getFolders().firstOrNull { it.id == targetId }
        return folder?.name ?: appCtx.getString(R.string.other)
    }

    companion object {
        private const val PREFS = "applib_settings"
        private const val KEY_FOLDERS = "applib_folders"
        private const val KEY_ASSIGNMENTS = "applib_assignments"

        const val TARGET_OTHER = "other"

        /** Nhóm "Others" của engine (Games=1..Productivity=8, Others=9). */
        private const val CATEGORY_OTHERS = 9
    }
}
