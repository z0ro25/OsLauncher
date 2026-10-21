package com.ezla.oslauncher.beautylauncher.Features.subs

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.view.View
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

    override fun initView() {
        // Vào từ luồng onboarding: back = thoát app (giống màn chọn hình nền), KHÔNG quay lại
        // màn trước — nếu không user bị kẹt vòng onboarding.
        if (intent.getBooleanExtra(EXTRA_FROM_ONBOARDING, false)) {
            onBackPressedDispatcher.addCallback {
                openLauncher()
            }
        }

        binding.apply {
            MainScope().launch {
                delay(3000)
                icX.isVisible = true
            }
        }

        binding.root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> binding.root.post { placeContent() } }
        renderPlans()
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
                renderPlans()
            }

            viewMonth.setOnClickListener {
                selectedIsYear = false
                renderPlans()
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

    // Figma có 2 frame (chọn Yearly / chọn Monthly): thẻ chọn cao 168dp, nền + viền gradient, radio tích,
    // giá gradient, trial xanh, bóng xanh; thẻ kia 150dp nền trắng, giá đặc, trial xám, bóng nhạt.
    private fun renderPlans() {
        binding.apply {
            renderPlan(viewYear, icDotYear, tvYearPrices, tvYearTrial, selectedIsYear, YEAR_PRICE_UNSELECTED_SP, YEAR_PRICE_SOLID)
            renderPlan(viewMonth, icDotMonth, tvMonthPrices, tvMonthTrial, !selectedIsYear, MONTH_PRICE_UNSELECTED_SP, MONTH_PRICE_SOLID)
        }
        bindThenPrice()
    }

    private fun renderPlan(
        card: View,
        radio: android.widget.ImageView,
        price: android.widget.TextView,
        trial: android.widget.TextView,
        selected: Boolean,
        unselectedPriceSp: Float,
        unselectedPriceColor: Int
    ) {
        val density = resources.displayMetrics.density
        card.layoutParams = card.layoutParams.apply {
            height = ((if (selected) CARD_SELECTED_DP else CARD_NORMAL_DP) * density).toInt()
        }
        card.setBackgroundResource(if (selected) R.drawable.bg_iap_plan_selected else R.drawable.bg_iap_plan_normal)
        card.elevation = (if (selected) 8f else 2f) * density
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val shadow = if (selected) SHADOW_SELECTED else SHADOW_NORMAL
            card.outlineSpotShadowColor = shadow
            card.outlineAmbientShadowColor = shadow
        }
        radio.setImageResource(if (selected) R.drawable.ic_iap_radio_on else R.drawable.ic_iap_radio_off)

        price.textSize = if (selected) PRICE_SELECTED_SP else unselectedPriceSp
        price.setTextColor(if (selected) PRICE_GRADIENT_START else unselectedPriceColor)
        if (selected) price.post { applyPriceGradient(price) } else price.paint.shader = null
        price.invalidate()

        trial.setBackgroundResource(if (selected) R.drawable.bg_iap_trial_selected else R.drawable.bg_iap_trial_normal)
        trial.setTextColor(if (selected) TRIAL_TEXT_SELECTED else TRIAL_TEXT_NORMAL)
    }

    // Đổ giá / trial từ ProductDetails; chưa có giá thật thì để trống, trial thì ẩn.
    private fun bindPrices() {
        binding.apply {
            tvYearPrices.text = yearPrice()
            tvMonthPrices.text = monthPrice()
            tvYearPerMonth.text = getString(
                R.string.subs_per_month_approx,
                BillingUtils.formattedMonthlyFromYearly(BillingUtils.Yearly)
                    ?: getString(R.string.subs_price_year_monthly_fallback)
            )
            bindTrial(tvYearTrial, BillingUtils.freeTrialDays(BillingUtils.Yearly))
            bindTrial(tvMonthTrial, BillingUtils.freeTrialDays(BillingUtils.Monthly))
        }
        // Giá đổi độ dài -> vẽ lại gradient cho thẻ đang chọn.
        renderPlans()
    }

    private fun bindTrial(view: android.widget.TextView, days: Int?) {
        view.isVisible = days != null
        if (days != null) view.text = getString(R.string.subs_free_trial_days, days)
    }

    // Màn cao: giãn khoảng giữa gói và Continue để tiêu đề nằm ngay dưới hero như Figma. Màn ngắn:
    // khoảng giãn = 0, cụm nội dung (neo đáy) tự đè lên đáy hero. Hero không thu nhỏ / không cắt.
    private fun placeContent() {
        val root = binding.root
        val hero = binding.imgHero
        if (root.height == 0 || hero.height == 0) return
        val bottomBlock = binding.layoutBottom.height - binding.spaceGap.height
        val free = root.height - root.paddingTop - root.paddingBottom - hero.height - bottomBlock
        val gap = free.coerceAtLeast(0)
        if (binding.spaceGap.layoutParams.height != gap) {
            binding.spaceGap.layoutParams = binding.spaceGap.layoutParams.apply { height = gap }
        }
    }

    // Giá Play (đúng tiền tệ của máy); Play chưa trả ProductDetails thì dùng giá Figma để màn không trống.
    private fun yearPrice() = BillingUtils.formattedPrice(BillingUtils.Yearly) ?: getString(R.string.subs_price_year_fallback)

    private fun monthPrice() = BillingUtils.formattedPrice(BillingUtils.Monthly) ?: getString(R.string.subs_price_month_fallback)

    // "Then {giá}/year (month) • Cancel anytime" theo gói đang chọn.
    private fun bindThenPrice() {
        binding.tvThenPrice.text =
            if (selectedIsYear) getString(R.string.subs_then_per_year, yearPrice())
            else getString(R.string.subs_then_per_month, monthPrice())
    }

    private fun applyPriceGradient(tv: android.widget.TextView) {
        val width = tv.paint.measureText(tv.text.toString())
        if (width <= 0f) return
        tv.paint.shader = android.graphics.LinearGradient(
            0f, 0f, width, 0f, PRICE_GRADIENT_START, PRICE_GRADIENT_END, android.graphics.Shader.TileMode.CLAMP
        )
        tv.invalidate()
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

        private val PRICE_GRADIENT_START = android.graphics.Color.parseColor("#2563EB")
        private val PRICE_GRADIENT_END = android.graphics.Color.parseColor("#7C3AED")
        // Giá thẻ không chọn: Yearly #151515 / 24sp, Monthly #1F2937 / 21sp (2 frame Figma).
        private val YEAR_PRICE_SOLID = android.graphics.Color.parseColor("#151515")
        private val MONTH_PRICE_SOLID = android.graphics.Color.parseColor("#1F2937")
        private val TRIAL_TEXT_SELECTED = android.graphics.Color.parseColor("#15803D")
        private val TRIAL_TEXT_NORMAL = android.graphics.Color.parseColor("#475569")
        private val SHADOW_SELECTED = android.graphics.Color.parseColor("#3B6CF6")
        private val SHADOW_NORMAL = android.graphics.Color.parseColor("#1E2B4A")
        private const val CARD_SELECTED_DP = 168
        private const val CARD_NORMAL_DP = 150
        private const val PRICE_SELECTED_SP = 24f
        private const val YEAR_PRICE_UNSELECTED_SP = 24f
        private const val MONTH_PRICE_UNSELECTED_SP = 21f

        // TODO(url): thay bằng link T&C / Privacy thật của app.
        const val URL_PRIVACY =
            "https://docs.google.com/document/d/1MQhESaXwlgu5Gx9JWXSfXQMGBaaWCJBs-ochf5Cng3Y/edit?tab=t.0"
        const val URL_TERMS = URL_PRIVACY
    }
}
