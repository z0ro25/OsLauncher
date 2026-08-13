package com.oslauncher.applauncher.themelauncher.Features.general.changeicon.viewmodels

import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.amz.ios.launcher.appoverride.AppOverrideStore
import com.oslauncher.applauncher.themelauncher.Features.general.changeicon.data.ChangeAppIconRepository
import com.oslauncher.applauncher.themelauncher.Features.general.changeicon.data.ChangeIconItem
import java.text.Collator

/**
 * Nạp toàn bộ app THẬT đã cài (qua LauncherApps, chạy background) cho màn Change App Icon.
 * displayLabel = tên custom (nếu user đã đổi tên ở màn rename) ngược lại = nhãn gốc hệ thống,
 * để tiêu đề khớp tên đang hiển thị ngoài desktop. Icon trong list ưu tiên ảnh custom đã đặt
 * (khớp icon ngoài desktop), ngược lại là icon gốc hệ thống.
 */
class ChangeAppIconViewModel(app: Application) : AndroidViewModel(app) {

    val appsLiveData: MutableLiveData<List<ChangeIconItem>> = MutableLiveData()

    private val iconRepo = ChangeAppIconRepository(app)

    /** Nạp toàn bộ app đã cài (chạy 1 lần khi mở màn). */
    fun loadInstalledApps() {
        Thread {
            val ctx = getApplication<Application>()
            val launcherApps = ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val list = mutableListOf<ChangeIconItem>()
            try {
                val activities = launcherApps.getActivityList(null, Process.myUserHandle())
                for (info in activities) {
                    val cn = info.componentName
                    val flatten = cn.flattenToString()
                    val defaultLabel = info.label.toString()
                    // Seed row (current=default, logo null) nếu chưa có.
                    AppOverrideStore.ensureSeeded(flatten, cn.packageName, defaultLabel)
                    // App đang hiển thị icon iOS ngoài desktop -> seed logo iOS vào current (đồng bộ đổi logo).
                    iconRepo.seedIosThemeIconIfNeeded(flatten)
                    val customLabel = AppOverrideStore.getCurrentName(flatten)
                    val customIcon = iconRepo.customIconFile(flatten)
                        ?.let { Drawable.createFromPath(it.absolutePath) }
                    list.add(
                        ChangeIconItem(
                            componentFlatten = flatten,
                            packageName = cn.packageName,
                            displayLabel = customLabel ?: defaultLabel,
                            icon = customIcon ?: info.getIcon(0)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val collator = Collator.getInstance()
            list.sortWith(compareBy(collator) { it.displayLabel })
            appsLiveData.postValue(list)
        }.start()
    }
}
