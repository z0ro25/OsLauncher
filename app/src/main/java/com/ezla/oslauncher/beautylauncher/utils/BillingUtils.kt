package com.ezla.oslauncher.beautylauncher.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.truongnt.ios.ioslite.common.ads.Ads
import com.truongnt.ios.ioslite.common.config.SharePrefUtils
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object BillingUtils {

    private const val TAG = "BillingUtils"

    // TODO(productId): điền ID thật lấy từ Play Console. Để rỗng thì bỏ qua không query.
    const val SubsMonth = "miospro_monthly"
    const val SubsYear = "miospro_yearly"

    // Cờ "đang có sub" — kênh duy nhất để các module khác biết user đã mua. Ghi ở saveSubs(),
    // đọc sớm ở isSubsCached() ngay trong Application.onCreate (trước khi BillingClient kịp nối).
    private const val KEY_PREMIUM_SUBS = "premium_subs"

    var billing: BillingManager? = null
        private set

    var Monthly: ProductDetails? = null
        private set

    var Yearly: ProductDetails? = null
        private set

    /** Báo khi trạng thái sub đổi (mua xong / hết hạn / mất quyền). Null = không ai quan tâm. */
    var onSubsChanged: ((Boolean) -> Unit)? = null

    /** Báo khi đã lấy xong ProductDetails — để màn paywall bind lại giá. Chạy trên main thread. */
    var onProductsLoaded: (() -> Unit)? = null

    /**
     * Khởi tạo billing, gọi một lần ở Application.onCreate.
     *
     * Gọi lại lần hai bị bỏ qua: mỗi BillingManager là một BillingClient riêng, tạo thêm sẽ
     * rò rỉ kết nối và nhân đôi callback.
     */
    fun initBilling(context: Context) {
        if (billing != null) return
        val appContext = context.applicationContext
        billing = BillingManager(appContext, object : BillingManager.PurchaseListener {
            override fun onSkuDetailsRetrieved(skuDetailsList: List<ProductDetails?>) {
                skuDetailsList.forEach { details ->
                    when (details?.productId) {
                        SubsMonth -> Monthly = details
                        SubsYear -> Yearly = details
                        // ID lạ = gõ sai hằng số hoặc Play trả product khác -> giá sẽ không bind.
                        else -> Log.w(TAG, "productId không khớp hằng số: ${details?.productId}")
                    }
                    Log.d(TAG, "product: ${details?.productId} offer=${details?.subscriptionOfferDetails?.size}")
                }
                onProductsLoaded?.invoke()
            }

            override fun onPurchasesCompleted(purchases: List<Purchase>) {
                purchases.forEach { handlePurchase(appContext, it) }
            }

            override fun onBillingConnection() {
                // Mỗi lần nối được Play là query lại: hết hạn, hoàn tiền hay đổi máy đều phải
                // cập nhật cờ, nên không tin cờ đã lưu mà lấy sự thật từ Google.
                checkSubs(appContext, null)
            }
        })
    }

    /** Đọc cờ sub đã lưu — dùng ở Application.onCreate để chặn quảng cáo ngay từ cold start. */
    fun isSubsCached(context: Context): Boolean =
        SharePrefUtils.getBoolean(context, KEY_PREMIUM_SUBS, false)

    /**
     * Query sub đang active từ Google rồi cập nhật cờ.
     *
     * Query lỗi (mất mạng / Play trục trặc) thì GIỮ NGUYÊN cờ đã lưu, không xoá — nếu không,
     * user đã trả tiền sẽ thấy quảng cáo quay lại chỉ vì một lần gọi thất bại.
     */
    fun checkSubs(context: Context, action: ((Boolean) -> Unit)? = null) {
        val appContext = context.applicationContext
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billing?.billingClient?.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "queryPurchases lỗi: ${result.responseCode} ${result.debugMessage}")
                action?.invoke(isSubsCached(appContext))
                return@queryPurchasesAsync
            }
            val isSubs = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            saveSubs(appContext, isSubs)
            action?.invoke(isSubs)
        }
    }

    /**
     * Giá hiển thị của một gói sub. Bỏ qua phase khuyến mãi giá 0 (free trial), lấy phase
     * tính tiền cuối cùng — đó mới là giá định kỳ user thực trả.
     */
    fun formattedPrice(details: ProductDetails?): String? {
        val phases = details?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList ?: return null
        return (phases.lastOrNull { it.priceAmountMicros > 0 } ?: phases.lastOrNull())?.formattedPrice
    }

    /**
     * Giá GỐC (số gạch ngang) của offer 50% = giá bán × 2 — con số marketing, KHÔNG phải giá Play
     * trả về. Format theo currency của phase nên vẫn đúng ký hiệu tiền tệ của từng thị trường.
     */
    fun formattedOriginalPrice(details: ProductDetails?): String? {
        val phases = details?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList ?: return null
        val phase = phases.lastOrNull { it.priceAmountMicros > 0 } ?: return null
        val currency = try {
            Currency.getInstance(phase.priceCurrencyCode)
        } catch (e: IllegalArgumentException) {
            return null
        }
        val amount = BigDecimal.valueOf(phase.priceAmountMicros)
            .divide(BigDecimal.valueOf(1_000_000))
            .multiply(BigDecimal.valueOf(2))
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            this.currency = currency
            maximumFractionDigits = if (amount.stripTrailingZeros().scale() <= 0) 0 else 2
        }.format(amount)
    }

    /** Mở luồng mua sub. Gọi từ màn paywall khi user bấm mua. */
    fun launchPurchaseSubs(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Log.e(TAG, "Không có offerToken cho ${productDetails.productId}")
            return
        }
        val productList: MutableList<BillingFlowParams.ProductDetailsParams> = ArrayList()
        productList.add(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productList)
            .build()
        billing?.billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Xử lý một purchase sub vừa mua xong.
     *
     * Sub bắt buộc phải acknowledge trong 3 ngày, quá hạn là Google tự hoàn tiền. Chỉ ghi cờ
     * khi purchaseState đã là PURCHASED — PENDING (chờ xác nhận thanh toán) chưa tính là mua.
     */
    private fun handlePurchase(context: Context, purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.isAcknowledged) {
            saveSubs(context, true)
            return
        }
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billing?.billingClient?.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                saveSubs(context, true)
            } else {
                Log.e(TAG, "acknowledge lỗi: ${result.responseCode} ${result.debugMessage}")
            }
        }
    }

    // Ghi cờ rồi đẩy sang Ads. Phải gọi Ads.setPremium mỗi lần đổi vì FSDAds chốt cờ premium
    // theo giá trị tại lúc setupConfig, không nhận lambda sống (xem Ads.setPremium).
    private fun saveSubs(context: Context, isSubs: Boolean) {
        SharePrefUtils.putBoolean(context, KEY_PREMIUM_SUBS, isSubs)
        Ads.setPremium(isSubs, false)
        onSubsChanged?.invoke(isSubs)
    }
}
