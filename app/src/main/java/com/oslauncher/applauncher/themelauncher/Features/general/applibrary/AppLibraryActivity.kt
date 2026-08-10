package com.oslauncher.applauncher.themelauncher.Features.general.applibrary

import android.content.Intent
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.adapters.AppLibraryAdapter
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.data.InstalledAppItem
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.viewmodels.AppLibraryViewModel
import com.oslauncher.applauncher.themelauncher.databinding.ActivityAppLibraryBinding
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.extensions.tap

/**
 * Màn App Library (list): danh sách app thật đã cài, mỗi dòng có nhãn folder/category hiện tại. Bấm 1
 * dòng -> Select Folder để gán. Hàng "Manage Library" -> quản lý folder. Category ĐỒNG BỘ 2 chiều với
 * App Library grid engine (folder custom giữ riêng ở app layer).
 */
class AppLibraryActivity : BaseActivity<ActivityAppLibraryBinding>() {

    override val setViewBinding: ActivityAppLibraryBinding
        get() = ActivityAppLibraryBinding.inflate(layoutInflater)

    private val listApp: ArrayList<InstalledAppItem> = arrayListOf()
    private val adapter: AppLibraryAdapter by lazy { AppLibraryAdapter(listApp) }
    private val viewModel: AppLibraryViewModel by lazy {
        ViewModelProvider(this)[AppLibraryViewModel::class.java]
    }

    override fun initView() {
        binding.rcvApps.adapter = adapter
        binding.progressBar.isVisible = true
        viewModel.loadInstalledApps()
    }

    override fun viewListener() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.llManageLibrary.tap { launchActivity<ManageLibraryActivity>() }

        adapter.onSelectFolder = { item ->
            val intent = Intent(this, SelectFolderActivity::class.java)
            intent.putExtra(SelectFolderActivity.EXTRA_COMPONENT, item.componentFlatten)
            intent.putExtra(SelectFolderActivity.EXTRA_APP_CATEGORY, item.appCategory)
            startActivity(intent)
        }
    }

    override fun dataObservable() {
        viewModel.appsLiveData.observe(this) {
            binding.progressBar.isVisible = false
            listApp.clear()
            it?.let { listApp.addAll(it) }
            adapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        // Sau khi quay lại từ Select Folder: cập nhật nhãn folder đã gán.
        viewModel.refreshLabels()
    }
}
