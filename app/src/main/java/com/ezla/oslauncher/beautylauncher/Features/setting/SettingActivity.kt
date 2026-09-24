package com.ezla.oslauncher.beautylauncher.Features.setting

import android.content.Intent
import android.net.Uri
import androidx.activity.addCallback
import androidx.core.view.isVisible
import com.truongnt.ios.rate.LauncherSharePrefUtils
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.Features.lang.LanguageSettingActivity
import com.ezla.oslauncher.beautylauncher.R
import com.ezla.oslauncher.beautylauncher.extensions.launchActivity
import com.ezla.oslauncher.beautylauncher.extensions.tap
import com.ezla.oslauncher.beautylauncher.databinding.ActivitySettingBinding
import com.truongnt.ios.ioslite.common.config.SharePrefUtils

class SettingActivity : BaseActivity<ActivitySettingBinding>() {
    override val setViewBinding: ActivitySettingBinding
        get() = ActivitySettingBinding.inflate(layoutInflater)

    override fun onResume() {
        super.onResume()
        showBannerIfEnabled()
    }

    override fun initView() {

        binding.llRate.isVisible = !SharePrefUtils.isRated(this)
        onBackPressedDispatcher.addCallback {
            finish()
        }
    }

    override fun viewListener() {
        binding.apply {
            ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

            llLangSetting.setOnClickListener {
                launchActivity<LanguageSettingActivity>{

                }
            }
            llShare.tap {
                val intentShare = Intent(Intent.ACTION_SEND)
                intentShare.type = "text/plain"
                intentShare.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                intentShare.putExtra(
                    Intent.EXTRA_TEXT, """
     ${getString(R.string.app_name)}
     https://play.google.com/store/apps/details?id=${packageName}
     """.trimIndent()
                )
                startActivity(Intent.createChooser(intentShare, "Share"))
            }
            llRate.tap {
                if (!SharePrefUtils.isRated(this@SettingActivity) && !LauncherSharePrefUtils.isRated(this@SettingActivity)) {
                    showRateDialog(false) {
                        llRate.isVisible = false
                    }
                }

            }
            llPolicy.tap {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://firebasestorage.googleapis.com/v0/b/asa155-launcher-ios.appspot.com/o/Privacy-Policy.html?alt=media&token=065a4c65-cd82-4198-abb9-f0979bc6c5e7")
                )
                startActivity(browserIntent)
            }
        }
    }

    override fun dataObservable() {

    }
}