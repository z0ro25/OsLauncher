package com.oslauncher.applauncher.themelauncher.Features.wallpaper.createwallpaper

import android.content.Intent
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.CustomWallpaper.CustomLockWallpaperActivity
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.createwallpaper.adapters.SeeAllAdapter
import com.oslauncher.applauncher.themelauncher.databinding.ActivitySeeAllWallpaperBinding
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager

class SeeAllWallpaperCategoryActivity : BaseActivity<ActivitySeeAllWallpaperBinding>() {
    override val setViewBinding: ActivitySeeAllWallpaperBinding
        get() = ActivitySeeAllWallpaperBinding.inflate(LayoutInflater.from(this))
    var listImage: ArrayList<String> = arrayListOf()
    val adapter: SeeAllAdapter by lazy { SeeAllAdapter(this, listImage) }

    val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == RESULT_OK){
            finish()
        }
    }

    override fun initView() {
        val data = intent.getStringExtra("ALL_IMAGE_CATEGORY")
        val listimg = data?.trim()?.split(",") ?: listOf()
        listImage.addAll(listimg)

        binding.rcvAllWallpaper.adapter = adapter
    }

    override fun viewListener() {
        binding.apply {
            ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

            adapter.onImageClick = {
                val intent = Intent(this@SeeAllWallpaperCategoryActivity, CustomLockWallpaperActivity::class.java)
                intent.apply {
                    putExtra(YourWallpaperDataManager.IMAGE_SELECTED, it)
                    putExtra(YourWallpaperDataManager.IMAGE_FROM_GALLERY,"server")
                }
                launcher.launch(intent)
            }

        }
    }

    override fun dataObservable() {

    }
}