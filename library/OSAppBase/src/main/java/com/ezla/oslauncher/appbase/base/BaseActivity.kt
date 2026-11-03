package com.ezla.oslauncher.appbase.base

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import com.ezla.oslauncher.appbase.extensions.hideNavigation
import com.ezla.oslauncher.appbase.extensions.showNav
//import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
//import com.ezt.v2.ezt.admobdemo.ads.placement.BannerAdOptions
//import com.ezt.v2.ezt.admobdemo.ads.placement.BannerSize
import com.truongnt.ios.ioslite.common.IosLocale
//import com.truongnt.ios.ioslite.common.config.AppAds
import com.truongnt.ios.ioslite.common.config.SharePrefUtils

// Base dùng chung cho các app tiện ích (Calculator / Compass / Clock), copy từ base của iOSThemes.
// Locale qua IosLocale (cùng prefs "data"/KEY_LANGUAGE của :app) nên đổi ngôn ngữ áp chung.
abstract class BaseActivity<b : ViewBinding> : AppCompatActivity() {
    lateinit var binding: b
    abstract val setViewBinding: b

    //setupView here
    abstract fun initView()

    //listen to user action here
    abstract fun viewListener()

    //listen to database change here
    abstract fun dataObservable()

    // Màn tràn viền tự xử lý inset (nền đen phủ cả status bar như Figma) thì trả false.
    protected open val padStatusBar: Boolean = true

    // Placement banner của từng app tiện ích (key trong JSON cấu hình ads).
    protected open val bannerPlacement: String = DEFAULT_BANNER_PLACEMENT

    private var appliedLanguage = ""

    override fun attachBaseContext(newBase: Context) {
        appliedLanguage = IosLocale.getSavedLanguage(newBase) ?: ""
        super.attachBaseContext(IosLocale.wrapLocale(newBase))
    }

    // Đổ banner placement [bannerPlacement] vào [container] (khối frBanner bọc @layout/ads_banner),
    // giống BaseActivity của :app. ads_banner nhúng sẵn shimmer -> subs / chưa có kit thì tự ẩn khối.
    // 1.2.0: tắt ads -> luôn ẩn khối banner, phần gọi SDK comment lại.
    protected fun showBannerIfEnabled(container: ViewGroup, isColapse: Boolean) {
        container.visibility = View.GONE
//        if (AdsSdk.isAdFree || !AppAds.hasKit()) {
//            container.visibility = View.GONE
//            return
//        }
//        AppAds.kit.showBanner(
//            this, bannerPlacement, container,
//            BannerAdOptions(collapsible = isColapse, bannerSize = BannerSize.STANDARD)
//        )
//        bannerContainer = container
//        if (FullscreenAdTracker.showing) onFullscreenAdChanged(true)
    }

    // Như showBannerIfEnabled nhưng frBanner đè lên [content] (main_layout): padding đáy của content
    // = phần SDK báo qua onReservedHeightChanged, nên collapse bung ra không đẩy nội dung.
    // 1.2.0: tắt ads -> luôn ẩn khối banner, phần gọi SDK comment lại.
    protected fun showBannerOverlay(container: ViewGroup, content: View, isColapse: Boolean) {
        container.visibility = View.GONE
//        if (AdsSdk.isAdFree || !AppAds.hasKit()) {
//            container.visibility = View.GONE
//            return
//        }
//        AppAds.kit.showBanner(
//            this, bannerPlacement, container,
//            BannerAdOptions(
//                collapsible = isColapse,
//                bannerSize = BannerSize.STANDARD,
//                onReservedHeightChanged = { h -> content.updatePadding(bottom = h) }
//            )
//        )
//        bannerContainer = container
//        if (FullscreenAdTracker.showing) onFullscreenAdChanged(true)
    }

    private var bannerContainer: ViewGroup? = null
    private var bannerHiddenByAd = false
    private val fullscreenAdListener: (Boolean) -> Unit = { onFullscreenAdChanged(it) }

    // AdActivity trong suốt -> banner màn dưới lộ ra đè lên App Open: ẩn trong lúc ad full màn hiện.
    // Chỉ hiện lại banner do chính mình ẩn, không đè trạng thái GONE mà SDK/subs đã đặt.
    private fun onFullscreenAdChanged(showing: Boolean) {
        val container = bannerContainer ?: return
        if (showing) {
            if (container.visibility != View.VISIBLE) return
            container.visibility = View.INVISIBLE
            bannerHiddenByAd = true
        } else if (bannerHiddenByAd) {
            container.visibility = View.VISIBLE
            bannerHiddenByAd = false
        }
    }

    // Trả true khi màn đã tự mở màn khác thay mình (vd onboarding lần đầu): base finish ngay,
    // không inflate / initView. Mặc định false nên màn cũ không đổi hành vi.
    protected open fun redirectBeforeCreate(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (redirectBeforeCreate()) {
            finish()
            return
        }
        binding = setViewBinding
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            // Pad the top by the status bar height so it never overlaps the content. The bottom
            // draws edge-to-edge behind the navigation bar, only padding for the keyboard when visible.
            view.updatePadding(
                top = if (padStatusBar) bars.top else 0,
                bottom = imeBottom
            )

            insets
        }

        // 1.2.0: tắt ads -> không theo dõi AdActivity nữa.
//        FullscreenAdTracker.register(application)
//        FullscreenAdTracker.addListener(fullscreenAdListener)

        initView()
        viewListener()
        dataObservable()
    }

    override fun onDestroy() {
//        FullscreenAdTracker.removeListener(fullscreenAdListener)
        bannerContainer = null
        super.onDestroy()
    }

    fun showActivity(activity: Class<*>, bundle: Bundle?) {
        val intent = Intent(this, activity)
        intent.putExtras(bundle ?: Bundle())
        startActivity(intent)
    }

    fun showActivityCustom(activity: Class<*>, bundle: Bundle?) {
        val intent = Intent(this, activity)
        intent.putExtras(bundle ?: Bundle())
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        // Cùng key với Constant.IS_HIDE_NAV của :app để cài đặt ẩn thanh điều hướng áp chung.
        if (SharePrefUtils.getBoolean(this, IS_HIDE_NAV, true)) {
            window?.hideNavigation()
        } else window?.showNav()

        // Đổi ngôn ngữ ở màn Language trong lúc màn này còn trong stack -> dựng lại theo locale mới.
        val saved = IosLocale.getSavedLanguage(this) ?: ""
        if (saved != appliedLanguage) {
            appliedLanguage = saved
            window.decorView.post { if (!isFinishing) recreate() }
        }
    }

    fun showInterSave(action: () -> Unit) {
        action.invoke()
    }

    companion object {
        private const val IS_HIDE_NAV = "IS_HIDE_NAV"
        // Dùng chung banner trong app của :app khi màn con không khai placement riêng.
        private const val DEFAULT_BANNER_PLACEMENT = "bannerinapp"
    }
}
