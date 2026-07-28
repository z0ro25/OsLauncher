package com.oslauncher.applauncher.themelauncher.Features.wallpaper.CustomWallpaper

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.amz.ios.database.HiddenAppManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.setting.SettingActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ActivityCustomWallpaperBinding
import com.oslauncher.applauncher.themelauncher.dialog.WallpaperSelectOptionDialog
import com.oslauncher.applauncher.themelauncher.extensions.getBitmapFromView
import com.oslauncher.applauncher.themelauncher.extensions.serializable
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.oslauncher.applauncher.themelauncher.model.YourWallpaper
import com.oslauncher.applauncher.themelauncher.utils.CommonAds
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class CustomLockWallpaperActivity : BaseActivity<ActivityCustomWallpaperBinding>() {
    override val setViewBinding: ActivityCustomWallpaperBinding
        get() = ActivityCustomWallpaperBinding.inflate(layoutInflater)
    var imagePath: String? = ""
    var imageType: String? = "gallery"
    var imageBm: Bitmap? = null
    var isEdit = false
    var wallPaperModel: YourWallpaper? = null
    var isCurrentWall = false

    val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }


    val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) {
            //start detail image
            imageType = "gallery"
            imagePath = it.toString()
            val inputStream: InputStream? = contentResolver.openInputStream(it)
            val bm = BitmapFactory.decodeStream(inputStream)
            imageBm = bm
            binding.ivBackground.setImageBitmap(bm)
        }
    }

    override fun initView() {
        binding.apply {
            isEdit = intent.getBooleanExtra(YourWallpaperDataManager.IS_EDIT, false)
            imageType = intent.getStringExtra(YourWallpaperDataManager.IMAGE_FROM_GALLERY)
            imagePath = intent.getStringExtra(YourWallpaperDataManager.IMAGE_SELECTED)
            wallPaperModel = intent.serializable("EMOJI_MODEL")
            isCurrentWall = intent.getBooleanExtra("IS_CURRENT", false)
            imageBm = BitmapFactory.decodeResource(resources, R.drawable.bg_hello)

            if (wallPaperModel != null) {
                wallPaperModel?.let {
                    if (it.lockBGPath.isNullOrEmpty()) {
                        imageBm = BitmapFactory.decodeResource(resources, R.drawable.bg_hello)
                    } else {
                        imageBm = BitmapFactory.decodeFile(it.lockBGPath)
                    }
                    ivBackground.setImageBitmap(imageBm)
                    btnAdd.setText(getString(R.string.save))
                }
            } else {
                btnAdd.setText(getString(R.string.add))
                if (imagePath.isNullOrEmpty()) {
                    val bm = BitmapFactory.decodeResource(resources, R.drawable.bg_hello)
                    ivBackground.setImageBitmap(bm)
                } else when (imageType) {
                    "cache" -> {
                        imageBm = BitmapFactory.decodeFile(imagePath)
                        ivBackground.setImageBitmap(imageBm)
                    }

                    "server" -> {
                        Glide.with(this@CustomLockWallpaperActivity)
                            .asBitmap()
                            .load(YourWallpaperDataManager.IMAGE_SERVER_PATH + imagePath)
                            .listener(object : RequestListener<Bitmap> {
                                override fun onLoadFailed(
                                    p0: GlideException?,
                                    p1: Any?,
                                    p2: Target<Bitmap>?,
                                    p3: Boolean
                                ): Boolean {
                                    return false
                                }

                                override fun onResourceReady(
                                    p0: Bitmap?,
                                    p1: Any?,
                                    p2: Target<Bitmap>?,
                                    p3: DataSource?,
                                    p4: Boolean
                                ): Boolean {
                                    imageBm = p0
                                    return false
                                }
                            })
                            .into(ivBackground)
                    }

                    "gallery" -> {
                        val inputStream: InputStream? =
                            contentResolver.openInputStream(Uri.parse(imagePath))
                        imageBm = BitmapFactory.decodeStream(inputStream)
                        ivBackground.setImageBitmap(imageBm)
                    }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            tvCancel.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            ivSelectImage.tap {
                pickImageLauncher.launch("image/*")
            }

            btnAdd.setOnClickListener {
                if (isEdit) {
                    rlLoading.isVisible = true
                    CoroutineScope(Dispatchers.IO).launch {
                        val path = imageBm?.let { it1 ->
                            YourWallpaperDataManager.saveBitmapToCache(this@CustomLockWallpaperActivity,it1, "ios_wallpaper_lock_${System.currentTimeMillis()}.png")
                        }

                        wallPaperModel?.apply {
                            lockBGPath = path
                            YourWallpaperDataManager.editYourWallpaper(
                                this@CustomLockWallpaperActivity,
                                this
                            )

                            if (isCurrentWall){
                                YourWallpaperDataManager.saveYourCurrentWallpaper(this@CustomLockWallpaperActivity, this)
                                setWallpaper()
                            }
                        }
                        MainScope().launch {
                            showInterSave {
                                rlLoading.isVisible = false
                                setResult(RESULT_OK)
                                finish()
                            }

                        }
                    }


                } else {
                    val selectionDialog =
                        WallpaperSelectOptionDialog(this@CustomLockWallpaperActivity, imageBm)
                    selectionDialog.onSetWallpaper = { bm ->
                        selectionDialog.dismiss()
                        rlLoading.isVisible = true
                        CoroutineScope(Dispatchers.IO).launch {
                            val path =
                                YourWallpaperDataManager.saveBitmapToCache(this@CustomLockWallpaperActivity,bm, "ios_wallpaper_lock_${System.currentTimeMillis()}.png")
                            val yourWallpaper = YourWallpaper(homeBGPath = path, lockBGPath = path)
                            YourWallpaperDataManager.addYourWallpaper(
                                this@CustomLockWallpaperActivity,
                                yourWallpaper
                            )
                            MainScope().launch {
                                showInterSave {
                                    rlLoading.isVisible = false
                                    setResult(RESULT_OK)
                                    finish()
                                }

                            }

                        }

                    }

                    selectionDialog.onCustomHome = {
                        selectionDialog.dismiss()
                        rlLoading.isVisible = true
                        CoroutineScope(Dispatchers.IO).launch {
                            val path =
                                YourWallpaperDataManager.saveBitmapToCache(this@CustomLockWallpaperActivity,it, "ios_wallpaper_lock_${System.currentTimeMillis()}.png")
                            val intent = Intent(
                                this@CustomLockWallpaperActivity,
                                CustomHomeWallpaperActivity::class.java
                            )
                            intent.apply {
                                putExtra(YourWallpaperDataManager.IMAGE_SELECTED, imagePath)
                                putExtra(YourWallpaperDataManager.LOCK_WALLPAPER_PATH, path)
                                putExtra(YourWallpaperDataManager.IMAGE_FROM_GALLERY, imageType)
                                putExtra("IS_CURRENT",isCurrentWall)
                            }
                            MainScope().launch {
                                rlLoading.isVisible = false
                                launcher.launch(intent)
                            }
                        }
                    }

                    selectionDialog.show()
                }
            }
        }
    }

    private fun setWallpaper() {
        val bm = binding.ivBackground.getBitmapFromView()
        CoroutineScope(Dispatchers.IO).launch {
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // For Android Nougat and above
                try {
                    Thread {
                        wallpaperManager.setBitmap(
                            bm,
                            null,
                            true,
                            WallpaperManager.FLAG_LOCK
                        )
                    }.start()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                // For Android versions below Nougat
                try {
                    wallpaperManager.setBitmap(bm)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            HiddenAppManager.notiFyUnHiddenApp(this@CustomLockWallpaperActivity)
        }
    }

    override fun dataObservable() {

    }
}