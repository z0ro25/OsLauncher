package com.oslauncher.applauncher.themelauncher.Features.general.changeicon

import android.content.Intent
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.general.changeicon.adapters.ChangeAppIconAdapter
import com.oslauncher.applauncher.themelauncher.Features.general.changeicon.data.ChangeIconItem
import com.oslauncher.applauncher.themelauncher.Features.general.changeicon.viewmodels.ChangeAppIconViewModel
import com.oslauncher.applauncher.themelauncher.databinding.ActivityChangeAppIconBinding

/**
 * Màn Change App Icon (list): danh sách toàn bộ app thật đã cài, mỗi dòng icon + tên + chevron ">".
 * Bấm 1 dòng -> [ChangeIconDetailActivity] để chọn ảnh gallery làm icon / reset về mặc định.
 */
class ChangeAppIconActivity : BaseActivity<ActivityChangeAppIconBinding>() {

    override val setViewBinding: ActivityChangeAppIconBinding
        get() = ActivityChangeAppIconBinding.inflate(layoutInflater)

    private val listApp: ArrayList<ChangeIconItem> = arrayListOf()
    private val adapter: ChangeAppIconAdapter by lazy { ChangeAppIconAdapter(listApp) }
    private val viewModel: ChangeAppIconViewModel by lazy {
        ViewModelProvider(this)[ChangeAppIconViewModel::class.java]
    }

    /** Bỏ qua lần onResume đầu (initView đã load); các lần sau (quay lại từ màn detail) reload để cập nhật icon custom. */
    private var loadedOnce = false

    override fun initView() {
        binding.rcvApps.adapter = adapter
        binding.progressBar.isVisible = true
        viewModel.loadInstalledApps()
    }

    override fun onResume() {
        super.onResume()
        if (loadedOnce) viewModel.loadInstalledApps() else loadedOnce = true
    }

    override fun viewListener() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        adapter.onSelectApp = { item ->
            val intent = Intent(this, ChangeIconDetailActivity::class.java)
            intent.putExtra(ChangeIconDetailActivity.EXTRA_COMPONENT, item.componentFlatten)
            intent.putExtra(ChangeIconDetailActivity.EXTRA_PACKAGE, item.packageName)
            intent.putExtra(ChangeIconDetailActivity.EXTRA_LABEL, item.displayLabel)
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
}
