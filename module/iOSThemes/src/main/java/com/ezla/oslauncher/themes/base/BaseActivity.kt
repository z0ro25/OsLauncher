package com.ezla.oslauncher.themes.base

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
import com.ezla.oslauncher.themes.extensions.hideNavigation
import com.ezla.oslauncher.themes.extensions.showNav
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.truongnt.ios.ioslite.common.IosLocale
import com.truongnt.ios.ioslite.common.config.AppAds
import com.truongnt.ios.ioslite.common.config.SharePrefUtils

// Bản rút gọn BaseActivity của :app: bỏ luồng mất mạng + rate dialog vì dùng class của :app
// (library/module không thấy được). Locale lấy qua IosLocale, cùng prefs "data"/KEY_LANGUAGE.
abstract class BaseActivity<b : ViewBinding> : AppCompatActivity() {
    lateinit var binding: b
    abstract val setViewBinding: b

    //setupView here
    abstract fun initView()

    //listen to user action here
    abstract fun viewListener()

    //listen to database change here
    abstract fun dataObservable()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(IosLocale.wrapLocale(newBase))
    }

    // Đổ banner placement "bannerinapp" vào [container], giống BaseActivity của :app.
    protected fun showBannerIfEnabled(container: ViewGroup, isColapse: Boolean) {
        if (AdsSdk.isAdFree) return
        AppAds.kit.showBanner(this, "bannerinapp", container, isColapse)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                top = bars.top,
                bottom = imeBottom
            )

            insets
        }

        initView()
        viewListener()
        dataObservable()
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
    }

    fun showInterSave(action: () -> Unit) {
        action.invoke()
    }

    companion object {
        private const val IS_HIDE_NAV = "IS_HIDE_NAV"
    }
}
