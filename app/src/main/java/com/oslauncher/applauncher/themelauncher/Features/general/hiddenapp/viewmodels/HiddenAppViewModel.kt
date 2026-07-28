package com.oslauncher.applauncher.themelauncher.Features.general.hiddenapp.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amz.ios.database.HiddenAppManager
import com.amz.ios.launcher.ItemInfo

class HiddenAppViewModel : ViewModel() {

    var allHiddenAppLiveData : MutableLiveData<List<ItemInfo>?> = MutableLiveData()

    fun getAllHiddenApp(){
        allHiddenAppLiveData.postValue(HiddenAppManager.getAllHiddenApp())
    }

}