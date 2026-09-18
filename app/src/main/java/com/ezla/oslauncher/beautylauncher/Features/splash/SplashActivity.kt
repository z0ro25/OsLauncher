package com.ezla.oslauncher.beautylauncher.Features.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.addCallback
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.Features.home.HomeActivity
import com.ezla.oslauncher.beautylauncher.Features.languageStart.LanguageStartActivity
import com.ezla.oslauncher.beautylauncher.databinding.ActivitySplashBinding
import com.ezla.oslauncher.beautylauncher.extensions.haveNetworkConnection
import com.ezla.oslauncher.beautylauncher.extensions.hideNavigation
import com.ezla.oslauncher.beautylauncher.tool.sharePreferenceTool.SharePrefUtils
import com.ezla.oslauncher.beautylauncher.utils.EventTrackingHelper
import com.ezla.oslauncher.beautylauncher.utils.GDPRRequestable
import com.ezla.oslauncher.beautylauncher.utils.RemoteConfigUtils
import com.truongnt.ios.ioslite.common.ads.AdsInterstitial
import com.truongnt.ios.ioslite.common.ads.AdsSlot

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

        EventTrackingHelper.logEvent(this, "splash_open")

        onBackPressedDispatcher.addCallback {

        }

        Handler(Looper.getMainLooper()).postDelayed({ goNext() }, 3000)
        requestUmp {
            AdsInterstitial.showSplash(
                this,
                AdsSlot.INTER_SPLASH
            ) {
                AdsInterstitial.load(this, AdsSlot.INTER_IN_APP) { }
                goNext()
            }
        }
    }

    override fun viewListener() {

    }

    override fun dataObservable() {

    }

    private fun requestUmp(action: () -> Unit) {
        Log.e("ạkshdfkjhasdjkfk", "ump")

        GDPRRequestable.getGdprRequestable(this).setOnRequestGDPRCompleted {
            initRemoteConfig {
                initAds {
                    action.invoke()
                }
            }
        }
        GDPRRequestable.getGdprRequestable(this).requestGDPR()
    }


    private fun initRemoteConfig(action: () -> Unit) {
        Log.e("ạkshdfkjhasdjkfk", "remote")
        RemoteConfigUtils.initRemoteConfig { result ->
            if (haveNetworkConnection()) {
                if (result.isSuccessful) {
                    Log.e("ạkshdfkjhasdjkfk", "success")
                    val update = result.result as Boolean
                    if (update) {
                        Log.e("ạkshdfkjhasdjkfk", "update")
                        RemoteConfigUtils.apply {

                        }
                    }
                }
            }

            action.invoke()
        }
    }


    private fun initAds(action: () -> Unit) {
        AdsInterstitial.preloadSplash(this, AdsSlot.INTER_SPLASH) {


            action.invoke()
        }
    }

    private fun goNext() {
        if (isFinishing) return
        if (SharePrefUtils.getBoolean(this, "PERMISSION_SHOWED", false)) {
            showActivity(HomeActivity::class.java, null)
        } else {
            val bundle = Bundle()
            bundle.putString("screen", "SplashActivity")
            showActivity(LanguageStartActivity::class.java, bundle)
        }
        finish()
    }
}
