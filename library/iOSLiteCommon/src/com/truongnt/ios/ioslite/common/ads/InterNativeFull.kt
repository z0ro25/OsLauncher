package com.truongnt.ios.ioslite.common.ads

import android.app.Activity
import android.app.Dialog
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.truongnt.ios.ioslite.common.R
import com.truongnt.ios.ioslite.common.config.RemoteConfigs


object InterNativeFull {

    private const val TAG = "InterNativeFull"

    /** Interstitial của cặp này. */
    private val INTER_SLOT = AdsSlot.INTER_IN_APP

    /** Native full của cặp này. */
    private val NATIVE_SLOT = AdsSlot.NATIVE_DIALOG_FULL

    /** Native đã tải xong, sẵn sàng đổ vào dialog chưa. */
    @Volatile
    private var nativeReady = false

    /** Callback của lần [load] đang chờ kết quả native. Bắn đúng một lần rồi tự xoá. */
    @Volatile
    private var pendingCallback: AdsInterNativeFullCallback? = null

    /** Dialog đang mở (nếu có) — để [clear] dọn được, tránh rò window khi màn bị huỷ. */
    @Volatile
    private var currentDialog: Dialog? = null

    /**
     * Tải trước cả hai phần.
     *
     * Interstitial được phát request trước, nhưng **không chờ** nó xong mới load native: hai
     * request độc lập, chờ nhau chỉ làm chậm luồng vào màn mà không đổi kết quả.
     *
     * [callback] bắn sau khi native có kết quả — `onLoaded` hoặc `onLoadFailed`, không bao giờ
     * cả hai và không bao giờ không bắn. Gọi [load] lần nữa trước khi lần trước kết thúc thì
     * callback cũ bị bỏ (chỉ callback mới nhất được báo).
     */
    @JvmStatic
    fun load(activity: Activity, callback: AdsInterNativeFullCallback?) {
        pendingCallback = callback

        // (1) Interstitial TRƯỚC.
        if (RemoteConfigs.isAdsEnabled(activity, RemoteConfigs.INTER_INAPP)) {
            // Kết quả load inter không quyết định callback — xem doc lớp.
            AdsInterstitial.load(activity, INTER_SLOT) { }
        } else {
            Log.w(TAG, "load: Remote Config tắt ${RemoteConfigs.INTER_INAPP} — bỏ qua interstitial")
        }

        // (2) Native full SAU — chỗ bắn callback.
        if (!RemoteConfigs.isAdsEnabled(activity, RemoteConfigs.NATIVE_FULL)) {
            Log.w(TAG, "load: Remote Config tắt ${RemoteConfigs.NATIVE_FULL}")
            finishLoad(AdsError(AdsError.CODE_BLOCKED, "vị trí native full đang bị tắt"))
            return
        }
        AdsNative.preload(activity, NATIVE_SLOT, object : AdsNativeCallback() {
            override fun onLoaded() {
                nativeReady = true
                finishLoad(null)
            }

            override fun onLoadFailed(error: AdsError) {
                nativeReady = false
                finishLoad(error)
            }
        })
    }

    /** Native full đã tải xong và còn dùng được chưa (đã show một lần thì thành `false`). */
    @JvmStatic
    fun isReady(): Boolean = nativeReady

    /**
     * Hiện interstitial rồi tới dialog native full; [action] chạy SAU KHI user đóng quảng cáo —
     * hoặc chạy ngay nếu không có gì để hiện.
     *
     * Gọi được từ cả Kotlin lẫn Java. Từ Java, `() -> Unit` hiện ra thành `Function0<Unit>` nên
     * lambda phải trả `Unit.INSTANCE` (giống các API khác trong package này).
     */
    @JvmStatic
    fun show(activity: Activity, action: () -> Unit) {
        // Chốt chạy đúng một lần: inter đóng, X, back, lỗi đổ ad — mọi nhánh đều nhả qua đây.
        var fired = false
        val finish: () -> Unit = {
            if (!fired) {
                fired = true
                action()
            }
        }

        if (activity.isFinishing) {
            Log.w(TAG, "show: Activity đang finish — chạy hành động luôn")
            finish()
            return
        }

        val interUsable = RemoteConfigs.isAdsEnabled(activity, RemoteConfigs.INTER_INAPP) &&
            AdsInterstitial.isReady(INTER_SLOT)
        if (interUsable) {
            // Interstitial tự chạy action khi trong tay không còn ad nào, nên nhánh "đóng inter"
            // luôn tới được showNativeOrFinish.
            AdsInterstitial.show(activity, INTER_SLOT) {
                showNativeOrFinish(activity, finish)
            }
        } else {
            showNativeOrFinish(activity, finish)
        }
    }

    /** Dọn trạng thái và đóng dialog đang mở (nếu có). Gọi khi màn bị huỷ. */
    @JvmStatic
    fun clear() {
        nativeReady = false
        pendingCallback = null
        val dialog = currentDialog
        currentDialog = null
        if (dialog?.isShowing == true) {
            dialog.dismiss()
        }
    }

    // ── Nội bộ ──────────────────────────────────────────────────────────────────────────

    private fun finishLoad(error: AdsError?) {
        val callback = pendingCallback ?: return
        pendingCallback = null
        if (error == null) {
            callback.onLoaded()
        } else {
            callback.onLoadFailed(error)
        }
    }

    /**
     * Mở dialog native full, hoặc chạy [finish] ngay nếu không có native để hiện.
     *
     * Không mở dialog rỗng: người dùng bấm một nút mà màn hình trắng trơn chặn lại thì tệ hơn
     * là không có quảng cáo.
     */
    private fun showNativeOrFinish(activity: Activity, finish: () -> Unit) {
        if (!nativeReady || !RemoteConfigs.isAdsEnabled(activity, RemoteConfigs.NATIVE_FULL)) {
            finish()
            return
        }

        // Theme AdsFullScreenDialog có windowIsFloating=false -> cửa sổ chiếm TRỌN màn hình.
        // Dialog tạo bằng theme mặc định là cửa sổ NỔI (có margin viền, chừa status bar) nên
        // không bao giờ full màn — đây là lý do phải truyền theme riêng.
        val dialog = Dialog(activity, R.style.AdsFullScreenDialog)
        try {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.layout_inter_native_full)
            // Ẩn status bar cho đúng nghĩa toàn màn hình.
            dialog.window?.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "không dựng được dialog native full", t)
            finish()
            return
        }

        // Bấm X chỉ gọi dismiss(); hành động chạy ở onDismiss nên chỉ có MỘT đường nhả, tránh
        // chạy hai lần. Bấm back cũng đi qua onDismiss vì dialog để cancelable mặc định —
        // người dùng không bao giờ bị kẹt trong dialog.
        dialog.findViewById<ImageView>(R.id.inter_native_full_close)?.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setOnDismissListener {
            currentDialog = null
            finish()
        }

        val container = dialog.findViewById<FrameLayout>(R.id.inter_native_full_ad_container)
        if (container == null) {
            Log.e(TAG, "layout thiếu inter_native_full_ad_container")
            finish()
            return
        }

        // Native ad dùng một lần: hạ cờ NGAY, lần show sau phải load lại.
        nativeReady = false

        currentDialog = dialog
        dialog.show()

        // Đặt kích thước SAU show(): window chỉ ổn định sau khi attach, đặt trước dễ bị theme
        // ghi đè lại — đây chính là chỗ dialog bị hụt, không phủ hết màn.
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )

        // layout_native_full là layout full do FSDAds đổ nội dung vào (cùng bộ id mà
        // layout_native_onb dùng: uniform/ad_headline/ad_body/ad_media/ad_app_icon/tvActionBtnTitle).
        // Lưu ý: khi Activity không phải FragmentActivity, FSDAds bỏ qua tham số layout này và
        // dùng layout mặc định của nó — xem AdsNative.show(container, slot, layoutResId, cb).
        AdsNative.show(
            container,
            NATIVE_SLOT,
            R.layout.layout_native_full,
            object : AdsNativeCallback() {
                override fun onLoadFailed(error: AdsError) {
                    Log.e(TAG, "không đổ được native vào dialog: ${error.message}")
                    // Không có ad để xem -> đóng dialog để hành động của người dùng chạy tiếp.
                    dialog.dismiss()
                }
            },
        )
    }
}
