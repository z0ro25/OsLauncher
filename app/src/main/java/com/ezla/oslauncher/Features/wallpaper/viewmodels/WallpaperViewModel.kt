package com.ezla.oslauncher.Features.wallpaper.viewmodels

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ezla.oslauncher.model.YourWallpaper
import com.ezla.oslauncher.utils.YourWallpaperDataManager

class WallpaperViewModel : ViewModel() {
    var liveDataYourWallpaper : MutableLiveData<List<YourWallpaper>> = MutableLiveData()

    fun getAllYourWallpaper(context: Context){
        val listdata = YourWallpaperDataManager.getAllYourWallPaper(context)
        liveDataYourWallpaper.postValue(listdata)
    }

}