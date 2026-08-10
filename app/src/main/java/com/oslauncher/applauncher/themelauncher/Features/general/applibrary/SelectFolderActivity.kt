package com.oslauncher.applauncher.themelauncher.Features.general.applibrary

import androidx.lifecycle.ViewModelProvider
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.adapters.SelectFolderAdapter
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.viewmodels.SelectFolderViewModel
import com.oslauncher.applauncher.themelauncher.databinding.ActivitySelectFolderBinding

/**
 * Màn Select Folder: chọn ĐƠN target (Other + folder custom + category) cho 1 app. Chọn -> lưu ngay
 * (category đồng bộ sang grid engine, folder custom lưu app layer) -> đóng, quay lại App Library
 * ([AppLibraryActivity.onResume] refresh nhãn).
 */
class SelectFolderActivity : BaseActivity<ActivitySelectFolderBinding>() {

    override val setViewBinding: ActivitySelectFolderBinding
        get() = ActivitySelectFolderBinding.inflate(layoutInflater)

    private val viewModel: SelectFolderViewModel by lazy {
        ViewModelProvider(this)[SelectFolderViewModel::class.java]
    }
    private var component: String = ""
    private var appCategory: Int = -1
    private lateinit var adapter: SelectFolderAdapter

    override fun initView() {
        component = intent.getStringExtra(EXTRA_COMPONENT).orEmpty()
        appCategory = intent.getIntExtra(EXTRA_APP_CATEGORY, -1)
        val targets = viewModel.buildTargets()
        val current = viewModel.getCurrentTarget(component, appCategory)
        adapter = SelectFolderAdapter(targets, current)
        binding.rcvTargets.adapter = adapter
    }

    override fun viewListener() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        adapter.onSelect = { targetId ->
            viewModel.assign(component, targetId, appCategory)
            finish()
        }
    }

    override fun dataObservable() {}

    companion object {
        const val EXTRA_COMPONENT = "extra_component"
        const val EXTRA_APP_CATEGORY = "extra_app_category"
    }
}
