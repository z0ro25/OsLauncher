package com.oslauncher.applauncher.themelauncher.Features.wallpaper.emojiwallpaper

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.DisplayMetrics
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.amz.ios.database.HiddenAppManager
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.CustomWallpaper.CustomHomeWallpaperActivity
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.emojiwallpaper.adapters.EmojiAdapter
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.emojiwallpaper.adapters.EmojiWallpaperAdapter
import com.oslauncher.applauncher.themelauncher.databinding.ActivityEmojiWallpaperActivityBinding
import com.oslauncher.applauncher.themelauncher.dialog.ColorPickerDialog
import com.oslauncher.applauncher.themelauncher.dialog.WallpaperSelectOptionDialog
import com.oslauncher.applauncher.themelauncher.extensions.getBitmapFromView
import com.oslauncher.applauncher.themelauncher.extensions.serializable
import com.oslauncher.applauncher.themelauncher.model.EmojiModel
import com.oslauncher.applauncher.themelauncher.model.YourWallpaper
import com.oslauncher.applauncher.themelauncher.utils.CommonAds
import com.oslauncher.applauncher.themelauncher.utils.EmojiWallpaperUtils
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager.saveBitmapToCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.IOException


class EmojiWallpaperActivity : BaseActivity<ActivityEmojiWallpaperActivityBinding>() {
    override val setViewBinding: ActivityEmojiWallpaperActivityBinding
        get() = ActivityEmojiWallpaperActivityBinding.inflate(layoutInflater)

    val emojis: ArrayList<EmojiModel> = arrayListOf()
    val emojiAdapter: EmojiAdapter by lazy { EmojiAdapter(this, emojis) }
    val vpAdapter: EmojiWallpaperAdapter by lazy { EmojiWallpaperAdapter() }
    var wallpaperType = EmojiWallpaperUtils.EmojiWallpaperType.SMALL_GRID.ordinal
    var currentColor = "#FFEBA8"
    var isEdit = false
    var emojiModel: YourWallpaper? = null
    var isCurrent = false

    val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }

    companion object {
        var width = 0
        var height = 0
    }

    override fun initView() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        width = displayMetrics.widthPixels
        height = displayMetrics.heightPixels

        binding.tvEmojiCount.text = "0/6"
        binding.rcvEmoji.adapter = emojiAdapter

        binding.vpEmojiType.adapter = vpAdapter
        binding.dotWallpaperEmoji.attachToPager(binding.vpEmojiType)
        isEdit = intent.getBooleanExtra(YourWallpaperDataManager.IS_EDIT, false)
        isCurrent = intent.getBooleanExtra("IS_CURRENT", false)

        val bm =
            YourWallpaperDataManager.createColorBitmap(512, 512, Color.parseColor(currentColor))
        binding.ivBackground.setImageBitmap(bm)

        emojiModel = intent.serializable<YourWallpaper>("EMOJI_MODEL")

        if (emojiModel != null) {
            emojiModel?.listEmoji?.trim()?.split(",")?.let { vpAdapter.listEmoji.addAll(it) }
            vpAdapter.notifyDataSetChanged()
            currentColor = emojiModel?.emojiBgColor ?: "#FFEBA8"
            val bm =
                YourWallpaperDataManager.createColorBitmap(512, 512, Color.parseColor(currentColor))
            binding.ivBackground.setImageBitmap(bm)
            binding.vpEmojiType.currentItem = emojiModel?.emojiType ?: 0
        }

        // Set the generated image as the wallpaper
//        val wallpaperManager = WallpaperManager.getInstance(this)
//        wallpaperManager.setBitmap(emojiBitmap)
    }

    override fun viewListener() {
        binding.apply {
            tvCancel.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            emojiPickerView.setOnEmojiPickedListener {
                if (emojis.size < 6) {
                    emojis.add(EmojiModel(it.emoji))
                    vpAdapter.listEmoji.add(it.emoji)
                    binding.tvEmojiCount.text = "${emojis.size}/6"
                    emojiAdapter.notifyDataSetChanged()
                    vpAdapter.notifyDataSetChanged()
                }
            }

            ivClose.setOnClickListener { addEmojiView.isVisible = false }
            ivOpenEmo.setOnClickListener { addEmojiView.isVisible = true }

            emojiAdapter.onEmojiClick = { model, post ->
                emojis.forEachIndexed { index, emojiModel ->
                    emojiModel.isEdit = emojiModel.emoji == model.emoji && index == post
                }

                emojiAdapter.notifyDataSetChanged()
            }

            emojiAdapter.onDeleteClick = {
                emojis.remove(it)
                vpAdapter.listEmoji.remove(it.emoji)
                binding.tvEmojiCount.text = "${emojis.size}/6"
                emojiAdapter.notifyDataSetChanged()
                vpAdapter.notifyDataSetChanged()
            }

            vpEmojiType.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    wallpaperType = position
                }
            })

            ivBgColor.setOnClickListener {
                val colorDialog = ColorPickerDialog.newInstance(currentColor)
                colorDialog.onColorPick = {
                    currentColor = String.format("#%08X", (0xFFFFFFFF and it.toLong()))
                    val colorBM = YourWallpaperDataManager.createColorBitmap(512, 512, it)
                    ivBgColor.setImageBitmap(colorBM)
                    ivBackground.setImageBitmap(colorBM)
                }
                colorDialog.show(supportFragmentManager, "color")
            }

            tvSave.setOnClickListener {
                if (isEdit) {
                    rlLoading.isVisible = true
                    CoroutineScope(Dispatchers.IO).launch {
                        val bm = wallpaperContainer.getBitmapFromView()
                        val path = saveBitmapToCache(
                            this@EmojiWallpaperActivity,
                            bm,
                            "ios_wallpaper_${System.currentTimeMillis()}.png"
                        )
                        emojiModel?.apply {
                            lockBGPath = path
                            listEmoji = vpAdapter.listEmoji.joinToString(",")
                            emojiBgColor = currentColor
                            emojiType = binding.vpEmojiType.currentItem
                        }

                        emojiModel?.let { it1 ->
                            YourWallpaperDataManager.editYourWallpaper(
                                this@EmojiWallpaperActivity,
                                it1
                            )

                            if (isCurrent) {
                                YourWallpaperDataManager.saveYourCurrentWallpaper(
                                    this@EmojiWallpaperActivity,
                                    it1
                                )
                            }
                        }

                        if (isCurrent) {
                            setWallpaper()
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
                    val selectionDialog = WallpaperSelectOptionDialog(
                        this@EmojiWallpaperActivity,
                        wallpaperContainer.getBitmapFromView()
                    )
                    selectionDialog.onSetWallpaper = { bm ->
                        selectionDialog.dismiss()
                        rlLoading.isVisible = true
                        CoroutineScope(Dispatchers.IO).launch {
                            val path = saveBitmapToCache(
                                this@EmojiWallpaperActivity,
                                bm,
                                "ios_wallpaper_lock_${System.currentTimeMillis()}.png"
                            )
                            val yourWallpaper = YourWallpaper(
                                homeBGPath = path,
                                lockBGPath = path,
                                listEmoji = vpAdapter.listEmoji.joinToString(","),
                                emojiBgColor = currentColor,
                                emojiType = binding.vpEmojiType.currentItem
                            )
                            YourWallpaperDataManager.addYourWallpaper(
                                this@EmojiWallpaperActivity,
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
                            val path = saveBitmapToCache(
                                this@EmojiWallpaperActivity,
                                it,
                                "ios_wallpaper_${System.currentTimeMillis()}.png"
                            )
                            val intent = Intent(
                                this@EmojiWallpaperActivity,
                                CustomHomeWallpaperActivity::class.java
                            )
                            intent.apply {
                                putExtra(YourWallpaperDataManager.IMAGE_SELECTED, path)
                                putExtra(YourWallpaperDataManager.LOCK_WALLPAPER_PATH, path)
                                putExtra(YourWallpaperDataManager.IMAGE_FROM_GALLERY, "cache")
                                putExtra("Emoji_items", vpAdapter.listEmoji.joinToString(","))
                                putExtra("Emoji_bg_color", currentColor)
                                putExtra("Emoji_type", binding.vpEmojiType.currentItem)
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

    override fun dataObservable() {

    }

    private fun setWallpaper() {
        val bm = binding.wallpaperContainer.getBitmapFromView()
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
                } finally {
                    HiddenAppManager.notiFyUnHiddenApp(this@EmojiWallpaperActivity)
                }
            } else {
                // For Android versions below Nougat
                try {
                    wallpaperManager.setBitmap(bm)
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    HiddenAppManager.notiFyUnHiddenApp(this@EmojiWallpaperActivity)
                }
            }
        }
    }
}