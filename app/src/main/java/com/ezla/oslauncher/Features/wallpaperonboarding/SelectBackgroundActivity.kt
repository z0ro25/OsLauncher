package com.ezla.oslauncher.Features.wallpaperonboarding

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.ezla.oslauncher.Base.BaseActivity
import com.ezla.oslauncher.Features.home.HomeActivity
import com.ezla.oslauncher.databinding.ActivitySelectBackgroundBinding
import com.ezla.oslauncher.extensions.launchActivity
import com.ezla.oslauncher.theme.AppThemeManager
import com.ezla.oslauncher.tool.sharePreferenceTool.SharePrefUtils

/**
 * Màn "Chọn hình nền" của onboarding — chèn sau PermissionActivity, trước HomeActivity.
 *
 * - Nguồn ảnh: các file trong assets/wallpapers/ (người dùng tự bổ sung sau);
 *   danh sách được liệt kê động lúc chạy nên không cần sửa code khi thêm ảnh.
 * - "Đặt làm hình nền": set ảnh đang chọn cho MÀN HÌNH CHÍNH (FLAG_SYSTEM) rồi vào Home.
 * - "Bỏ qua": vào thẳng Home, không đổi hình nền.
 *
 * Bất biến: chỉ điều hướng tiến tới Home (bước cuối onboarding); không đụng lock screen.
 */
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

    override fun initView() {
        loadWallpaperAssets()
        setupViewPager()

        onBackPressedDispatcher.addCallback {
            // Onboarding không cho lùi về Permission; back = bỏ qua vào Home.
            goToHome()
        }
    }

    override fun viewListener() {
        binding.tvSkip.setOnClickListener { goToHome() }

        binding.tvSetWallpaper.setOnClickListener {
            if (wallpaperAssets.isEmpty()) {
                // Chưa có ảnh nào trong assets -> coi như bỏ qua.
                goToHome()
                return@setOnClickListener
            }
            setWallpaperAndGoHome(wallpaperAssets[currentPos])
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

            if (wallpaperAssets.isNotEmpty()){
                binding.vpWallpaper.currentItem = currentPos
            }
        }
    }

    /** Set ảnh cho màn hình chính rồi vào Home. Giải mã + set trên thread nền để không nghẽn UI. */
    private fun setWallpaperAndGoHome(assetPath: String) {
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
                    goToHome()
                }
            }
        }.start()
    }

    private fun goToHome() {
        launchActivity<HomeActivity> { }
        finish()
    }

    companion object {
        /** Key SharePref lưu đường dẫn asset hình nền onboarding đã chọn — màn Hello dùng lại. */
        const val HELLO_BG_ASSET_KEY = "HELLO_BG_ASSET"
    }
}
