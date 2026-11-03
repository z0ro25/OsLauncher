package com.ezla.oslauncher.themes.features.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.themes.Models.SettingItem
import com.ezla.oslauncher.themes.Models.SettingRow
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.ThemesEntry
import com.ezla.oslauncher.themes.data.SubsRepository
import com.ezla.oslauncher.themes.databinding.ThemesViewSettingsContentBinding
import com.truongnt.ios.ioslite.common.IosLocale
import com.truongnt.ios.ioslite.common.config.SharePrefUtils
import com.truongnt.ios.rate.LauncherSharePrefUtils
import java.util.Locale

// Logic nội dung Settings dùng chung cho SettingsActivity và tab Setting. Premium / Manage subscriptions
// mở màn Subs (:app); Rate us mở dialog rate của :app; Contact / Update / Share / Privacy như :app.
class SettingsController(
    private val activity: Activity,
    private val binding: ThemesViewSettingsContentBinding,
    private val callback: SettingsCallback = object : SettingsCallback {}
) {

    private val adapter = SettingRowAdapter { onRowClick(it) }

    fun setup() {
        binding.rvSettings.layoutManager = LinearLayoutManager(activity)
        binding.rvSettings.adapter = adapter
        binding.tvVersion.text = appVersion()
        binding.layoutPremium.setOnClickListener {
            callback.onPremiumClick(activity)
            ThemesEntry.openPremium(activity, SOURCE)
        }
    }

    // Đọc lại mỗi lần hiện: có thể vừa mua subs / vừa đổi ngôn ngữ / vừa rate ở màn khác.
    fun onResume() {
        binding.layoutPremium.visibility =
            if (SubsRepository.isSubscribed(activity)) View.GONE else View.VISIBLE
        adapter.submit(rows())
    }

    // Dòng ngôn ngữ chỉ hiện khi :app có gắn màn Language. Đã rate (cờ chung :app + launcher) thì ẩn Rate.
    private fun rows() = listOfNotNull(
        SettingRow(SettingItem.LANGUAGE, R.drawable.themes_ic_setting_language, R.string.themes_setting_language,
            currentLanguage()).takeIf { ThemesEntry.languageOpener != null },
        SettingRow(SettingItem.RATE, R.drawable.themes_ic_setting_rate, R.string.themes_setting_rate)
            .takeUnless { isRated() },
        SettingRow(SettingItem.CONTACT, R.drawable.themes_ic_setting_contact, R.string.themes_setting_contact),
        SettingRow(SettingItem.UPDATE, R.drawable.themes_ic_setting_update, R.string.themes_setting_update),
        SettingRow(SettingItem.SHARE, R.drawable.themes_ic_setting_share, R.string.themes_setting_share),
        SettingRow(SettingItem.PRIVACY, R.drawable.themes_ic_setting_privacy, R.string.themes_setting_privacy),
        SettingRow(SettingItem.SUBSCRIPTION, R.drawable.themes_ic_setting_subscription, R.string.themes_setting_subscription)
    )

    // Tên ngôn ngữ đang dùng, viết bằng chính ngôn ngữ đó (vd "Español"); chưa chọn = theo máy.
    private fun currentLanguage(): String {
        val code = IosLocale.getSavedLanguage(activity).orEmpty()
        val locale = if (code.isEmpty()) Locale.getDefault() else Locale(code)
        return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
    }

    private fun isRated() = SharePrefUtils.isRated(activity) || LauncherSharePrefUtils.isRated(activity)

    private fun onRowClick(item: SettingItem) {
        when (item) {
            SettingItem.LANGUAGE -> {
                callback.onItemClick(activity, item)
                ThemesEntry.openLanguage(activity)
            }
            // Dialog rate dùng chung của :app (RatingDialog + In-App Review); rate xong ẩn dòng Rate.
            SettingItem.RATE -> if (!isRated()) {
                ThemesEntry.openRate(activity) { if (!activity.isFinishing) adapter.submit(rows()) }
            }
            // Manage subscriptions mở màn Subs của :app (không mở trang quản lý gói của Play).
            SettingItem.SUBSCRIPTION -> {
                callback.onItemClick(activity, item)
                ThemesEntry.openPremium(activity, SOURCE)
            }
            SettingItem.CONTACT -> {
                callback.onItemClick(activity, item)
                sendFeedbackMail()
            }
            SettingItem.UPDATE -> {
                callback.onItemClick(activity, item)
                openPlayStore()
            }
            SettingItem.SHARE -> {
                callback.onItemClick(activity, item)
                shareApp()
            }
            SettingItem.PRIVACY -> {
                callback.onItemClick(activity, item)
                startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(URL_PRIVACY)))
            }
        }
    }

    // Giống "Mail to us" của :app: gửi tới email hỗ trợ (SharePrefUtils), kèm model / Android / version.
    private fun sendFeedbackMail() {
        val recipients = listOf(SharePrefUtils.email, SharePrefUtils.email1)
            .filter { it.isNotBlank() }
            .toTypedArray()
        val body = buildString {
            append("\n\n---\n")
            append("Model: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
            append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
            append("App version: ").append(appVersion())
        }
        // "mailto:" trống + EXTRA_EMAIL: giữ được ký tự đặc biệt ở subject/body mà không phải tự encode.
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, recipients)
            putExtra(Intent.EXTRA_SUBJECT, "${SharePrefUtils.subject}${appName()}")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startSafely(Intent.createChooser(intent, activity.getString(R.string.themes_setting_contact)))
    }

    // Thử thẳng market:// thay vì resolveActivity (Android 11+ cần <queries> mới thấy Play).
    private fun openPlayStore() {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activity.packageName}")))
        } catch (e: ActivityNotFoundException) {
            startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl())))
        }
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appName())
            putExtra(Intent.EXTRA_TEXT, "${appName()}\n${playStoreUrl()}")
        }
        startSafely(Intent.createChooser(intent, activity.getString(R.string.themes_cd_share)))
    }

    private fun playStoreUrl() = "https://play.google.com/store/apps/details?id=${activity.packageName}"

    // Tên app lấy từ label của :app (module không thấy R.string.app_name của :app).
    private fun appName(): String = activity.applicationInfo.loadLabel(activity.packageManager).toString()

    // Không có app nào xử lý intent (không có app mail / trình duyệt) -> báo thay vì crash.
    private fun startSafely(intent: Intent) {
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.themes_no_app_to_open, Toast.LENGTH_SHORT).show()
        }
    }

    private fun appVersion(): String = try {
        val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
        "${info.versionName}(${PackageInfoCompat.getLongVersionCode(info)})"
    } catch (e: Exception) {
        ""
    }

    private companion object {
        const val SOURCE = "themes_settings"
        // Cùng link Privacy với Home / màn Subs của :app.
        const val URL_PRIVACY =
            "https://docs.google.com/document/d/1MQhESaXwlgu5Gx9JWXSfXQMGBaaWCJBs-ochf5Cng3Y/edit?tab=t.0"
    }
}
