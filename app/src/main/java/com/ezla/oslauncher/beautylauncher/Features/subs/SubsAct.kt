package com.ezla.oslauncher.beautylauncher.Features.subs

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.R
import com.ezla.oslauncher.beautylauncher.databinding.ActSubsBinding
import com.ezla.oslauncher.beautylauncher.extensions.launchActivity
import com.ezla.oslauncher.beautylauncher.utils.BillingUtils
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.ezt.v2.ezt.admobdemo.ads.placement.AdFullScreenCallback
import com.ezt.v2.ezt.admobdemo.ads.placement.AdFullScreenResult
import com.truongnt.ios.ioslite.common.config.AppAds
import com.truongnt.ios.ioslite.common.config.SharePrefUtils
import com.truongnt.ios.launcher.searchlauncher.SearchLauncher
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SubsAct : BaseActivity<ActSubsBinding>() {
    override val setViewBinding: ActSubsBinding
        get() = ActSubsBinding.inflate(layoutInflater)

    // Gói đang chọn — quyết định Continue mua Yearly hay Monthly. Mặc định Yearly theo layout.
    private var selectedIsYear = true

    private val apps: ArrayList<IapAppIcon> =
        arrayListOf(
            IapAppIcon(com.truongnt.ios.launcher.R.drawable.ic_app_gmail, "Mail"),
            IapAppIcon(com.truongnt.ios.launcher.R.drawable.ic_app_gmeet, "FaceTime"),
            IapAppIcon(com.truongnt.ios.launcher.R.drawable.ic_app_gallery, "Photo"),
            IapAppIcon(com.truongnt.ios.launcher.R.drawable.ic_app_maps, "Map"),
            IapAppIcon(com.truongnt.ios.launcher.R.drawable.ic_app_camera, "Camera"),
            IapAppIcon(com.ezla.oslauncher.beautylauncher.R.drawable.img_weather, "Weather"),
            IapAppIcon(com.ezla.oslauncher.beautylauncher.R.drawable.img_contact, "Contact"),
        )
    val adapter by lazy { IapAppIconAdapter(apps) }

    override fun initView() {
        // Vào từ luồng onboarding: back = thoát app (giống màn chọn hình nền), KHÔNG quay lại
        // màn trước — nếu không user bị kẹt vòng onboarding.
        if (intent.getBooleanExtra(EXTRA_FROM_ONBOARDING, false)) {
            onBackPressedDispatcher.addCallback {
                openLauncher()
            }
        }

        binding.apply {
            rcvApps.adapter = adapter

            MainScope().launch {
                delay(3000)
                icX.isVisible = true
            }
        }

        bindPrices()

        // Giá có thể chưa tải xong lúc mở màn (query billing là bất đồng bộ) — bind lại khi có.
        BillingUtils.onProductsLoaded = { runOnUiThread { bindPrices() } }

        // Mua/khôi phục thành công: từ onboarding thì vào thẳng launcher (finish sẽ trả về màn chọn
        // nền), chỗ khác thì đóng màn. Callback có thể gọi nhiều lần nên chặn bằng isFinishing.
        BillingUtils.onSubsChanged = { isSubs ->
            runOnUiThread {
                if (isSubs && !isFinishing) {
                    if (intent.getBooleanExtra(EXTRA_FROM_ONBOARDING, false)) openLauncher() else finish()
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            icX.setOnClickListener {
                if (intent.getBooleanExtra(EXTRA_FROM_ONBOARDING, false)) {
                    onBackPressedDispatcher.onBackPressed()
                }else finish()
            }

            viewYear.setOnClickListener {
                selectedIsYear = true
                viewYear.setBackgroundResource(R.drawable.bg_iap_card_selected)
                icDotYear.setImageResource(R.drawable.ic_subs__pack_dot_select)

                viewMonth.setBackgroundResource(R.drawable.bg_c24_fff)
                icDotMonth.setImageResource(R.drawable.ic_subs__pack_dot)
            }

            viewMonth.setOnClickListener {
                selectedIsYear = false
                viewMonth.setBackgroundResource(R.drawable.bg_iap_card_selected)
                icDotMonth.setImageResource(R.drawable.ic_subs__pack_dot_select)

                viewYear.setBackgroundResource(R.drawable.bg_c24_fff)
                icDotYear.setImageResource(R.drawable.ic_subs__pack_dot)
            }

            btnContinue.setOnClickListener {
                val product = if (selectedIsYear) BillingUtils.Yearly else BillingUtils.Monthly
                if (product == null) {
                    // Chưa có ProductDetails: chưa điền productId, hoặc billing chưa query xong.
                    toast(R.string.subs_product_not_ready)
                } else {
                    BillingUtils.launchPurchaseSubs(this@SubsAct, product)
                }
            }

            tvRestore.setOnClickListener {
                BillingUtils.checkSubs(this@SubsAct) { isSubs ->
                    // finish khi isSubs do onSubsChanged lo; ở đây chỉ báo kết quả cho user.
                    runOnUiThread {
                        toast(if (isSubs) R.string.subs_restore_success else R.string.subs_restore_none)
                    }
                }
            }

            tvTc.setOnClickListener { openWeb(URL_TERMS) }
            tvPrivacy.setOnClickListener { openWeb(URL_PRIVACY) }
        }
    }

    override fun dataObservable() {}

    // Đổ giá từ ProductDetails; chưa có giá thật thì GIỮ NGUYÊN placeholder trong layout.
    private fun bindPrices() {
        BillingUtils.formattedPrice(BillingUtils.Yearly)?.let { binding.tvYearPrices.text = it }
        BillingUtils.formattedPrice(BillingUtils.Monthly)?.let { binding.tvMonthPrices.text = it }
    }

    private fun openWeb(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        // Gỡ callback global kẻo giữ tham chiếu Activity gây leak.
        BillingUtils.onSubsChanged = null
        BillingUtils.onProductsLoaded = null
        super.onDestroy()
    }

    private val PREF_PROMPT_SET_DEFAULT_ON_DESKTOP = "prompt_set_default_on_desktop"

    fun openLauncher() {
        var fired = false
        val continueToLauncher: () -> Unit = {
            if (!fired) {
                fired = true
                val isDefault = isDefaultLauncher()
                if (isDefault) {
                    launchActivity<SearchLauncher> { }
                    finishAffinity()
                } else {
                    SharePrefUtils.putBoolean(this, "hello_pending", true)
                    SharePrefUtils.putBoolean(this, PREF_PROMPT_SET_DEFAULT_ON_DESKTOP, true)
                    launchActivity<SearchLauncher> { }
                    finishAffinity()
                }
            }
        }

        if (AdsSdk.isAdFree) {
            continueToLauncher()
            return
        }
        lifecycleScope.launch {
            AppAds.kit.showAdFullScreen(
                this@SubsAct,
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


    // Public để dialog ưu đãi (SubsOfferDialog) dùng lại đúng 2 link này, khỏi chép trùng.
    companion object {
        /** Extra: màn được mở từ luồng onboarding đầu tiên (back = thoát app, không quay lại). */
        const val EXTRA_FROM_ONBOARDING = "subs_from_onboarding"

        // TODO(url): thay bằng link T&C / Privacy thật của app.
        const val URL_PRIVACY =
            "https://docs.google.com/document/d/1MQhESaXwlgu5Gx9JWXSfXQMGBaaWCJBs-ochf5Cng3Y/edit?tab=t.0"
        const val URL_TERMS = URL_PRIVACY
    }
}
