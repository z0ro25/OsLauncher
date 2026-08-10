package com.oslauncher.applauncher.themelauncher.Features.general.applibrary

import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.adapters.FolderReorderCallback
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.adapters.ManageFolderAdapter
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.data.AppLibraryFolder
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.dialog.NewFolderDialog
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.viewmodels.ManageLibraryViewModel
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ActivityManageLibraryBinding

/**
 * Màn Manage Library: CRUD folder custom + kéo sắp xếp (chỉ từ tay cầm). Nút "+" -> dialog New Folder.
 */
class ManageLibraryActivity : BaseActivity<ActivityManageLibraryBinding>() {

    override val setViewBinding: ActivityManageLibraryBinding
        get() = ActivityManageLibraryBinding.inflate(layoutInflater)

    private val listFolder: MutableList<AppLibraryFolder> = mutableListOf()
    private val adapter: ManageFolderAdapter by lazy { ManageFolderAdapter(listFolder) }
    private val viewModel: ManageLibraryViewModel by lazy {
        ViewModelProvider(this)[ManageLibraryViewModel::class.java]
    }
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun initView() {
        binding.rcvFolders.adapter = adapter
        itemTouchHelper = ItemTouchHelper(FolderReorderCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.rcvFolders)
        viewModel.loadFolders()
    }

    override fun viewListener() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.ivAdd.setOnClickListener { showNewFolderDialog() }

        adapter.onStartDrag = { vh -> itemTouchHelper.startDrag(vh) }
        adapter.onOrderChanged = { orderedIds -> viewModel.saveOrder(orderedIds) }
        adapter.onSwipeDelete = { folder -> viewModel.deleteFolder(folder.id) }
    }

    override fun dataObservable() {
        viewModel.foldersLiveData.observe(this) {
            listFolder.clear()
            it?.let { listFolder.addAll(it) }
            binding.rcvFolders.isVisible = listFolder.isNotEmpty()
            binding.emptyView.isVisible = listFolder.isEmpty()
            adapter.notifyDataSetChanged()
        }
    }

    private fun showNewFolderDialog() {
        val dialog = NewFolderDialog(this)
        dialog.onSave = onSave@{ name ->
            if (name.isEmpty()) {
                toast(R.string.applib_folder_name_empty)
                return@onSave false
            }
            if (viewModel.isNameExists(name)) {
                toast(R.string.applib_folder_name_exists)
                return@onSave false
            }
            viewModel.addFolder(name)
            true
        }
        dialog.show()
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show()
    }
}
