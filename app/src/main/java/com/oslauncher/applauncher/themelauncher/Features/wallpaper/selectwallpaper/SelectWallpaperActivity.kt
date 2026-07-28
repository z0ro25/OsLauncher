package com.oslauncher.applauncher.themelauncher.Features.wallpaper.selectwallpaper

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.amz.ios.database.HiddenAppManager
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.CustomWallpaper.CustomHomeWallpaperActivity
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.CustomWallpaper.CustomLockWallpaperActivity
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.createwallpaper.CreateWallpaperActivity
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.emojiwallpaper.EmojiWallpaperActivity
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.selectwallpaper.adapters.VPWallpaperAdapter
import com.oslauncher.applauncher.themelauncher.Features.wallpaper.viewmodels.WallpaperViewModel
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ActivitySelectWallpaperBinding
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.oslauncher.applauncher.themelauncher.model.YourWallpaper
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils
import com.oslauncher.applauncher.themelauncher.utils.Constant
import com.oslauncher.applauncher.themelauncher.utils.YourWallpaperDataManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException


class SelectWallpaperActivity : BaseActivity<ActivitySelectWallpaperBinding>() {
    override val setViewBinding: ActivitySelectWallpaperBinding
        get() = ActivitySelectWallpaperBinding.inflate(layoutInflater)
    private var listYourWallPaper: ArrayList<YourWallpaper> = arrayListOf()
    private var isEdit = false
    private val viewModel: WallpaperViewModel by lazy { ViewModelProvider(this)[WallpaperViewModel::class.java] }
    private val yourWallpaperAdapter: VPWallpaperAdapter by lazy {
        VPWallpaperAdapter(
            this,
            listYourWallPaper
        )
    }
    var currentWallpaper: YourWallpaper? = null
    var isCurrentWallpaper = false
    var selectWallpaper: YourWallpaper? = null
    var currentPost = 0

    override fun initView() {
        currentPost = 0
        currentWallpaper = YourWallpaperDataManager.getYourCurrentWallpaper(this)
        binding.vpYourWallpaper.adapter = yourWallpaperAdapter
        isEdit = intent.getBooleanExtra(YourWallpaperDataManager.IS_EDIT, false)


        loadNative()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAllYourWallpaper(this@SelectWallpaperActivity)
    }

    override fun viewListener() {
        binding.apply {

            ivBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            vpYourWallpaper.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    currentPost = position
                    selectWallpaper = listYourWallPaper[currentPost]
                    currentWallpaper = YourWallpaperDataManager.getYourCurrentWallpaper(this@SelectWallpaperActivity)

                    currentWallpaper?.let {
                        if (selectWallpaper?.lockBGPath == currentWallpaper?.lockBGPath && selectWallpaper?.homeBGPath == currentWallpaper?.homeBGPath) {
                            tvSetCurrent.text = getString(R.string.current)
                            tvSetCurrent.background = null
                            isCurrentWallpaper = true
                        } else {
                            isCurrentWallpaper = false
                            tvSetCurrent.text = getString(R.string.set_as_current)
                            tvSetCurrent.setBackgroundResource(R.drawable.bg_circle_ff8c21_15)
                        }
                    } ?: run {
                        if (position == 0) {
                            tvSetCurrent.text = getString(R.string.current)
                            tvSetCurrent.background = null
                            isCurrentWallpaper = true
                        } else {
                            isCurrentWallpaper = false
                            tvSetCurrent.text = getString(R.string.set_as_current)
                            tvSetCurrent.setBackgroundResource(R.drawable.bg_circle_ff8c21_15)
                        }
                    }
                    //check if current
                }
            })

            btnAddNewWallpaper.setOnClickListener {
                launchActivity<CreateWallpaperActivity> {}
            }

            yourWallpaperAdapter.apply {
                onHomeCustomClick = {
                    launchActivity<CustomHomeWallpaperActivity> {
                        putExtra(YourWallpaperDataManager.IMAGE_SELECTED, it.homeBGPath)
                        putExtra(YourWallpaperDataManager.IMAGE_FROM_GALLERY, "cache")
                        putExtra(YourWallpaperDataManager.IS_EDIT, true)
                        putExtra("EMOJI_MODEL", it)
                        putExtra("IS_CURRENT",isCurrentWallpaper)
                    }
                }

                onLockCustomClick = {
                    if (it.listEmoji.isEmpty()) {
                        launchActivity<CustomLockWallpaperActivity> {
                            putExtra(YourWallpaperDataManager.IMAGE_SELECTED, it.lockBGPath)
                            putExtra(YourWallpaperDataManager.IMAGE_FROM_GALLERY, "cache")
                            putExtra(YourWallpaperDataManager.IS_EDIT, true)
                            putExtra("EMOJI_MODEL", it)
                            putExtra("IS_CURRENT",isCurrentWallpaper)
                        }
                    } else {
                        launchActivity<EmojiWallpaperActivity> {
                            putExtra(YourWallpaperDataManager.IMAGE_SELECTED, it.lockBGPath)
                            putExtra(YourWallpaperDataManager.IS_EDIT, true)
                            putExtra("EMOJI_MODEL", it)
                            putExtra("IS_CURRENT",isCurrentWallpaper)
                        }
                    }
                }
            }

            tvSetCurrent.tap {
                selectWallpaper = listYourWallPaper[currentPost]
                if (!isCurrentWallpaper) {
                    selectWallpaper?.let { model ->
                        rlLoading.isVisible = true
                        YourWallpaperDataManager.saveYourCurrentWallpaper(
                            this@SelectWallpaperActivity,
                            model
                        )
                        currentWallpaper = model
                        setWallpaper(model.lockBGPath ?: "", model.homeBGPath ?: "")
                        HiddenAppManager.notiFyUnHiddenApp(this@SelectWallpaperActivity)
                        tvSetCurrent.text = getString(R.string.current)
                        tvSetCurrent.background = null
                        isCurrentWallpaper = true
                    }
                }
            }
        }
    }

    override fun dataObservable() {
        viewModel.liveDataYourWallpaper.observe(this) {
            listYourWallPaper.clear()
            listYourWallPaper.addAll(it)

            binding.dotIndicator.attachToPager(binding.vpYourWallpaper)

            yourWallpaperAdapter.notifyDataSetChanged()
        }
    }

    fun setWallpaper(lockScreen: String, homeScreen: String) {
        // Set the home screen wallpaper
        val wallpaperManager = WallpaperManager.getInstance(applicationContext)
        val lockBm =
            if (lockScreen.isNotEmpty()) BitmapFactory.decodeFile(lockScreen) else BitmapFactory.decodeResource(
                resources,
                R.drawable.bg_hello
            )
        val homeBm =
            if (homeScreen.isNotEmpty()) BitmapFactory.decodeFile(homeScreen) else BitmapFactory.decodeResource(
                resources,
                R.drawable.bg_hello
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // For Android Nougat and above
            try {
                Thread {
                    wallpaperManager.setBitmap(
                        lockBm,
                        null,
                        true,
                        WallpaperManager.FLAG_LOCK
                    )
                    wallpaperManager.setBitmap(
                        homeBm,
                        null,
                        true,
                        WallpaperManager.FLAG_SYSTEM
                    )
                }.start()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(
                    applicationContext,
                    "Home screen wallpaper setting failed!",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                MainScope().launch {
                    delay(2000)
                    binding.rlLoading.isVisible = false
                }
            }
        } else {
            // For Android versions below Nougat
            try {
                Handler().postDelayed({
                    wallpaperManager.setBitmap(lockBm)
                    runOnUiThread {

                    }
                }, 1000)


            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(
                    applicationContext,
                    "Home screen wallpaper setting failed!",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                MainScope().launch {
                    delay(2000)
                    binding.rlLoading.isVisible = false
                }
            }
        }

    }


    private fun loadNative() {
        binding.frCurrent.isVisible = false
    }
}