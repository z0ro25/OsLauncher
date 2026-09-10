package com.ezla.oslauncher.Features.general.hiddenapp

import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.ezla.oslauncher.Base.BaseActivity
import com.ezla.oslauncher.Features.general.hiddenapp.adapters.HiddenAdapter
import com.ezla.oslauncher.Features.general.hiddenapp.viewmodels.HiddenAppViewModel
import com.ezla.oslauncher.databinding.ActivityHiddenappBinding
import com.ezt.ios.database.HiddenAppManager
import com.truongnt.ios.launcher.ItemInfo

class HiddenAppActivity : BaseActivity<ActivityHiddenappBinding>() {
    override val setViewBinding: ActivityHiddenappBinding
        get() = ActivityHiddenappBinding.inflate(layoutInflater)
    var allHiddenApp: ArrayList<ItemInfo> = arrayListOf()
    val adapter: HiddenAdapter by lazy { HiddenAdapter(this, allHiddenApp) }
    val viewModel: HiddenAppViewModel by lazy { ViewModelProvider(this)[HiddenAppViewModel::class.java] }

    override fun initView() {
        binding.rcvHidden.adapter = adapter
        viewModel.getAllHiddenApp()
    }

    override fun viewListener() {
        binding.apply {
            ivBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            adapter.onRemoveHiddenApp = {
                HiddenAppManager.unHideApp(it)
                viewModel.getAllHiddenApp()
                HiddenAppManager.notiFyUnHiddenApp(this@HiddenAppActivity)
            }
        }
    }

    override fun dataObservable() {
        viewModel.allHiddenAppLiveData.observe(this) {
            allHiddenApp.clear()
            it?.let {
                allHiddenApp.addAll(it)
            }
            binding.rcvHidden.isVisible = allHiddenApp.isNotEmpty()
            binding.emptyView.isVisible = allHiddenApp.isEmpty()
            adapter.notifyDataSetChanged()
        }
    }
}