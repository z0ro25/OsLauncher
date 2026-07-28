package com.oslauncher.applauncher.themelauncher.Features.no_internet

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.databinding.ActivityNoInternetBinding
import com.oslauncher.applauncher.themelauncher.extensions.tap
import kotlin.system.exitProcess
class NoInternetActivity : BaseActivity<ActivityNoInternetBinding>() {
    override val setViewBinding: ActivityNoInternetBinding
        get() = ActivityNoInternetBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
                exitProcess(0)
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun viewListener() {
        binding.tvTryAgain.tap {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, prompt the user to enable Wi-Fi
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                startActivity(panelIntent)
            } else {
                //AppOpenManager.getInstance().disableAppResumeWithActivity(javaClass)
                // For Android 9 and below, enable Wi-Fi programmatically
                val wifiSettingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
                startActivity(wifiSettingsIntent)
            }
        }
    }

    override fun dataObservable() {

    }
}