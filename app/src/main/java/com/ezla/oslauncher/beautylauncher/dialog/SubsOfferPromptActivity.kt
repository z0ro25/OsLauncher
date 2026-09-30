package com.ezla.oslauncher.beautylauncher.dialog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ezla.oslauncher.beautylauncher.utils.BillingUtils

/**
 * Activity TRONG SUỐT host dialog ưu đãi sub ngay trên desktop launcher.
 * SearchLauncher mở qua ComponentName (không phụ thuộc ngược :app): 1 lần/ngày, chỉ khi chưa mua.
 * Đóng dialog -> finish, trả về desktop.
 */
class SubsOfferPromptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Chốt lại lần nữa: người đã mua không bị mời mua (desktop cũng đã kiểm tra trước khi mở).
        if (BillingUtils.isSubsCached(this)) {
            finish()
            return
        }
        SubsOfferDialog().apply {
            onDismissed = { finish() }
        }.show(supportFragmentManager, SubsOfferDialog.TAG)
    }
}
