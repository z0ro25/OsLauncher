package com.oslauncher.applauncher.themelauncher.Features.general.hiddenapp

import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.general.hiddenapp.adapters.HiddenAdapter
import com.oslauncher.applauncher.themelauncher.Features.general.hiddenapp.viewmodels.HiddenAppViewModel
import com.oslauncher.applauncher.themelauncher.databinding.ActivityHiddenappBinding
import com.amz.ios.database.HiddenAppManager
import com.amz.ios.launcher.ItemInfo

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