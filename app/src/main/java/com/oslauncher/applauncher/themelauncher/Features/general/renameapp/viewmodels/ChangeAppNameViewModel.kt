package com.oslauncher.applauncher.themelauncher.Features.general.renameapp.viewmodels

import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.amz.ios.launcher.appoverride.AppOverrideStore
import com.oslauncher.applauncher.themelauncher.Features.general.changeicon.data.ChangeAppIconRepository
import com.oslauncher.applauncher.themelauncher.Features.general.renameapp.data.RenameAppItem
import com.oslauncher.applauncher.themelauncher.Features.general.renameapp.data.RenameAppRepository
import java.text.Collator

/**
 * Nạp toàn bộ app THẬT đã cài (qua LauncherApps, chạy background) cho màn Change App Name.
 * displayLabel = tên custom nếu đã đặt, ngược lại = nhãn gốc hệ thống.
 * icon = icon custom đã đặt ở màn đổi icon (nếu có) để khớp desktop, ngược lại = icon gốc hệ thống.
 */
class ChangeAppNameViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = RenameAppRepository(app)
    // Tái dùng nguồn icon custom CÓ SẴN của màn đổi icon (đọc file custom trong filesDir) — không nhân bản path.
    private val iconRepo = ChangeAppIconRepository(app)
    val appsLiveData: MutableLiveData<List<RenameAppItem>> = MutableLiveData()

    private var cache: MutableList<RenameAppItem> = mutableListOf()
    // Icon gốc hệ thống mỗi component, giữ lại để fallback khi user Reset icon custom.
    private val defaultIcons = HashMap<String, Drawable?>()

    /** Nạp toàn bộ app đã cài (chạy 1 lần khi mở màn). */
    fun loadInstalledApps() {
        Thread {
            val ctx = getApplication<Application>()
            val launcherApps = ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val list = mutableListOf<RenameAppItem>()
            try {
                val activities = launcherApps.getActivityList(null, Process.myUserHandle())
                for (info in activities) {
                    val cn = info.componentName
                    val flatten = cn.flattenToString()
                    val defaultLabel = info.label.toString()
                    val defaultIcon = info.getIcon(0)
                    defaultIcons[flatten] = defaultIcon
                    // Seed row (current=default, logo null) nếu chưa có — nguồn sự thật để reset về gốc.
                    AppOverrideStore.ensureSeeded(flatten, cn.packageName, defaultLabel)
                    // App đang hiển thị icon iOS ngoài desktop -> seed logo iOS vào current (đồng bộ đổi logo).
                    iconRepo.seedIosThemeIconIfNeeded(flatten)
                    list.add(
                        RenameAppItem(
                            componentFlatten = flatten,
                            packageName = cn.packageName,
                            defaultLabel = defaultLabel,
                            displayLabel = repo.getCustomLabel(flatten) ?: defaultLabel,
                            icon = customIconOf(flatten) ?: defaultIcon
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val collator = Collator.getInstance()
            list.sortWith(compareBy(collator) { it.displayLabel })
            cache = list
            appsLiveData.postValue(list)
        }.start()
    }

    /**
     * Cập nhật lại tên + icon hiển thị (gọi ở onResume sau khi quay lại từ màn đổi tên/đổi icon) —
     * không nạp lại toàn bộ. Icon custom được đọc lại từ file, fallback về icon gốc đã lưu khi bị Reset.
     */
    fun refreshLabels() {
        if (cache.isEmpty()) return
        cache.forEach {
            it.displayLabel = repo.getCustomLabel(it.componentFlatten) ?: it.defaultLabel
            it.icon = customIconOf(it.componentFlatten) ?: defaultIcons[it.componentFlatten]
        }
        appsLiveData.postValue(cache.toList())
    }

    /** Icon custom đã đặt cho [flatten] (đọc file trong filesDir), hoặc null nếu chưa đặt. */
    private fun customIconOf(flatten: String): Drawable? =
        iconRepo.customIconFile(flatten)?.let { Drawable.createFromPath(it.absolutePath) }
}
