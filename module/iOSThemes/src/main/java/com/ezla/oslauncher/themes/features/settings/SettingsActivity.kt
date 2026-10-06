package com.ezla.oslauncher.themes.features.settings

import com.ezla.oslauncher.themes.Models.SettingRow
import com.ezla.oslauncher.themes.Models.SettingItem
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.databinding.ThemesActivitySettingsBinding

// Màn Settings (Figma "Settings"). Chỉ dựng UI: các hành động đi qua SettingsCallback, chưa nối.
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
        binding.layoutPremium.setOnClickListener { callback.onPremiumClick(this) }
    }

    override fun dataObservable() {}

    // Không có dòng ngôn ngữ: đổi ngôn ngữ dùng chung màn Language của :app.
    private fun rows() = listOf(
        SettingRow(SettingItem.RATE, R.drawable.themes_ic_setting_rate, R.string.themes_setting_rate),
        SettingRow(SettingItem.CONTACT, R.drawable.themes_ic_setting_contact, R.string.themes_setting_contact),
        SettingRow(SettingItem.UPDATE, R.drawable.themes_ic_setting_update, R.string.themes_setting_update),
        SettingRow(SettingItem.SHARE, R.drawable.themes_ic_setting_share, R.string.themes_setting_share),
        SettingRow(SettingItem.PRIVACY, R.drawable.themes_ic_setting_privacy, R.string.themes_setting_privacy),
        SettingRow(SettingItem.SUBSCRIPTION, R.drawable.themes_ic_setting_subscription, R.string.themes_setting_subscription)
    )

    private fun onRowClick(item: SettingItem) {
        when (item) {
            SettingItem.RATE -> RateDialog(this) { stars -> callback.onRateSubmit(this, stars) }.show()
            else -> callback.onItemClick(this, item)
        }
    }

    private fun appVersion(): String = try {
        val info = packageManager.getPackageInfo(packageName, 0)
        "${info.versionName}(${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info)})"
    } catch (e: Exception) {
        ""
    }
}
