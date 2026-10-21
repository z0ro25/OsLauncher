package com.ezla.oslauncher.beautylauncher.dialog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ezla.oslauncher.beautylauncher.utils.BillingUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            onDismissed = {
                markShownToday()
                finish()
            }
        }.show(supportFragmentManager, SubsOfferDialog.TAG)
    }

    // Ghi khi user đóng dialog (không phải lúc mở): launcher bị dựng lại giữa chừng thì còn lượt.
    // Key/file phải khớp SearchLauncher.subsOfferKeyToday() (iOSLiteCommon không đọc được :app).
    private fun markShownToday() {
        val key = "subs_offer_" + SimpleDateFormat("ddMMyyyy", Locale.US).format(Date())
        getSharedPreferences(packageName, MODE_PRIVATE).edit().putBoolean(key, true).commit()
    }
}
