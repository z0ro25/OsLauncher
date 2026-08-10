package com.oslauncher.applauncher.themelauncher.Features.general.applibrary.viewmodels

import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.data.AppLibraryRepository
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.data.InstalledAppItem
import java.text.Collator

/**
 * Nạp app THẬT đã cài (qua LauncherApps, chạy background) + gắn nhãn folder/category hiện tại từ repo
 * (ĐỒNG BỘ 2 chiều với grid engine).
 */
class AppLibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppLibraryRepository(app)
    val appsLiveData: MutableLiveData<List<InstalledAppItem>> = MutableLiveData()

    private var cache: MutableList<InstalledAppItem> = mutableListOf()

    /** Nạp toàn bộ app đã cài (chạy 1 lần khi mở màn). */
    fun loadInstalledApps() {
        Thread {
            val ctx = getApplication<Application>()
            val launcherApps =
                ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val list = mutableListOf<InstalledAppItem>()
            try {
                val activities = launcherApps.getActivityList(null, Process.myUserHandle())
                for (info in activities) {
                    val cn = info.componentName
                    val flatten = cn.flattenToString()
                    val category = info.applicationInfo.category
                    list.add(
                        InstalledAppItem(
                            componentFlatten = flatten,
                            packageName = cn.packageName,
                            label = info.label.toString(),
                            icon = info.getIcon(0),
                            appCategory = category,
                            folderLabel = repo.labelFor(flatten, category)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val collator = Collator.getInstance()
            list.sortWith(compareBy(collator) { it.label })
            cache = list
            appsLiveData.postValue(list)
        }.start()
    }

    /** Chỉ cập nhật lại nhãn folder/category (gọi ở onResume sau khi gán) — không nạp lại toàn bộ. */
    fun refreshLabels() {
        if (cache.isEmpty()) return
        cache.forEach { it.folderLabel = repo.labelFor(it.componentFlatten, it.appCategory) }
        appsLiveData.postValue(cache.toList())
    }
}
