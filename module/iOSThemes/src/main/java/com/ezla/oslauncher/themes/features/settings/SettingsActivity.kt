package com.ezla.oslauncher.themes.features.settings

import com.ezla.oslauncher.themes.Models.SettingRow
import com.ezla.oslauncher.themes.Models.SettingItem
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.ThemesEntry
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.data.SubsRepository
import com.ezla.oslauncher.themes.databinding.ThemesActivitySettingsBinding
import com.truongnt.ios.ioslite.common.config.SharePrefUtils
import com.truongnt.ios.rate.LauncherSharePrefUtils

// Màn Settings (Figma "Settings"). Premium / Manage subscriptions mở màn Subs (:app); Rate us mở
// dialog rate của :app; Contact / Update / Share / Privacy làm giống Settings + Home của :app.
class SettingsActivity : BaseActivity<ThemesActivitySettingsBinding>() {
    override val setViewBinding: ThemesActivitySettingsBinding
        get() = ThemesActivitySettingsBinding.inflate(LayoutInflater.from(this))

    var callback: SettingsCallback = object : SettingsCallback {}

    private val adapter = SettingRowAdapter { onRowClick(it) }

    override fun initView() {
        binding.header.tvHeaderTitle.setText(R.string.themes_settings_title)
        binding.rvSettings.layoutManager = LinearLayoutManager(this)
        binding.rvSettings.adapter = adapter
        adapter.submit(rows())
        binding.tvVersion.text = appVersion()
    }

    override fun viewListener() {
        binding.header.imgBack.setOnClickListener { finish() }
        binding.layoutPremium.setOnClickListener {
            callback.onPremiumClick(this)
            ThemesEntry.openPremium(this)
        }
    }

    override fun dataObservable() {}

    // Đã subs thì ẩn banner Premium; đọc lại mỗi lần quay về (có thể vừa mua ở màn Subs).
    override fun onResume() {
        super.onResume()
        binding.layoutPremium.visibility =
            if (SubsRepository.isSubscribed(this)) View.GONE else View.VISIBLE
    }

    // Không có dòng ngôn ngữ: đổi ngôn ngữ dùng chung màn Language của :app.
    // Đã rate (cờ chung với :app + launcher) thì ẩn dòng Rate, giống Settings của :app.
    private fun rows() = listOfNotNull(
        SettingRow(SettingItem.RATE, R.drawable.themes_ic_setting_rate, R.string.themes_setting_rate)
            .takeUnless { isRated() },
        SettingRow(SettingItem.CONTACT, R.drawable.themes_ic_setting_contact, R.string.themes_setting_contact),
        SettingRow(SettingItem.UPDATE, R.drawable.themes_ic_setting_update, R.string.themes_setting_update),
        SettingRow(SettingItem.SHARE, R.drawable.themes_ic_setting_share, R.string.themes_setting_share),
        SettingRow(SettingItem.PRIVACY, R.drawable.themes_ic_setting_privacy, R.string.themes_setting_privacy),
        SettingRow(SettingItem.SUBSCRIPTION, R.drawable.themes_ic_setting_subscription, R.string.themes_setting_subscription)
    )

    private fun isRated() = SharePrefUtils.isRated(this) || LauncherSharePrefUtils.isRated(this)

    private fun onRowClick(item: SettingItem) {
        when (item) {
            // Dialog rate dùng chung của :app (RatingDialog + In-App Review); rate xong ẩn dòng Rate.
            SettingItem.RATE -> if (!isRated()) {
                ThemesEntry.openRate(this) { if (!isFinishing) adapter.submit(rows()) }
            }
            // Manage subscriptions mở màn Subs của :app (không mở trang quản lý gói của Play).
            SettingItem.SUBSCRIPTION -> {
                callback.onItemClick(this, item)
                ThemesEntry.openPremium(this)
            }
            SettingItem.CONTACT -> {
                callback.onItemClick(this, item)
                sendFeedbackMail()
            }
            SettingItem.UPDATE -> {
                callback.onItemClick(this, item)
                openPlayStore()
            }
            SettingItem.SHARE -> {
                callback.onItemClick(this, item)
                shareApp()
            }
            SettingItem.PRIVACY -> {
                callback.onItemClick(this, item)
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
        startSafely(Intent.createChooser(intent, getString(R.string.themes_setting_contact)))
    }

    // Check app updates: mở trang app trên Play Store; máy không có Play thì mở bằng trình duyệt.
    // Thử thẳng market:// thay vì resolveActivity (Android 11+ cần <queries> mới thấy Play).
    private fun openPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl())))
        }
    }

    // Giống Share của :app: tên app + link Play Store.
    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appName())
            putExtra(Intent.EXTRA_TEXT, "${appName()}\n${playStoreUrl()}")
        }
        startSafely(Intent.createChooser(intent, getString(R.string.themes_cd_share)))
    }

    private fun playStoreUrl() = "https://play.google.com/store/apps/details?id=$packageName"

    // Tên app lấy từ label của :app (module không thấy R.string.app_name của :app).
    private fun appName(): String = applicationInfo.loadLabel(packageManager).toString()

    // Không có app nào xử lý intent (không có app mail / trình duyệt) -> báo thay vì crash.
    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.themes_no_app_to_open, Toast.LENGTH_SHORT).show()
        }
    }

    private fun appVersion(): String = try {
        val info = packageManager.getPackageInfo(packageName, 0)
        "${info.versionName}(${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info)})"
    } catch (e: Exception) {
        ""
    }

    private companion object {
        // Cùng link Privacy với Home / màn Subs của :app.
        const val URL_PRIVACY =
            "https://docs.google.com/document/d/1MQhESaXwlgu5Gx9JWXSfXQMGBaaWCJBs-ochf5Cng3Y/edit?tab=t.0"
    }
}
