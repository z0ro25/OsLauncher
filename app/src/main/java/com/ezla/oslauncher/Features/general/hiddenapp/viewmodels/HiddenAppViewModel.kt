package com.ezla.oslauncher.Features.general.hiddenapp.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ezt.ios.database.HiddenAppManager
import com.truongnt.ios.launcher.ItemInfo

class HiddenAppViewModel : ViewModel() {

    var allHiddenAppLiveData : MutableLiveData<List<ItemInfo>?> = MutableLiveData()

    fun getAllHiddenApp(){
        allHiddenAppLiveData.postValue(HiddenAppManager.getAllHiddenApp())
    }

}