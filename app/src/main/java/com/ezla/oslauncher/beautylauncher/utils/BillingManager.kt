package com.ezla.oslauncher.beautylauncher.utils

import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams

class BillingManager(context: Context, private val listener: PurchaseListener) :
    PurchasesUpdatedListener {

    // Giữ applicationContext: BillingClient sống suốt vòng đời process, giữ Activity sẽ leak.
    private val appContext = context.applicationContext

    lateinit var billingClient: BillingClient

    init {
        startBillingConnection()
    }

    // Khởi tạo kết nối với Google Play Billing
    private fun startBillingConnection() {
        val pendingPurchaseParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .enablePrepaidPlans()
            .build()

        billingClient = BillingClient.newBuilder(appContext)
            .setListener(this)
            .enableAutoServiceReconnection()
            .enablePendingPurchases(pendingPurchaseParams)
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                // Thử kết nối lại nếu bị ngắt kết nối
                Log.e(TAG, "Billing Service Disconnected")
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                // Phải log CẢ nhánh lỗi: setup fail (app không cài từ Play, chưa có license
                // tester) thì queryAvailableProducts() không bao giờ chạy -> giá không hiện,
                // mà trước đây im lặng nên không có manh mối nào để chẩn đoán.
                Log.d(
                    TAG,
                    "onBillingSetupFinished: code=${billingResult.responseCode} msg=${billingResult.debugMessage}"
                )
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    listener.onBillingConnection()
                    queryAvailableProducts()
                }
            }
        })
    }

    // Lấy danh sách sản phẩm có sẵn từ Google Play. Hiện chỉ có 2 gói sub tháng/năm —
    // thêm gói mới thì bổ sung vào productList dưới đây.
    private fun queryAvailableProducts() {
        val productIds = listOf(BillingUtils.SubsMonth, BillingUtils.SubsYear)
            .filter { it.isNotBlank() }
        if (productIds.isEmpty()) {
            Log.w(TAG, "Chưa điền productId sub — bỏ qua queryProductDetails")
            return
        }

        val productList: MutableList<QueryProductDetailsParams.Product> = ArrayList()
        productIds.forEach { id ->
            productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, skuDetailsList ->
            val details = skuDetailsList?.productDetailsList.orEmpty()
            Log.d(
                TAG,
                "queryProductDetails: code=${billingResult.responseCode} " +
                        "msg=${billingResult.debugMessage} size=${details.size}"
            )
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                skuDetailsList != null
            ) {
                // OK mà rỗng: ID sai, product chưa active, hoặc sub chưa có base plan.
                if (details.isEmpty()) {
                    Log.e(TAG, "Play trả RỖNG cho $productIds — kiểm tra lại trên Play Console")
                }
                listener.onSkuDetailsRetrieved(details)
            } else {
                Log.e(
                    TAG,
                    "queryProductDetails lỗi: ${billingResult.responseCode} ${billingResult.debugMessage}"
                )
            }
        }
    }

    // Xử lý kết quả mua hàng
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            listener.onPurchasesCompleted(purchases)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase Canceled")
        } else {
            Log.d(TAG, "Error: ${billingResult.debugMessage}")
        }
    }

    fun release() {
        if (::billingClient.isInitialized && billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    interface PurchaseListener {
        fun onSkuDetailsRetrieved(skuDetailsList: List<ProductDetails?>)
        fun onPurchasesCompleted(purchases: List<Purchase>)
        fun onBillingConnection()
    }

    private companion object {
        const val TAG = "BillingManager"
    }
}
