package com.ezla.oslauncher.beautylauncher.Features.languageStart

//import com.ezla.oslauncher.beautylauncher.launchActivity
import android.view.LayoutInflater
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.Features.intro.IntroActivity
import com.ezla.oslauncher.beautylauncher.R
import com.ezla.oslauncher.beautylauncher.databinding.ActivityLanguageStartBinding
import com.ezla.oslauncher.beautylauncher.extensions.launchActivity
import com.ezla.oslauncher.beautylauncher.model.LanguageModel
import com.ezla.oslauncher.beautylauncher.tool.languageTool.LanguageUtil
import com.ezt.v2.ezt.admobdemo.ads.NativeAds
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.truongnt.ios.ioslite.common.config.AppAds
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale


class LanguageStartActivity : BaseActivity<ActivityLanguageStartBinding>() {

    var codeLang = "en"
    var listLanguage: ArrayList<LanguageModel>? = arrayListOf()

    override val setViewBinding: ActivityLanguageStartBinding
        get() = ActivityLanguageStartBinding.inflate(LayoutInflater.from(this))


    companion object {
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
                AppAds.kit.showNativeInline(
                    this@LanguageStartActivity, "native_lang2", binding.frNative,
                    R.layout.layout_native_lang2
                )


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

        AppAds.kit.showNativeInline(
            this@LanguageStartActivity,
            "native_lang1",
            binding.frNative,
            R.layout.layout_native_lang1
        )

        preloadAds()
    }

    private fun preloadAds() {
        if (!AdsSdk.isAdFree) {
            AppAds.kit.preloadNativeInline( "native_onb1")
            AppAds.kit.preloadNativeInline("native_onb2")
            AppAds.kit.preloadNativeInline("native_onb3")
            AppAds.kit.preloadNativeInline("native_onb_full")

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