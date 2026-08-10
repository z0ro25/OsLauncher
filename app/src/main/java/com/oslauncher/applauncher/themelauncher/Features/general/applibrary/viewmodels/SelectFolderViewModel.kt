package com.oslauncher.applauncher.themelauncher.Features.general.applibrary.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.data.AppLibraryRepository
import com.oslauncher.applauncher.themelauncher.R

/**
 * Dựng danh sách chọn cho màn Select Folder: [Other] + folder custom + category cố định.
 * Lưu/đọc assignment cho 1 app (theo component flatten).
 */
class SelectFolderViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppLibraryRepository(app)

    /** Danh sách (targetId, label) đầy đủ theo thứ tự hiển thị. */
    fun buildTargets(): List<Pair<String, String>> {
        val ctx = getApplication<Application>()
        val result = mutableListOf<Pair<String, String>>()
        // Other (mặc định) luôn đầu tiên.
        result.add(AppLibraryRepository.TARGET_OTHER to ctx.getString(R.string.other))
        // Folder custom (theo order).
        repo.getFolders().forEach { result.add(it.id to it.name) }
        // Category cố định.
        val ids = ctx.resources.getStringArray(R.array.applib_fixed_category_ids)
        val labels = ctx.resources.getStringArray(R.array.applib_fixed_categories)
        for (i in ids.indices) result.add(ids[i] to labels[i])
        return result
    }

    fun getCurrentTarget(componentFlatten: String, appCategory: Int): String =
        repo.resolveTarget(componentFlatten, appCategory)

    fun assign(componentFlatten: String, targetId: String, appCategory: Int) {
        repo.assign(componentFlatten, targetId, appCategory)
    }
}
