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

        // Áp locale khi back (Đánh dấu, áp dụng khi back): chỉ đổi khi khác ngôn ngữ hiện tại.
        onBackPressedDispatcher.addCallback {
            val selected = listLanguage?.firstOrNull { it.active == true }
            val current = LanguageUtil.getPreLanguage(baseContext)
            if (selected != null && selected.code != current) {
                LanguageUtil.saveLocale(baseContext, selected.code)
                launchActivity<HomeActivity> { }
            } else {
                setResult(RESULT_OK)
            }
            finish()
        }
    }

    override fun viewListener() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun dataObservable() {

    }

    private fun initData() {
        // Thứ tự cố định theo Figma (không dồn ngôn ngữ hiện tại lên đầu).
        listLanguage = ArrayList()
        listLanguage?.add(LanguageModel("English", "en"))
        listLanguage?.add(LanguageModel("Hindi", "hi"))
        listLanguage?.add(LanguageModel("Spanish", "es"))
        listLanguage?.add(LanguageModel("French", "fr"))
        listLanguage?.add(LanguageModel("German", "de"))
        listLanguage?.add(LanguageModel("Korean", "ko"))
        listLanguage?.add(LanguageModel("Portuguese", "pt"))
    }


}