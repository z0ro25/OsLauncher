package com.ezla.oslauncher.themes.data

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.annotation.DrawableRes
import com.ezla.oslauncher.themes.Api.ApiLog
import com.ezla.oslauncher.themes.Models.CoinPack
import com.ezla.oslauncher.themes.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Gói xu IAP (in-app tiêu hao). Billing nằm ở :app (BillingUtils) nên module chỉ khai gói + cộng xu;
// :app gắn [store] để lấy giá / mở luồng mua, rồi gọi [credit] khi Play báo mua xong.
object CoinPacks {

    private class Def(
        val productId: String,
        val coins: Int,
        // type gửi POST /credits để server biết xu đến từ gói nào.
        val creditType: String,
        val fallbackPrice: String,
        @DrawableRes val icon: Int
    )

    private val PACKS = listOf(
        Def("theme_coin_pack1", 10, "theme-coin-pack1", "$0.79", R.drawable.themes_ic_coins_500),
        Def("theme_coin_pack2", 30, "theme-coin-pack2", "$1.99", R.drawable.themes_ic_coins_1500),
        Def("theme_coin_pack3", 100, "theme-coin-pack3", "$5.99", R.drawable.themes_ic_coins_2500),
        Def("theme_coin_pack4", 250, "theme-coin-pack4", "$9.99", R.drawable.themes_ic_coins_3500),
        Def("theme_coin_pack5", 500, "theme-coin-pack5", "$19.99", R.drawable.themes_ic_coins_5000),
        Def("theme_coin_pack6", 1250, "theme-coin-pack6", "$39.99", R.drawable.themes_ic_coins_6000)
    )

    // Cầu nối sang billing của :app; null = app chưa gắn (giá dự phòng, bấm mua báo chưa sẵn sàng).
    interface Store {
        fun price(productId: String): String?
        // false = chưa có thông tin gói từ Play (chưa nối được / ID chưa active).
        fun launchPurchase(activity: Activity, productId: String): Boolean
        // Xử lý giao dịch xu đã trả tiền nhưng chưa cộng/consume (kẹt thì Play chặn mua lại gói đó).
        fun syncPending() {}
    }

    var store: Store? = null

    // Màn Rewards nghe: giá Play tải xong / đã cộng xu (coins vừa cộng, số dư mới). Gọi trên main.
    var onPricesLoaded: (() -> Unit)? = null
    var onCoinsAdded: ((coins: Int, balance: Int) -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())

    val productIds: List<String> get() = PACKS.map { it.productId }

    fun isCoinPack(productId: String) = PACKS.any { it.productId == productId }

    // Giá lấy từ Play (đúng tiền tệ của máy); chưa có thì dùng giá trong bảng.
    fun packs(): List<CoinPack> = PACKS.map {
        CoinPack(it.productId, it.coins, store?.price(it.productId) ?: it.fallbackPrice, it.icon)
    }

    fun launchPurchase(activity: Activity, productId: String): Boolean =
        store?.launchPurchase(activity, productId) ?: false

    fun syncPending() {
        store?.syncPending()
    }

    // Cộng xu cho 1 giao dịch (coins x quantity) qua POST /credits; [done] chạy trên main.
    // Bên gọi chỉ consume giao dịch khi kết quả thành công, để lỗi thì lần sau cộng lại được.
    fun credit(productId: String, quantity: Int, done: (Result<Int>) -> Unit) {
        val def = PACKS.firstOrNull { it.productId == productId }
        if (def == null) {
            done(Result.failure(IllegalArgumentException("unknown coin pack $productId")))
            return
        }
        val coins = def.coins * quantity.coerceAtLeast(1)
        scope.launch {
            val result = CreditRepository.addCredit(coins, def.creditType)
            result.onFailure { ApiLog.e("coin pack $productId: cộng xu lỗi ${it.message}", it) }
            main.post {
                done(result)
                result.onSuccess { balance -> onCoinsAdded?.invoke(coins, balance) }
            }
        }
    }

    fun notifyPricesLoaded() {
        main.post { onPricesLoaded?.invoke() }
    }
}
