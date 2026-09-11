package com.ezla.oslauncher.Features.wallpaperonboarding

import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
import com.ezla.oslauncher.databinding.ActivitySelectBackgroundBinding
import com.ezla.oslauncher.extensions.launchActivity
import com.ezla.oslauncher.theme.AppThemeManager
import com.ezla.oslauncher.tool.sharePreferenceTool.SharePrefUtils
import com.truongnt.ios.launcher.searchlauncher.SearchLauncher

/**
 * Màn "Chọn hình nền" — BƯỚC CUỐI của onboarding lần đầu vào app.
 *
 * - Nguồn ảnh: các file trong assets/wallpapers/ (người dùng tự bổ sung sau);
 *   danh sách được liệt kê động lúc chạy nên không cần sửa code khi thêm ảnh.
 * - "Start Launcher": set ảnh đang chọn cho MÀN HÌNH CHÍNH (FLAG_SYSTEM) rồi vào THẲNG LAUNCHER.
 * - "Bỏ qua": vào thẳng launcher, không đổi hình nền.
 *
 * [ĐỔI LUỒNG] Trước đây cả 2 nút đều dẫn sang HomeActivity (màn cài đặt). Nay lần đầu vào app đi
 * thẳng ra launcher cho người dùng thấy ngay thành quả; màn Home chỉ mở khi người dùng chủ động
 * vào app settings sau này.
 *
 * Bất biến: không đụng lock screen.
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
            // Onboarding không cho lùi về Permission; back = bỏ qua, vào thẳng launcher.
            goToLauncher()
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

            if (wallpaperAssets.isNotEmpty()){
                binding.vpWallpaper.currentItem = currentPos
            }
        }
    }

    /**
     * Set ảnh cho màn hình chính rồi vào THẲNG launcher.
     * Giải mã + set trên thread nền để không nghẽn UI; dù set thành công hay lỗi vẫn đi tiếp
     * (khối finally) — không chặn người dùng lại ở màn onboarding.
     */
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

    /**
     * Kết thúc onboarding: vào THẲNG launcher (không qua màn Home nữa).
     *
     * Dùng lại đúng logic của HomeActivity.goToLauncher() để hai đường vào launcher hành xử giống
     * nhau:
     * - ĐÃ là launcher mặc định: mở desktop rồi đóng app (finishAffinity) — không còn gì để làm
     *   trong app settings.
     * - CHƯA là default: đặt cờ để desktop hiện màn Hello và nhắc lại dialog "Set as default
     *   launcher"; KHÔNG đóng app để người dùng còn quay lại được.
     */
    private fun goToLauncher() {
        val isDefault = isDefaultLauncher()
        if (isDefault) {
            launchActivity<SearchLauncher> { }
            finishAffinity()
        } else {
            SharePrefUtils.putBoolean(this, "hello_pending", false)
            SharePrefUtils.putBoolean(this, PREF_PROMPT_SET_DEFAULT_ON_DESKTOP, true)
            launchActivity<SearchLauncher> { }
            finishAffinity()
        }
    }

    /**
     * App hiện có đang là launcher mặc định không. Resolve HOME intent rồi so package — đúng ở MỌI
     * API (RoleManager chỉ có từ Q trở lên).
     */
    private fun isDefaultLauncher(): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res: ResolveInfo? =
            packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return res?.activityInfo?.packageName == packageName
    }

    companion object {
        /** Key SharePref lưu đường dẫn asset hình nền onboarding đã chọn — màn Hello dùng lại. */
        const val HELLO_BG_ASSET_KEY = "HELLO_BG_ASSET"

        /**
         * Cờ dùng-1-lần: vào launcher khi CHƯA là default -> desktop hiện lại dialog Set default.
         * Phải TRÙNG tên với hằng cùng tên trong HomeActivity (SearchLauncher đọc chung key này).
         */
        private const val PREF_PROMPT_SET_DEFAULT_ON_DESKTOP = "prompt_set_default_on_desktop"
    }
}
