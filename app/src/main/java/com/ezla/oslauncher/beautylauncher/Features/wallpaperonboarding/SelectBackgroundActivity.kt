package com.ezla.oslauncher.beautylauncher.Features.wallpaperonboarding

import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
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
        showBannerIfEnabled(binding.frBanner, false)
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
        binding.frLoading.isVisible = true
        // User chủ động chọn hình nền -> từ đây đổi mode không ghi đè ảnh của user.
        AppThemeManager.markUserWallpaper(this)
        // Lưu asset đã chọn để màn Hello dùng lại làm nền (thay ảnh bg_hello cứng).
        SharePrefUtils.putString(this, HELLO_BG_ASSET_KEY, assetPath)

        Thread {
            try {
                val bitmap = assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
                val wm = WallpaperManager.getInstance(applicationContext)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                } else {
                    wm.setBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Set wallpaper failed!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                runOnUiThread {
                    binding.frLoading.isVisible = false
                    goToLauncher()
                }
            }
        }.start()
    }

    private fun goToLauncher() {
        Log.d(TAG, "goToLauncher: offered=$subsOffered premium=${BillingUtils.isSubsCached(this)} inst=${hashCode()}")

        if (!subsOffered && !BillingUtils.isSubsCached(this)) {
            subsOffered = true
            subsLauncher.launch(
                Intent(this, SubsAct::class.java)
                    .putExtra(SubsAct.EXTRA_FROM_ONBOARDING, true)
            )
            return
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
        const val HELLO_BG_ASSET_KEY = "HELLO_BG_ASSET"
        private const val PREF_PROMPT_SET_DEFAULT_ON_DESKTOP = "prompt_set_default_on_desktop"
    }
}
