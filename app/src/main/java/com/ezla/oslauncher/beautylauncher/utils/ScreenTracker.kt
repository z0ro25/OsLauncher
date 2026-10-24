package com.ezla.oslauncher.beautylauncher.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.truongnt.ios.ioslite.common.analytics.AnalyticsDelegate

// Tự log mở màn (Activity) + tab/dialog (Fragment) vào Firebase, khỏi gắn tay từng màn.
// Chỉ log lần tạo đầu (savedInstanceState == null): xoay màn / dựng lại không tính thêm.
object ScreenTracker {

    private const val PKG_APP = "com.ezla.oslauncher.beautylauncher."
    private const val PKG_THEMES = "com.ezla.oslauncher.themes."
    private const val PKG_LAUNCHER = "com.truongnt.ios."
    private const val MAX_EVENT_LEN = 40 // giới hạn tên event của Firebase

    // Desktop: tên class là SearchLauncher (kế thừa Launcher) -> đặt tên dễ đọc.
    private val OVERRIDES = mapOf(
        "com.truongnt.ios.launcher.searchlauncher.SearchLauncher" to "screen_desktop"
    )

    fun register(app: Application) {
        val analytics = FirebaseAnalytics.getInstance(app)
        AnalyticsDelegate.setDialogLogger { className -> log(analytics, className) }
        val fragmentCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
                if (savedInstanceState == null) log(analytics, f.javaClass.name)
            }
        }
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (savedInstanceState == null) log(analytics, activity.javaClass.name)
                (activity as? FragmentActivity)?.supportFragmentManager
                    ?.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun log(analytics: FirebaseAnalytics, className: String) {
        val name = eventName(className) ?: return
        analytics.logEvent(name, null)
    }

    // null = class ngoài package của mình (SDK quảng cáo, Glide...) -> không log.
    fun eventName(className: String): String? {
        OVERRIDES[className]?.let { return it }
        val prefix = when {
            className.startsWith(PKG_THEMES) -> "themes_"
            className.startsWith(PKG_APP) -> ""
            className.startsWith(PKG_LAUNCHER) -> "launcher_"
            else -> return null
        }
        val simple = className.substringAfterLast('.').substringBefore('$')
        if (simple.isEmpty()) return null
        val isDialog = simple.endsWith("Dialog")
        val kind = if (isDialog) "dialog_" else "screen_"
        val core = simple
            .removeSuffix("Activity").removeSuffix("Fragment").removeSuffix("Dialog").removeSuffix("Act")
            .ifEmpty { simple }
        val snake = core.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
            .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1_$2")
            .lowercase()
        return (prefix + kind + snake).take(MAX_EVENT_LEN)
    }
}
