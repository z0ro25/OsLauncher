package com.oslauncher.applauncher.themelauncher.Features.general.applibrary.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.data.AppLibraryFolder
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.data.AppLibraryRepository

/** Quản lý danh sách folder custom (Manage Library). */
class ManageLibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppLibraryRepository(app)
    val foldersLiveData: MutableLiveData<List<AppLibraryFolder>> = MutableLiveData()

    fun loadFolders() {
        foldersLiveData.postValue(repo.getFolders())
    }

    fun isNameExists(name: String): Boolean = repo.isFolderNameExists(name)

    fun addFolder(name: String) {
        repo.addFolder(name)
        loadFolders()
    }

    fun deleteFolder(id: String) {
        repo.deleteFolder(id)
        loadFolders()
    }

    /** Lưu thứ tự mới sau khi kéo (không reload để tránh giật danh sách đang hiển thị). */
    fun saveOrder(orderedIds: List<String>) {
        repo.reorderFolders(orderedIds)
    }
}
