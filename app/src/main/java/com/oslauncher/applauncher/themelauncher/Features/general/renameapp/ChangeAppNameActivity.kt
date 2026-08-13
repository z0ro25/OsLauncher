package com.oslauncher.applauncher.themelauncher.Features.general.renameapp

import android.content.Intent
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.general.renameapp.adapters.ChangeAppNameAdapter
import com.oslauncher.applauncher.themelauncher.Features.general.renameapp.data.RenameAppItem
import com.oslauncher.applauncher.themelauncher.Features.general.renameapp.viewmodels.ChangeAppNameViewModel
import com.oslauncher.applauncher.themelauncher.databinding.ActivityChangeAppNameBinding

/**
 * Màn Change App Name (list): danh sách toàn bộ app thật đã cài, mỗi dòng icon + tên + chevron ">".
 * Bấm 1 dòng -> [RenameAppActivity] để đổi tên. Quay lại -> onResume refresh tên hiển thị.
 */
class ChangeAppNameActivity : BaseActivity<ActivityChangeAppNameBinding>() {

    override val setViewBinding: ActivityChangeAppNameBinding
        get() = ActivityChangeAppNameBinding.inflate(layoutInflater)

    private val listApp: ArrayList<RenameAppItem> = arrayListOf()
    private val adapter: ChangeAppNameAdapter by lazy { ChangeAppNameAdapter(listApp) }
    private val viewModel: ChangeAppNameViewModel by lazy {
        ViewModelProvider(this)[ChangeAppNameViewModel::class.java]
    }

    override fun initView() {
        binding.rcvApps.adapter = adapter
        binding.progressBar.isVisible = true
        viewModel.loadInstalledApps()
    }

    override fun viewListener() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        adapter.onSelectApp = { item ->
            val intent = Intent(this, RenameAppActivity::class.java)
            intent.putExtra(RenameAppActivity.EXTRA_COMPONENT, item.componentFlatten)
            intent.putExtra(RenameAppActivity.EXTRA_PACKAGE, item.packageName)
            intent.putExtra(RenameAppActivity.EXTRA_DEFAULT_LABEL, item.defaultLabel)
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
        // Sau khi quay lại từ màn đổi tên: cập nhật tên hiển thị mới.
        viewModel.refreshLabels()
    }
}
