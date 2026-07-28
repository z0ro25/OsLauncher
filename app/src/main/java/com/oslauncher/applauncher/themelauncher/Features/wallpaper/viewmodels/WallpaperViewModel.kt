package com.oslauncher.applauncher.themelauncher.Features.wallpaper.viewmodels

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.oslauncher.applauncher.themelauncher.model.YourWallpaper
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager

class WallpaperViewModel : ViewModel() {
    var liveDataYourWallpaper : MutableLiveData<List<YourWallpaper>> = MutableLiveData()

    fun getAllYourWallpaper(context: Context){
        val listdata = YourWallpaperDataManager.getAllYourWallPaper(context)
        liveDataYourWallpaper.postValue(listdata)
    }

}