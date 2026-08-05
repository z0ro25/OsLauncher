package com.oslauncher.applauncher.themelauncher.Features.wallpaper.CustomWallpaper

import com.oslauncher.applauncher.themelauncher.theme.AppThemeManager

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
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
import com.oslauncher.applauncher.themelauncher.databinding.ActivityCustomHomeWallpaperBinding
import com.oslauncher.applauncher.themelauncher.dialog.ColorPickerDialog
import com.oslauncher.applauncher.themelauncher.dialog.GradientColorPickerDialog
import com.oslauncher.applauncher.themelauncher.extensions.getBitmapFromView
import com.oslauncher.applauncher.themelauncher.extensions.serializable
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.oslauncher.applauncher.themelauncher.model.GradientColorModel
import com.oslauncher.applauncher.themelauncher.model.YourWallpaper
import com.oslauncher.applauncher.themelauncher.utils.CommonAds
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager
import com.oslauncher.applauncher.themelauncher.views.blur.Blurry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream


class CustomHomeWallpaperActivity : BaseActivity<ActivityCustomHomeWallpaperBinding>() {
    override val setViewBinding: ActivityCustomHomeWallpaperBinding
        get() = ActivityCustomHomeWallpaperBinding.inflate(layoutInflater)
    var imagePath: String = ""
    var lockWallpaperPath: String = ""
    var currentWallpaperBM: Bitmap? = null

    var pairBM: Bitmap? = null
    var photoBM: Bitmap? = null
    var colorBM: Bitmap? = null
    var gradientBM: Bitmap? = null

    var isBlur = false
    var gradientColor: GradientColorModel = GradientColorModel("#94EDFA", "#6B57F5")
    var currentColor = "#FFEBA8"

    var emojiList: String = ""
    var emoBgColor: String = ""
    var emoType: Int = 0
    var isEdit = false
    var wallpaperModel: YourWallpaper? = null
    var isCurrentWall = false

    val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) {
            val inputStream: InputStream? = contentResolver.openInputStream(it)
            photoBM = BitmapFactory.decodeStream(inputStream)
            currentWallpaperBM = photoBM
            if (isBlur) {
                Blurry.with(this@CustomHomeWallpaperActivity)
                    .radius(15)
                    .from(currentWallpaperBM)
                    .into(binding.ivBackground)
            } else binding.ivBackground.setImageBitmap(currentWallpaperBM)
            currentWallpaperBM?.let {
                binding.photo.setPair(it)
                binding.photo.isAddGrad = false
                binding.photo.setlectView()
            }
        }
    }

    var isShowDialog = false

    override fun initView() {
        gradientBM = YourWallpaperDataManager.createGradientBitmap(
            512,
            512,
            Color.parseColor(gradientColor.startColor),
            Color.parseColor(gradientColor.endColor)
        )
        isCurrentWall = intent.getBooleanExtra("IS_CURRENT", false)

        isEdit = intent.getBooleanExtra(YourWallpaperDataManager.IS_EDIT, false)
        wallpaperModel = intent.serializable("EMOJI_MODEL")

        colorBM =
            YourWallpaperDataManager.createColorBitmap(512, 512, Color.parseColor(currentColor))
        binding.color.setPair(colorBM!!)

        binding.apply {
            emojiList = intent.getStringExtra("Emoji_items") ?: ""
            emoBgColor = intent.getStringExtra("Emoji_bg_color") ?: ""
            emoType = intent.getIntExtra("Emoji_type", 0)

            val fromGallery =
                intent.getStringExtra(YourWallpaperDataManager.IMAGE_FROM_GALLERY)
            val bmPath = intent.getStringExtra(YourWallpaperDataManager.IMAGE_SELECTED)
            lockWallpaperPath =
                intent.getStringExtra(YourWallpaperDataManager.LOCK_WALLPAPER_PATH).toString()
            if (lockWallpaperPath.isNullOrEmpty()) {
                lockWallpaperPath = bmPath ?: ""
            }

            if (bmPath.isNullOrEmpty()) {
                val bm = BitmapFactory.decodeResource(resources, R.drawable.bg_hello)
                currentWallpaperBM = bm
                pairBM = bm
                ivBackground.setImageBitmap(currentWallpaperBM)
                pair.setPair(bm)
                pair.setlectView()
            } else when (fromGallery) {
                "cache" -> {
                    val bm = BitmapFactory.decodeFile(bmPath)
                    currentWallpaperBM = bm
                    pairBM = bm
                    ivBackground.setImageBitmap(currentWallpaperBM)
                    pair.setPair(bm)
                    pair.setlectView()
                }

                "gallery" -> {
                    val inputStream: InputStream? =
                        contentResolver.openInputStream(Uri.parse(bmPath))
                    val bm = BitmapFactory.decodeStream(inputStream)
                    currentWallpaperBM = bm
                    pairBM = bm
                    ivBackground.setImageBitmap(currentWallpaperBM)
                    pair.setPair(bm)
                    pair.setlectView()
                }

                "server" -> {
                    imagePath = YourWallpaperDataManager.IMAGE_SERVER_PATH + bmPath
                    Glide.with(this@CustomHomeWallpaperActivity)
                        .asBitmap()
                        .load(imagePath)
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
                                currentWallpaperBM = p0
                                pairBM = p0
                                currentWallpaperBM?.let { pair.setPair(it) }
                                pair.setlectView()
                                return false
                            }
                        })
                        .into(ivBackground)
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            tvCancel.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            blur.onViewClick = {
                isBlur = !isBlur
                setPreViewWallpaper()
                if (!isBlur) {
                    blur.unSelectView()
                }
            }

            pair.onViewClick = {
                color.unSelectView()
                gradient.unSelectView()
                photo.unSelectView()
                currentWallpaperBM = pairBM
                setPreViewWallpaper()
            }

            color.onViewClick = {
                pair.unSelectView()
                gradient.unSelectView()
                photo.unSelectView()
                currentWallpaperBM = colorBM
                setPreViewWallpaper()
            }

            gradient.onViewClick = {
                pair.unSelectView()
                color.unSelectView()
                photo.unSelectView()
                currentWallpaperBM = gradientBM
                setPreViewWallpaper()
            }

            photo.onViewClick = {
                pair.unSelectView()
                color.unSelectView()
                gradient.unSelectView()
                if (photoBM != null) {
                    currentWallpaperBM = photoBM
                    setPreViewWallpaper()
                }
            }

            color.onAddColorClick = {
                val colorDialog = ColorPickerDialog.newInstance(currentColor)
                colorDialog.onColorPick = {
                    currentColor = String.format("#%08X", (0xFFFFFFFF and it.toLong()))
                    colorBM = YourWallpaperDataManager.createColorBitmap(512, 512, it)
                    colorBM?.let { binding.color.setPair(it) }
                    currentWallpaperBM = colorBM
                    setPreViewWallpaper()
                }
                if (!isShowDialog) {
                    isShowDialog = true
                    colorDialog.show(supportFragmentManager, "color")
                    Handler().postDelayed({ isShowDialog = false }, 2000)
                }


            }

            gradient.onAddGradientClick = {
                val photoGradientDialog = GradientColorPickerDialog.newInstance(gradientColor)
                photoGradientDialog.onColorPicked = { color, bm ->
                    bm?.let { gradient.setGradient(it) }
                    gradientColor = color
                    gradientBM = bm
                    currentWallpaperBM = gradientBM
                    setPreViewWallpaper()
                }

                if (!isShowDialog) {
                    isShowDialog = true
                    photoGradientDialog.show(supportFragmentManager, "gradient_picker")
                    Handler().postDelayed({ isShowDialog = false }, 2000)
                }
            }

            photo.onAddPhotoClick = {
                if (!isShowDialog) {
                    isShowDialog = true
                    pickImageLauncher.launch("image/*")
                    Handler().postDelayed({ isShowDialog = false }, 2000)
                }
            }

            tvSave.tap {
                rlLoading.isVisible = true
                if (isEdit) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val bm = ivBackground.getBitmapFromView()
                        val path =
                            YourWallpaperDataManager.saveBitmapToCache(
                                this@CustomHomeWallpaperActivity,
                                bm,
                                "ios_wallpaper_${System.currentTimeMillis()}.png"
                            )
                        wallpaperModel?.apply {
                            homeBGPath = path
                            YourWallpaperDataManager.editYourWallpaper(
                                this@CustomHomeWallpaperActivity,
                                this
                            )

                            if (isCurrentWall) {
                                YourWallpaperDataManager.saveYourCurrentWallpaper(
                                    this@CustomHomeWallpaperActivity,
                                    this
                                )
                            }
                        }

                        if (isCurrentWall) {
                            setWallpaper()
                        }

                        showInterSave {
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        val bm = ivBackground.getBitmapFromView()
                        val path =
                            YourWallpaperDataManager.saveBitmapToCache(
                                this@CustomHomeWallpaperActivity,
                                bm,
                                "ios_wallpaper_home_${System.currentTimeMillis()}.png"
                            )
                        val yourWallpaper = YourWallpaper(
                            homeBGPath = path,
                            lockBGPath = lockWallpaperPath,
                            listEmoji = emojiList,
                            emojiType = emoType,
                            emojiBgColor = emoBgColor
                        )
                        YourWallpaperDataManager.addYourWallpaper(
                            this@CustomHomeWallpaperActivity,
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

            }
        }
    }

    private fun ActivityCustomHomeWallpaperBinding.setPreViewWallpaper() {
        if (isBlur) {
            Blurry.with(this@CustomHomeWallpaperActivity)
                .radius(if (isBlur) 15 else 1)
                .from(currentWallpaperBM)
                .into(ivBackground)
        } else {
            ivBackground.setImageBitmap(currentWallpaperBM)
        }
    }

    override fun dataObservable() {

    }


    private fun setWallpaper() {
        // User tự set wallpaper -> từ đây đổi mode không ghi đè ảnh của user.
        AppThemeManager.markUserWallpaper(this)
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
                            WallpaperManager.FLAG_SYSTEM
                        )
                    }.start()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    MainScope().launch {
                        binding.rlLoading.isVisible = false
                    }
                    HiddenAppManager.notiFyUnHiddenApp(this@CustomHomeWallpaperActivity)
                }
            } else {
                // For Android versions below Nougat
                try {
                    wallpaperManager.setBitmap(bm)
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    MainScope().launch {
                        binding.rlLoading.isVisible = false
                    }
                    HiddenAppManager.notiFyUnHiddenApp(this@CustomHomeWallpaperActivity)
                }
            }

        }
    }

}