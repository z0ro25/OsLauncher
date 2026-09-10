package com.ezla.oslauncher.Features.wallpaper.createwallpaper

import android.content.Intent
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.ezla.oslauncher.Base.BaseActivity
import com.ezla.oslauncher.Features.wallpaper.CustomWallpaper.CustomLockWallpaperActivity
import com.ezla.oslauncher.Features.wallpaper.createwallpaper.adapters.AllServerImgAdapter
import com.ezla.oslauncher.Features.wallpaper.createwallpaper.viewmodels.CreateWallpaperViewModel
import com.ezla.oslauncher.Features.wallpaper.emojiwallpaper.EmojiWallpaperActivity
import com.ezla.oslauncher.databinding.ActivityCreateWallpaperBinding
import com.ezla.oslauncher.extensions.haveNetworkConnection
import com.ezla.oslauncher.extensions.launchActivity
import com.ezla.oslauncher.extensions.tap
import com.ezla.oslauncher.model.LIstImageModel
import com.ezla.oslauncher.tool.sharePreferenceTool.SharePrefUtils
import com.ezla.oslauncher.utils.Constant
import com.ezla.oslauncher.utils.YourWallpaperDataManager

class CreateWallpaperActivity : BaseActivity<ActivityCreateWallpaperBinding>() {
    override val setViewBinding: ActivityCreateWallpaperBinding
        get() = ActivityCreateWallpaperBinding.inflate(layoutInflater)
    var allListPhoto: ArrayList<LIstImageModel> = arrayListOf()
    val allServerImgAdapter: AllServerImgAdapter by lazy { AllServerImgAdapter(this, allListPhoto) }
    val viewmodel: CreateWallpaperViewModel by lazy {
        ViewModelProvider(this)[CreateWallpaperViewModel::class.java]
    }

    val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { path ->
            if (path != null) {
                //start detail image
                val intent =
                    Intent(this@CreateWallpaperActivity, CustomLockWallpaperActivity::class.java)
                intent.apply {
                    putExtra(YourWallpaperDataManager.IMAGE_SELECTED, path.toString())
                    putExtra(YourWallpaperDataManager.IMAGE_FROM_GALLERY, "gallery")
                }
                launcher.launch(intent)
            }
        }

    val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            finish()
        }
    }

    override fun initView() {
        binding.apply {
            rcvAppImage.adapter = allServerImgAdapter
            viewmodel.getAllWallpaper()
        }

        onBackPressedDispatcher.addCallback {
            finish()
        }
    }

    override fun viewListener() {
        binding.apply {
            ivBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            llPhoto.tap {
                pickImageLauncher.launch("image/*")
            }

            llEmoji.tap {
                launcher.launch(
                    Intent(
                        this@CreateWallpaperActivity,
                        EmojiWallpaperActivity::class.java
                    )
                )
            }

            allServerImgAdapter.onImageClick = {
                val intent =
                    Intent(this@CreateWallpaperActivity, CustomLockWallpaperActivity::class.java)
                intent.apply {
                    putExtra(YourWallpaperDataManager.IMAGE_SELECTED, it)
                    putExtra(YourWallpaperDataManager.IMAGE_FROM_GALLERY, "server")
                }
                launcher.launch(intent)
            }

            allServerImgAdapter.onSeeAllClick = {
                val intent = Intent(this@CreateWallpaperActivity, SeeAllWallpaperCategoryActivity::class.java)
                intent.apply {
                    putExtra("ALL_IMAGE_CATEGORY", it.listImage)
                }
                launcher.launch(intent)
            }
        }
    }

    override fun dataObservable() {
        viewmodel.liveDataWallpaper.observe(this) { data ->
            allListPhoto.clear()
            data?.let {
                allListPhoto.addAll(it)
            }

            allServerImgAdapter.notifyDataSetChanged()
        }
    }
}