package com.ezla.oslauncher.beautylauncher.Features.wallpaperonboarding

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.manager.Lifecycle
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.Features.subs.SubsAct
import com.ezla.oslauncher.beautylauncher.databinding.ActivitySelectBackgroundBinding
import com.ezla.oslauncher.beautylauncher.extensions.launchActivity
import com.ezla.oslauncher.beautylauncher.theme.AppThemeManager
import com.ezla.oslauncher.beautylauncher.utils.BillingUtils
import com.ezt.v2.ezt.admobdemo.ads.FullscreenNativeAds
import com.ezt.v2.ezt.admobdemo.ads.InterAds
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.ezt.v2.ezt.admobdemo.ads.placement.AdFullScreenCallback
import com.ezt.v2.ezt.admobdemo.ads.placement.AdFullScreenResult
import com.ezt.v2.ezt.admobdemo.ads.placement.PlacementFormat
import com.truongnt.ios.ioslite.common.config.AppAds
import com.truongnt.ios.ioslite.common.config.SharePrefUtils
import com.truongnt.ios.launcher.searchlauncher.SearchLauncher
import kotlinx.coroutines.launch

class SelectBackgroundActivity : BaseActivity<ActivitySelectBackgroundBinding>() {

    override val setViewBinding: ActivitySelectBackgroundBinding
        get() = ActivitySelectBackgroundBinding.inflate(layoutInflater)

    // Thư mục assets chứa hình nền onboarding.
    private val wallpaperAssetDir = "wallpapers"

    // Đường dẫn asset tương đối, ví dụ "wallpapers/img_1.webp".
    private val wallpaperAssets: ArrayList<String> = arrayListOf()

    private val adapter: SelectBackgroundAdapter by lazy {
        SelectBackgroundAdapter(this, wallpaperAssets)
    }

    private var currentPos = 3

    /** Đã mời mua sub trong lần onboarding này chưa — chặn mở lặp khi user quay về màn. */
    private var subsOffered = false

    // Hình nền user chọn, chỉ đặt lúc rời màn (sau inter / sau Subs); null = bấm Skip.
    private var pendingWallpaperAsset: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        subsOffered = savedInstanceState?.getBoolean(KEY_SUBS_OFFERED) == true
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SUBS_OFFERED, subsOffered)
    }

    // Mở màn subs rồi chờ đóng: quay về mới đi tiếp luồng vào launcher.
    private val subsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Back ở màn subs gọi finishAffinity() -> app đang thoát, không được chạy tiếp luồng.
        if (isFinishing || isDestroyed) return@registerForActivityResult
        goToLauncher()
    }

    override fun onResume() {
        super.onResume()
        showBannerOverlay(binding.frBanner, binding.spaceBanner)
    }

    override fun initView() {

        if (!AdsSdk.isAdFree) {
            lifecycleScope.launch {
                AppAds.kit.preloadAdFullScreen("start_launcher_Inter")
            }
        }


        loadWallpaperAssets()
        setupViewPager()

        onBackPressedDispatcher.addCallback {
            // Onboarding không cho lùi về Permission; back = bỏ qua, vào thẳng launcher.
            finishAffinity()
        }
    }

    override fun viewListener() {
        binding.tvSkip.setOnClickListener { goToLauncher() }

        binding.tvSetWallpaper.setOnClickListener {
            if (wallpaperAssets.isEmpty()) {
                // Chưa có ảnh nào trong assets -> coi như bỏ qua.
                goToLauncher()
                return@setOnClickListener
            }
            setWallpaperAndStartLauncher(wallpaperAssets[currentPos])
        }
    }

    override fun dataObservable() {}

    /** Liệt kê ảnh trong assets/wallpapers/, lọc theo đuôi ảnh phổ biến. */
    private fun loadWallpaperAssets() {
        wallpaperAssets.clear()
        val imageExts = listOf(".jpg", ".jpeg", ".png", ".webp")
        try {
            assets.list(wallpaperAssetDir)?.forEach { name ->
                if (imageExts.any { name.lowercase().endsWith(it) }) {
                    wallpaperAssets.add("$wallpaperAssetDir/$name")
                }
            }
        } catch (_: Exception) {
            // Thư mục chưa tồn tại -> danh sách rỗng, carousel trống là bình thường.
        }
    }

    private fun setupViewPager() {
        binding.vpWallpaper.apply {
            adapter = this@SelectBackgroundActivity.adapter
            offscreenPageLimit = 3
            clipToPadding = false
            clipChildren = false

            // Padding ngang trên RecyclerView con để lộ mép 2 trang kề (hiệu ứng carousel).
            val hPad = (68 * resources.displayMetrics.density).toInt()
            (getChildAt(0) as? RecyclerView)?.apply {
                setPadding(hPad, 0, hPad, 0)
                clipToPadding = false
            }

            val marginPx = (12 * resources.displayMetrics.density).toInt()
            val transformer = CompositePageTransformer().apply {
                addTransformer(MarginPageTransformer(marginPx))
                addTransformer(SelectBackgroundPageTransformer())
            }
            setPageTransformer(transformer)

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    currentPos = position
                }
            })

            if (wallpaperAssets.isNotEmpty()) {
                binding.vpWallpaper.currentItem = currentPos
            }
        }
    }

    private fun setWallpaperAndStartLauncher(assetPath: String) {
        // User chủ động chọn hình nền -> từ đây đổi mode không ghi đè ảnh của user.
        AppThemeManager.markUserWallpaper(this)
        // Lưu asset đã chọn để màn Hello dùng lại làm nền (thay ảnh bg_hello cứng).
        SharePrefUtils.putString(this, HELLO_BG_ASSET_KEY, assetPath)
        // Đặt wallpaper SAU inter: Android 12+ đổi bảng màu theo wallpaper sẽ dựng lại Activity,
        // đặt trước thì inter đang hiện bị tắt ngang và kẹt lại màn này.
        pendingWallpaperAsset = assetPath
        goToLauncher()
    }

    // Đặt wallpaper XONG rồi mới mở desktop: đổi wallpaper lúc desktop đang onCreate (lần đầu) làm
    // crash NPE mDragLayer ở Launcher.setupViews. Inter đã đóng nên không còn bị cắt ngang.
    private fun applyWallpaperThenStartLauncher(assetPath: String) {
        binding.frLoading.isVisible = true
        val appContext = applicationContext
        Thread {
            try {
                val bitmap = appContext.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
                val wm = WallpaperManager.getInstance(appContext)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                } else {
                    wm.setBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(appContext, "Set wallpaper failed!", Toast.LENGTH_SHORT).show()
                }
            }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                binding.frLoading.isVisible = false
                startLauncherAndFinish()
            }
        }.start()
    }

    private fun startLauncherAndFinish() {
        if (!isDefaultLauncher()) {
            SharePrefUtils.putBoolean(this, "hello_pending", true)
            SharePrefUtils.putBoolean(this, PREF_PROMPT_SET_DEFAULT_ON_DESKTOP, true)
        }
        launchActivity<SearchLauncher> { }
        finishAffinity()
    }

    private fun goToLauncher() {
        Log.d(TAG, "goToLauncher: offered=$subsOffered premium=${BillingUtils.isSubsCached(this)} inst=${hashCode()}")

        if (SHOW_SUBS_ON_ONBOARDING && !subsOffered && !BillingUtils.isSubsCached(this)) {
            subsOffered = true
            subsLauncher.launch(
                Intent(this, SubsAct::class.java)
                    .putExtra(SubsAct.EXTRA_FROM_ONBOARDING, true)
            )
            return
        }
        openLauncher()
    }

    // Cùng logic SubsAct.openLauncher: chưa mua thì hiện inter rồi mới vào launcher.
    private fun openLauncher() {
        var fired = false
        val continueToLauncher: () -> Unit = {
            if (!fired) {
                fired = true
                val asset = pendingWallpaperAsset
                pendingWallpaperAsset = null
                if (asset != null) applyWallpaperThenStartLauncher(asset) else startLauncherAndFinish()
            }
        }

        if (AdsSdk.isAdFree) {
            continueToLauncher()
            return
        }
        lifecycleScope.launch {
            AppAds.kit.showAdFullScreen(
                this@SelectBackgroundActivity,
                "start_launcher_Inter",
                object : AdFullScreenCallback {
                    override fun onFinished(result: AdFullScreenResult) {
                        super.onFinished(result)
                    }
                })

            lifecycle.withResumed {
                continueToLauncher()
            }
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res: ResolveInfo? =
            packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return res?.activityInfo?.packageName == packageName
    }

    companion object {
        private const val TAG = "SelectBackground"
        private const val KEY_SUBS_OFFERED = "subs_offered"
        // Tạm ẩn màn Subs sau onboarding: Skip / Set wallpaper -> inter -> launcher. Bật lại = true.
        private const val SHOW_SUBS_ON_ONBOARDING = false
        const val HELLO_BG_ASSET_KEY = "HELLO_BG_ASSET"
        private const val PREF_PROMPT_SET_DEFAULT_ON_DESKTOP = "prompt_set_default_on_desktop"
    }
}
