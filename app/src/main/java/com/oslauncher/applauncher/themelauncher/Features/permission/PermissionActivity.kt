package com.oslauncher.applauncher.themelauncher.Features.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.addCallback
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.home.HomeActivity
import com.oslauncher.applauncher.themelauncher.databinding.ActivityPermissionBinding
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils
import com.oslauncher.applauncher.themelauncher.utils.PermissionManager

class PermissionActivity : BaseActivity<ActivityPermissionBinding>() {
    override val setViewBinding: ActivityPermissionBinding
        get() = ActivityPermissionBinding.inflate(layoutInflater)

    override fun initView() {
//        loadNative()

        PermissionManager.initLauncher(this)

        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()

        binding.apply {
           swPer.isChecked = PermissionManager.isExternalPermission(this@PermissionActivity)
            swPer.isEnabled = !PermissionManager.isExternalPermission(this@PermissionActivity)
        }
    }


    override fun viewListener() {
        binding.apply {
            swPer.setOnClickListener {
                if (!PermissionManager.isExternalPermission(this@PermissionActivity)){
                    PermissionManager.requestExternalPermission(this@PermissionActivity,object : PermissionManager.PermissionResultListener{
                        override fun onDenied() {
                            super.onDenied()
                            swPer.isChecked = false
                        }

                        override fun onAllow() {
                            super.onAllow()
                            swPer.isChecked = true
                        }

                        override fun goSetting() {
                            super.goSetting()
                            val packageName = packageName
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.parse("package:$packageName")
                            startActivity(intent)
                        }
                    })
                }
            }


            tvContinue.setOnClickListener {
                SharePrefUtils.putBoolean(this@PermissionActivity, "PERMISSION_SHOWED", true)
                launchActivity<HomeActivity> { }
            }
        }
    }

    override fun dataObservable() {

    }


//    fun loadNative() {
//        if (haveNetworkConnection() && SharePrefUtils.getBoolean(this, Constant.AdsKey.NATIVE_PER)){
//            binding.frNative.isVisible = true
//            val nativeBuilder = NativeBuilder(
//                this,
//                binding.frNative,
//                R.layout.shimmer_native_permisison,
//                R.layout.layout_native_per
//            )
//
//            nativeBuilder.setListIdAd(AdmobApi.getInstance().getListIDByName(Constant.AdsKey.NATIVE_PER))
//
//            val nativeManager = NativeManager(
//                this,
//                this,
//                nativeBuilder,
//                binding.frNative,
//                R.layout.shimmer_native_permisison,
//                R.layout.layout_native_meta_lang
//            )
//
//            nativeManager.setAlwaysReloadOnResume(true)
//
//            val timeInterval = SharePrefUtils.getLong(
//                this,
//                Constant.AdsKey.INTERVAL_RELOAD_NATIVE
//            )
//            nativeManager.setIntervalReloadNative(
//                ( timeInterval* 1000).toInt()
//            )
//        }else  binding.frNative.isVisible = false
//    }
}