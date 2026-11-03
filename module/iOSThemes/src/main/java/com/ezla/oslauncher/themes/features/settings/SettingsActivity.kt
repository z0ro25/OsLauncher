package com.ezla.oslauncher.themes.features.settings

import android.view.LayoutInflater
import com.ezla.oslauncher.themes.R
import com.ezla.oslauncher.themes.base.BaseActivity
import com.ezla.oslauncher.themes.databinding.ThemesActivitySettingsBinding

// Màn Settings (Figma "Settings") dạng màn riêng; nội dung + logic dùng chung với tab Setting của Home
// qua SettingsController.
class SettingsActivity : BaseActivity<ThemesActivitySettingsBinding>() {
    override val setViewBinding: ThemesActivitySettingsBinding
        get() = ThemesActivitySettingsBinding.inflate(LayoutInflater.from(this))

    var callback: SettingsCallback = object : SettingsCallback {}

    private val controller by lazy { SettingsController(this, binding.content, callback) }

    override fun initView() {
        binding.header.tvHeaderTitle.setText(R.string.themes_settings_title)
        controller.setup()
    }

    override fun viewListener() {
        binding.header.imgBack.setOnClickListener { finish() }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        controller.onResume()
    }
}
