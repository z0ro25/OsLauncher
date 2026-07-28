package com.oslauncher.applauncher.themelauncher.Features.wallpaper.createwallpaper.viewmodels

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.oslauncher.applauncher.themelauncher.api.ApiService
import com.oslauncher.applauncher.themelauncher.model.LIstImageModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

class CreateWallpaperViewModel : ViewModel() {
    val liveDataWallpaper: MutableLiveData<List<LIstImageModel>?> = MutableLiveData()

    fun getAllWallpaper() {
        ApiService.apiService.callApi().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<List<LIstImageModel>> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    liveDataWallpaper.postValue(null)
                }

                override fun onComplete() {

                }

                override fun onNext(t: List<LIstImageModel>) {
                    liveDataWallpaper.postValue(t)
                }

            })
    }

}