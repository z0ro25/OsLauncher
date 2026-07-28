package com.oslauncher.applauncher.themelauncher.Features.lang

import android.view.LayoutInflater
import androidx.activity.addCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.home.HomeActivity
import com.oslauncher.applauncher.themelauncher.databinding.ActivityLanguageBinding
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.model.LanguageModel
import com.oslauncher.applauncher.themelauncher.tool.languageTool.LanguageUtil
import java.util.Locale

class LanguageSettingActivity : BaseActivity<ActivityLanguageBinding>() {

    var listLanguage: ArrayList<LanguageModel>? = arrayListOf()

    var codeLang: String? = null
    override val setViewBinding: ActivityLanguageBinding
        get() = ActivityLanguageBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        initData()

        val linearLayoutManager = LinearLayoutManager(this)
        val languageAdapter = LanguageAdapter(
            listLanguage, { code -> }, this
        )
        languageAdapter.setCheck(LanguageUtil.getPreLanguage(baseContext))
        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = languageAdapter


//        if (SharePrefUtils.getBoolean(this, Constant.AdsKey.BANNER_ALL)) {
//            binding.frBanner.isVisible = true
//            val bannerBuilder = BannerBuilder(this, this)
//            bannerBuilder.setListId(
//                AdmobApi.getInstance().getListIDByName(Constant.AdsKey.BANNER_ALL)
//            )
//            bannerManager = BannerManager(bannerBuilder)
//        } else binding.frBanner.isVisible = false

        onBackPressedDispatcher.addCallback {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun viewListener() {

        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.ivDone.setOnClickListener { obj ->
            for (i in listLanguage!!) {
                if (i.active == true && i.code !== LanguageUtil.getPreLanguage(baseContext)) {
                    LanguageUtil.saveLocale(baseContext, i.code)
                    launchActivity<HomeActivity> { }
                    finish()
                } else {
                    finish()
                }
            }
        }
    }

    override fun dataObservable() {

    }

    private fun initData() {
        listLanguage = ArrayList()
        val lang = Locale.getDefault().language
        listLanguage?.add(LanguageModel("English", "en"))
        listLanguage?.add(LanguageModel("Portuguese", "pt"))
        listLanguage?.add(LanguageModel("Spanish", "es"))
        listLanguage?.add(LanguageModel("German", "de"))
        listLanguage?.add(LanguageModel("French", "fr"))
        listLanguage?.add(LanguageModel("Hindi", "hi"))
        listLanguage?.add(LanguageModel("Indonesian", "in"))

        for (i in listLanguage!!.indices) {
            if (listLanguage!![i].code == lang) {
                listLanguage!!.add(0, listLanguage!![i])
                listLanguage!!.removeAt(i + 1)
            }
        }
    }


}