package com.ezla.oslauncher.beautylauncher.Features.subs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.isVisible
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.R
import com.ezla.oslauncher.beautylauncher.databinding.ActSubsBinding
import com.ezla.oslauncher.beautylauncher.utils.BillingUtils
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
            onBackPressedDispatcher.addCallback { finishAffinity() }
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

        // Mua/khôi phục thành công thì đóng màn; premium đã được BillingUtils.setPremium lo.
        BillingUtils.onSubsChanged = { isSubs -> runOnUiThread { if (isSubs) finish() } }
    }

    override fun viewListener() {
        binding.apply {
            icX.setOnClickListener { finish() }

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
