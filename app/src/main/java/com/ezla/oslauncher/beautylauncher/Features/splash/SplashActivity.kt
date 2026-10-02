package com.ezla.oslauncher.beautylauncher.Features.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.Features.home.HomeActivity
import com.ezla.oslauncher.beautylauncher.Features.languageStart.LanguageStartActivity
import com.ezla.oslauncher.beautylauncher.databinding.ActivitySplashBinding
import com.ezla.oslauncher.beautylauncher.extensions.haveNetworkConnection
import com.ezla.oslauncher.beautylauncher.extensions.hideNavigation
import com.ezla.oslauncher.beautylauncher.utils.EventTrackingHelper
import com.ezla.oslauncher.beautylauncher.utils.GDPRRequestable
import com.ezla.oslauncher.beautylauncher.utils.RemoteConfigUtils
import com.ezt.v2.ezt.admobdemo.ads.NativeAds
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.ezla.oslauncher.beautylauncher.ads.AppAdsRemote
import com.truongnt.ios.ioslite.common.config.AppAds
import com.truongnt.ios.ioslite.common.config.SharePrefUtils
import com.ezla.oslauncher.themes.features.home.ThemesActivity
import com.ezla.oslauncher.themes.ThemesEntry
import com.truongnt.ios.ioslite.common.Router
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    override val setViewBinding: ActivitySplashBinding
        get() = ActivitySplashBinding.inflate(layoutInflater)

    override fun initView() {
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intent.action != null && intent.action.equals(
                Intent.ACTION_MAIN
            )
        ) {
            finish()
            return
        }
        window?.hideNavigation()


        onBackPressedDispatcher.addCallback {

        }
        lifecycleScope.launch {
            val kit = AppAds.initialize(applicationContext) { AppAdsRemote.fetchPlacements() }
            // Đăng ký App Open cho trang Themes; ở đây vì library không thấy ThemesActivity.
            // Gọi lại sẽ thay toàn bộ danh sách cũ, nên chỉ đăng ký tại Splash.
            kit.enableAppOpenOnForeground(setOf(ThemesActivity::class.java), "return_to_app")

            val initialized = suspendCancellableCoroutine<Boolean> { continuation ->
                AdsSdk.initializeWithConsent(this@SplashActivity) { ready, error ->
                    error?.let { Log.w("Consent", "${it.errorCode}: ${it.message}") }
                    if (continuation.isActive) continuation.resume(ready)
                }
            }
            if (initialized) {
                kit.preloadNativeInline("native_lang1")
                kit.preloadNativeInline("native_lang2")

                lifecycle.withResumed { }
                // JSON của key splash chọn open/inter/native_full/combo.
                val result = kit.showAdFullScreen(this@SplashActivity, "splash")
                Log.d("SplashAds", "$result")
            }
            lifecycle.withResumed {
                goNext()
                finish()
            }
        }


    }

    override fun viewListener() {}
    override fun dataObservable() {}

    private fun goNext() {
        // Mở từ icon Themes trên desktop (Router.startThemesViaSplash): bỏ qua luồng Language/Intro.
        if (intent?.getBooleanExtra(Router.EXTRA_OPEN_THEMES, false) == true) {
            ThemesEntry.open(this)
        } else if (SharePrefUtils.getBoolean(this, "PERMISSION_SHOWED", false)) {
            showActivity(HomeActivity::class.java, null)
        } else {
            val bundle = Bundle()
            bundle.putString("screen", "SplashActivity")
            showActivity(LanguageStartActivity::class.java, bundle)
        }
        finish()
    }

}
