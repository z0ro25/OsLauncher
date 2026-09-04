package com.oslauncher.applauncher.themelauncher.Features.languageStart

import android.view.LayoutInflater
import androidx.activity.addCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.model.LanguageModel
//import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.Features.intro.IntroActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ActivityLanguageStartBinding
import com.oslauncher.applauncher.themelauncher.extensions.haveNetworkConnection
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.tool.languageTool.LanguageUtil
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils
import com.oslauncher.applauncher.themelauncher.utils.Constant
import java.util.Locale


class LanguageStartActivity : BaseActivity<ActivityLanguageStartBinding>() {

    var codeLang = "en"
    var listLanguage: ArrayList<LanguageModel>? = arrayListOf()

    override val setViewBinding: ActivityLanguageStartBinding
        get() = ActivityLanguageStartBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        initData()
        val linearLayoutManager = LinearLayoutManager(this)
        val languageAdapter = LanguageStartAdapter(listLanguage, { code -> codeLang = code }, this)

        languageAdapter.setCheck(codeLang)

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = languageAdapter

        languageAdapter.setCheck(codeLang)

        binding.ivDone.setOnClickListener { v ->
            LanguageUtil.saveLocale(baseContext, codeLang)
            launchActivity<IntroActivity> { }
        }

        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }
    }

    override fun viewListener() {

    }

    override fun dataObservable() {

    }

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