package com.ezla.oslauncher.beautylauncher.Features.languageStart

//import com.ezla.oslauncher.beautylauncher.launchActivity
import android.view.LayoutInflater
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.Features.intro.IntroActivity
import com.ezla.oslauncher.beautylauncher.databinding.ActivityLanguageStartBinding
import com.ezla.oslauncher.beautylauncher.extensions.launchActivity
import com.ezla.oslauncher.beautylauncher.model.LanguageModel
import com.ezla.oslauncher.beautylauncher.tool.languageTool.LanguageUtil
import com.truongnt.ios.ioslite.common.R
import com.truongnt.ios.ioslite.common.ads.Ads
import com.truongnt.ios.ioslite.common.ads.AdsError
import com.truongnt.ios.ioslite.common.ads.AdsNative
import com.truongnt.ios.ioslite.common.ads.AdsNativeCallback
import com.truongnt.ios.ioslite.common.ads.AdsSlot
import com.truongnt.ios.ioslite.common.config.RemoteConfigs
import com.truongnt.ios.ioslite.common.config.SharePrefUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale


class LanguageStartActivity : BaseActivity<ActivityLanguageStartBinding>() {

    var codeLang = "en"
    var listLanguage: ArrayList<LanguageModel>? = arrayListOf()

    override val setViewBinding: ActivityLanguageStartBinding
        get() = ActivityLanguageStartBinding.inflate(LayoutInflater.from(this))


    companion object{
        var nativeOnb1 = true
        var nativeOnb2 = true
        var nativeOnb3 = true
        var nativeOnbFull = true
    }

    override fun initView() {
        initData()
        val linearLayoutManager = LinearLayoutManager(this)
        val languageAdapter = LanguageStartAdapter(listLanguage, { code ->

            MainScope().launch {
                delay(2000)

                binding.frNative.removeAllViews()
                // Remote Config tắt native ở vị trí này -> không hiện ad, giấu luôn ô chứa để
                // không chừa khoảng trống. Vẫn phải chạy tiếp phần hiện nút Done bên dưới.
                if (RemoteConfigs.isAdsEnabled(
                        this@LanguageStartActivity,
                        RemoteConfigs.NATIVE_LANG2
                    )
                ) {
                    // Slot NATIVE_LANGUAGE_2 (không phải _1): vị trí này dùng layout số 2 và
                    // splash đã preload sẵn slot _2 cho nó — trước đây truyền nhầm _1.
                    AdsNative.show(
                        binding.frNative,
                        AdsSlot.NATIVE_LANGUAGE_2,
                        R.layout.layout_native_language_2,
                        object : AdsNativeCallback() {
                            override fun onLoadFailed(error: AdsError) {
                                super.onLoadFailed(error)
                                binding.frNative.isVisible = false
                            }
                        })
                } else {
                    binding.frNative.isVisible = false
                }

                if (!binding.ivDone.isVisible) {
                    binding.ivDone.isVisible = true
                }
            }

            codeLang = code
        }, this)

//        languageAdapter.setCheck(codeLang)

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = languageAdapter

//        languageAdapter.setCheck(codeLang)

        binding.ivDone.setOnClickListener { v ->
            LanguageUtil.saveLocale(baseContext, codeLang)
            launchActivity<IntroActivity> { }
        }

        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }

        MainScope().launch {
            delay(100)

            // Native hiện ngay khi vào màn — gác theo cờ Remote Config của vị trí.
            if (RemoteConfigs.isAdsEnabled(
                    this@LanguageStartActivity,
                    RemoteConfigs.NATIVE_LANG1
                )
            ) {
                AdsNative.show(
                    binding.frNative,
                    AdsSlot.NATIVE_LANGUAGE_1,
                    R.layout.layout_native_language_1,
                    object : AdsNativeCallback() {
                        override fun onLoadFailed(error: AdsError) {
                            super.onLoadFailed(error)
                            binding.frNative.isVisible = false
                        }
                    })
            } else {
                binding.frNative.isVisible = false
            }
        }

        preloadAds()
    }

    private fun preloadAds() {
        if (RemoteConfigs.isAdsEnabled(RemoteConfigs.NATIVE_ONB1)){
            AdsNative.preload(this, AdsSlot.NATIVE_ONBOARDING_1,object : AdsNativeCallback(){
                override fun onLoaded() {
                    super.onLoaded()
                    nativeOnb1 = true
                }

                override fun onLoadFailed(error: AdsError) {
                    super.onLoadFailed(error)
                    nativeOnb1 = false
                }
            })
        }

        if (RemoteConfigs.isAdsEnabled(RemoteConfigs.NATIVE_ONB2)){
            AdsNative.preload(this, AdsSlot.NATIVE_ONBOARDING_2,object : AdsNativeCallback(){
                override fun onLoaded() {
                    super.onLoaded()
                    nativeOnb2 = true
                }

                override fun onLoadFailed(error: AdsError) {
                    super.onLoadFailed(error)
                    nativeOnb2 = false
                }
            })
        }

        if (RemoteConfigs.isAdsEnabled(RemoteConfigs.NATIVE_ONB3)){
            AdsNative.preload(this, AdsSlot.NATIVE_ONBOARDING_3,object : AdsNativeCallback(){
                override fun onLoaded() {
                    super.onLoaded()
                    nativeOnb3 = true
                }

                override fun onLoadFailed(error: AdsError) {
                    super.onLoadFailed(error)
                    nativeOnb3 = false
                }
            })
        }

        if (RemoteConfigs.isAdsEnabled(RemoteConfigs.NATIVE_ONB_FULL)){
            AdsNative.preload(this, AdsSlot.NATIVE_ONBOARDING_FULL,object : AdsNativeCallback(){
                override fun onLoaded() {
                    super.onLoaded()
                    nativeOnbFull = true
                }

                override fun onLoadFailed(error: AdsError) {
                    super.onLoadFailed(error)
                    nativeOnbFull = false
                }
            })
        }
    }

    override fun viewListener() {}

    override fun dataObservable() {}

    private fun initData() {
        listLanguage = arrayListOf()
        codeLang =
            if (LanguageUtil.getPreLanguage(this) == null || LanguageUtil.getPreLanguage(this)
                    .isEmpty()
            ) {
                Locale.getDefault().language
            } else LanguageUtil.getPreLanguage(this)
        // Bộ ngôn ngữ thống nhất với màn cài đặt (LanguageSettingActivity): en/hi/es/fr/de/ko/pt.
        // Bỏ Indonesian (không thuộc danh sách), thêm Korean cho khớp — xem Localize note.
        listLanguage!!.add(LanguageModel("English", "en"))
        listLanguage!!.add(LanguageModel("Hindi", "hi"))
        listLanguage!!.add(LanguageModel("Spanish", "es"))
        listLanguage!!.add(LanguageModel("French", "fr"))
        listLanguage!!.add(LanguageModel("German", "de"))
        listLanguage!!.add(LanguageModel("Korean", "ko"))
        listLanguage!!.add(LanguageModel("Portuguese", "pt"))


        val model = listLanguage?.lastOrNull { it.code == codeLang }
        if (model == null) {
            codeLang = "en"
        }

        for (i in listLanguage!!.indices) {
            if (listLanguage!![i].code == codeLang) {
                listLanguage!!.add(0, listLanguage!![i])
                listLanguage!!.removeAt(i + 1)
            }
        }
    }

    override fun onBackPressed() {
        finishAffinity()
        super.onBackPressed()
    }

}