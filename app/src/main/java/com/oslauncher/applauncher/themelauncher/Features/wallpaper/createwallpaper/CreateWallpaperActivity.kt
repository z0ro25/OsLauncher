package com.oslauncher.applauncher.themelauncher.Features.wallpaper.createwallpaper

import android.content.Intent
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.CustomWallpaper.CustomLockWallpaperActivity
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.createwallpaper.adapters.AllServerImgAdapter
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.createwallpaper.viewmodels.CreateWallpaperViewModel
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.emojiwallpaper.EmojiWallpaperActivity
import com.oslauncher.applauncher.themelauncher.databinding.ActivityCreateWallpaperBinding
import com.oslauncher.applauncher.themelauncher.extensions.haveNetworkConnection
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.oslauncher.applauncher.themelauncher.model.LIstImageModel
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils
import com.oslauncher.applauncher.themelauncher.utils.Constant
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager

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