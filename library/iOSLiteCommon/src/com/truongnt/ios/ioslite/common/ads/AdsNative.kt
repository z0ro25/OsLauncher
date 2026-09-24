package com.truongnt.ios.ioslite.common.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.truongnt.fsd.nttads.FsdAds
import com.truongnt.fsd.nttads.NativeAdsCallback
import com.truongnt.ios.ioslite.common.R

/**
 * Toàn bộ native ad.
 *
 * Khác biệt căn bản với hạ tầng cũ: FSDAds **không trả về một `View`** cho ta tự gắn.
 * Nó preload theo một khoá gọi là *alias* ([AdsSlot.alias]), rồi tự đổ nội dung vào một
 * `FrameLayout` do ta đưa. Vì vậy API ở đây là `container`, không phải `getAdContentView()`.
 *
 * ### Vì sao có hai đường gọi FSDAds
 *
 * FSDAds có hai nhóm API:
 *  - `showPreloadNative(...)` — chỉ nhận `FragmentActivity`.
 *  - `reloadNative(...)`      — nhận `Activity` thường.
 *
 * Mà `Launcher` của dự án kế thừa `android.app.Activity` trần (qua `CommonActivity`),
 * **không phải** `FragmentActivity`, trong khi các màn ThemeClub lại là `AppCompatActivity`.
 * [show] tự dò Activity từ `container` rồi chọn đường phù hợp, nên call-site ở cả hai nơi
 * dùng chung một API.
 */
object AdsNative {

    private const val TAG = "AdsNative"

    /**
     * Giữ skeleton shimmer thêm chừng này kể từ lúc ad SẴN SÀNG rồi mới thay bằng ad.
     *
     * Vì sao cần: ad (nhất là ad test) thường về rất nhanh, skeleton chỉ thoáng qua nên nhìn như
     * không có. Giữ đủ [SHIMMER_HOLD_MS] thì mắt mới thấy được hiệu ứng chờ.
     */
    private const val SHIMMER_HOLD_MS = 1000L

    /**
     * Tải trước native ad cho [slot].
     *
     * Nội dung được FSDAds giữ theo [AdsSlot.alias]; [show] sau đó lấy ra đổ vào container.
     * Gọi càng sớm càng tốt (lúc dựng màn hình) để lúc cần là có sẵn.
     */
    /** Các slot đã bắt đầu tải trước — tránh phát lại request mỗi lần màn hình hiện lại. */
    private val preloadStarted = java.util.Collections.synchronizedSet(HashSet<AdsSlot>())

    /**
     * Native ad đã tải xong, chờ đổ vào view — khoá theo [AdsSlot].
     *
     * Vì sao phải tự giữ: `FsdAds.preLoadNative` trả `NativeAd` qua callback của nó, nhưng đường
     * hiển thị sẵn có của SDK (`FsdAds.showPreloadNative`) chỉ gọi được từ `FragmentActivity`.
     * Desktop launcher (`Launcher`, `Activity` trần) muốn dùng layout riêng thì phải tự inflate
     * rồi tự populate — xem [pourOwnLayout].
     *
     * Ad dùng MỘT lần: [pourOwnLayout] lấy ra là xoá luôn, muốn hiện lại phải [preload] lại.
     */
    private val preloadedNativeAds =
        java.util.concurrent.ConcurrentHashMap<AdsSlot, NativeAd>()

    @JvmStatic
    fun preload(activity: Activity, slot: AdsSlot, callback: AdsNativeCallback?) {
        if (!Ads.canUse(slot)) {
            Log.e(TAG, "preload: bị chặn" )
            callback?.onLoadFailed(AdsError(AdsError.CODE_BLOCKED, "slot bị chặn: ${slot.alias}"))
            return
        }
        try {
            FsdAds.preLoadNative(activity, slot.alias, AdsIds.idFor(slot)) { ad ->
                if (ad == null) {
                    Log.e(TAG, "preload: false" )
                    preloadedNativeAds.remove(slot)
                    callback?.onLoadFailed(
                        AdsError(AdsError.CODE_NOT_READY, "preload không trả về ad nào"),
                    )
                } else {
                    Log.e(TAG, "preload: success" )
                    // Giữ lại ad để đường tự đổ (Activity trần) dùng — xem pourOwnLayout.
                    preloadedNativeAds[slot] = ad
                    callback?.onLoaded()
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "preload lỗi slot=${slot.alias}", t)
            callback?.onLoadFailed(AdsError(AdsError.CODE_NOT_READY, t.message))
        }
    }

    /**
     * Đổ native ad của [slot] vào [container], dùng layout mặc định của FSDAds.
     *
     * Không tự gọi [preload] — phải preload trước, nếu không FSDAds không có gì để đổ và
     * sẽ báo lỗi qua [AdsNativeCallback.onLoadFailed].
     */
    @JvmStatic
    fun show(container: FrameLayout, slot: AdsSlot, callback: AdsNativeCallback?) {
        val activity = beginShow(slot, callback, container.context) ?: return
        // Khai báo kiểu tường minh: nếu để Kotlin tự suy luận thì `callback?.onLoaded()` trả
        // về Unit? và lambda thành `() -> Unit?` — không khớp tham số `() -> Unit` của FSDAds.
        val onDone: (View) -> Unit = { _ -> callback?.onLoaded() }
        val onFailed: () -> Unit = {
            callback?.onLoadFailed(
                AdsError(AdsError.CODE_NOT_READY, "FSDAds không đổ được native vào container"),
            )
        }
        try {
            if (activity is FragmentActivity) {
                // Bỏ trống layoutResId -> FSDAds dùng layout mặc định của nó.
                FsdAds.showPreloadNative(activity, slot.alias, container, onDone, onFailed)
            } else {
                reloadInto(activity, container, slot, callback)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "show lỗi slot=${slot.alias}", t)
            callback?.onLoadFailed(AdsError(AdsError.CODE_SHOW_FAILED, t.message))
        }
    }

    /**
     * Như [show] nhưng chỉ định layout để inflate.
     *
     * Hai đường, tuỳ host:
     *  - Host là `FragmentActivity` (mọi màn của :app — `BaseActivity` là AppCompatActivity):
     *    đưa thẳng cho SDK, `FsdAds.showPreloadNative` tự inflate [layoutResId].
     *  - Host là `Activity` trần (desktop launcher: màn trái, App Library, Search trong Launcher):
     *    SDK KHÔNG nhận layout ở đường nào cả — `showPreloadNative` đòi `FragmentActivity`, còn
     *    `reloadNative` không có tham số layout (nó đọc layout đã ghi cứng trong `nativeConfigs`
     *    của SDK). Nên phải tự inflate + tự populate, xem [pourOwnLayout].
     *
     * [layoutResId] phải theo đúng bộ id của SDK: root `NativeAdView` id `uniform`, cùng
     * `ad_media` (MediaView), `ad_headline`, `ad_body`, `tvActionBtnTitle`, `ad_app_icon`.
     */
    @JvmStatic
    fun show(container: FrameLayout, slot: AdsSlot, layoutResId: Int, callback: AdsNativeCallback?) {
        val activity = beginShow(slot, callback, container.context) ?: return
        // Khai báo kiểu tường minh: nếu để Kotlin tự suy luận thì `callback?.onLoaded()` trả
        // về Unit? và lambda thành `() -> Unit?` — không khớp tham số `() -> Unit` của FSDAds.
        val onDone: (View) -> Unit = { _ -> callback?.onLoaded() }
        val onFailed: () -> Unit = {
            Log.e(TAG, "show: ", )
            callback?.onLoadFailed(
                AdsError(AdsError.CODE_NOT_READY, "FSDAds không đổ được native vào container"),
            )
        }
        try {
            if (activity is FragmentActivity) {
                FsdAds.showPreloadNative(activity, slot.alias, container, onDone, onFailed, layoutResId)
            } else {
                // Activity trần -> SDK không nhận layout, tự inflate rồi tự populate.
                pourOwnLayout(activity, container, slot, layoutResId, callback)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "show lỗi slot=${slot.alias}", t)
            callback?.onLoadFailed(AdsError(AdsError.CODE_SHOW_FAILED, t.message))
        }
    }

    /** Yêu cầu FSDAds nạp lại native vào [container] (đường đi dùng được với `Activity` thường). */
    @JvmStatic
    fun reload(activity: Activity, container: FrameLayout, slot: AdsSlot, callback: AdsNativeCallback?) {
        if (!Ads.canUse(slot)) {
            callback?.onLoadFailed(AdsError(AdsError.CODE_BLOCKED, "slot bị chặn: ${slot.alias}"))
            return
        }
        reloadInto(activity, container, slot, callback)
    }

    private fun reloadInto(
        activity: Activity,
        container: FrameLayout,
        slot: AdsSlot,
        callback: AdsNativeCallback?,
    ) {
        try {
            FsdAds.reloadNative(
                activity,
                slot.alias,
                container,
                object : NativeAdsCallback {
                    override fun onLoaded() {
                        callback?.onLoaded()
                    }

                    override fun onError() {
                        callback?.onLoadFailed(
                            AdsError(AdsError.CODE_NOT_READY, "reloadNative báo lỗi"),
                        )
                    }
                },
            )
        } catch (t: Throwable) {
            Log.e(TAG, "reload lỗi slot=${slot.alias}", t)
            callback?.onLoadFailed(AdsError(AdsError.CODE_SHOW_FAILED, t.message))
        }
    }

    /**
     * Đổ native vào [container] bằng CHÍNH [layoutResId] — đường dành cho Activity không phải
     * `FragmentActivity` (desktop launcher), nơi SDK không nhận layout.
     *
     * <p>Thứ tự: lấy ad đã tải sẵn trong [preloadedNativeAds] (do [preload] giữ lại). Nếu chưa có
     * — chuyện thường gặp ở lần mở màn đầu tiên, vì preload vừa mới được phát và ad chưa về —
     * thì TỰ phát request rồi đổ khi ad về. Không có bước này thì ô quảng cáo sẽ trống ở lần mở
     * đầu (đường cũ dùng `reloadNative` của SDK cũng load tại chỗ, nên bỏ đi là mất ad).
     *
     * <p>Ad dùng MỘT lần nên lấy ra là xoá khỏi map.
     */
    private fun pourOwnLayout(
        activity: Activity,
        container: FrameLayout,
        slot: AdsSlot,
        layoutResId: Int,
        callback: AdsNativeCallback?,
    ) {
        // Đổ skeleton GIỮ CHỖ trước tiên — cho MỌI nhánh, kể cả nhánh ad đã có sẵn trong tay
        // (nhánh đó cũng phải giữ skeleton đủ SHIMMER_HOLD_MS, xem holdShimmerThenPour).
        // Ad đổ vào sau sẽ thay skeleton: pourNativeAdView gọi removeAllViews trước khi addView.
        pourShimmer(container, layoutResId)

        val ready = preloadedNativeAds.remove(slot)
        if (ready != null) {
            holdShimmerThenPour(container, ready, layoutResId, callback)
            return
        }

        Log.d(TAG, "pourOwnLayout: chưa có ad tải sẵn — phát request cho slot=${slot.alias}")
        try {
            FsdAds.preLoadNative(activity, slot.alias, AdsIds.idFor(slot)) { ad ->
                if (ad == null) {
                    // Không có ad -> dọn skeleton để không còn khối xám nằm trơ.
                    container.removeAllViews()
                    callback?.onLoadFailed(
                        AdsError(AdsError.CODE_NOT_READY, "load native thất bại: ${slot.alias}"),
                    )
                } else {
                    holdShimmerThenPour(container, ad, layoutResId, callback)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "pourOwnLayout: phát request lỗi slot=${slot.alias}", t)
            container.removeAllViews()
            callback?.onLoadFailed(AdsError(AdsError.CODE_NOT_READY, t.message))
        }
    }

    /**
     * Giữ skeleton thêm [SHIMMER_HOLD_MS] kể từ lúc ad ĐÃ SẴN SÀNG rồi mới thay bằng ad.
     *
     * <p>Áp cho CẢ nhánh ad có sẵn trong tay: nếu hiện ngay thì user không thấy skeleton chút nào
     * (ad test về nhanh, có khi tức thì) — trong khi yêu cầu là "cộng thêm 1s sau khi ad xong, thời
     * gian skeleton ít nhất 1s trong mọi trường hợp".
     *
     * <p>Skeleton đã được [pourShimmer] đổ trước đó nên trong lúc chờ luôn có khối xám, không có
     * quãng trống.
     */
    private fun holdShimmerThenPour(
        container: FrameLayout,
        nativeAd: NativeAd,
        layoutResId: Int,
        callback: AdsNativeCallback?,
    ) {
        container.postDelayed(
            Runnable { pourNativeAdView(nativeAd, container, layoutResId, callback) },
            SHIMMER_HOLD_MS,
        )
    }

    /**
     * Đổ skeleton shimmer giữ chỗ vào [container] trong lúc chờ ad.
     *
     * <p>Bắt chước việc SDK làm trong `initNativeAds` (`if (frameLayout.childCount == 0)
     * showNativeLoading(...)`) — đường tự populate phải tự làm, nếu không ô quảng cáo trống trơn
     * trong lúc chờ rồi ad hiện ra đột ngột.
     *
     * <p><b>Chiều cao — chỗ dễ sai nhất:</b>
     * <ul>
     *   <li>Container đã có chiều cao cố định (App Library: nằm trong box tỉ lệ 2:1) -> skeleton
     *       để `match_parent` cho lấp đầy ô, giống hệt cách ad thật lấp đầy ô.</li>
     *   <li>Container để `wrap_content` (màn trái, search) -> phải đặt chiều cao CỤ THỂ, lấy bằng
     *       chiều cao thật của [layoutResId] đo được ([measureAdLayoutHeight]). Để `match_parent`
     *       ở đây là sai: FrameLayout wrap_content sẽ đo skeleton theo khoảng trống còn lại và ô
     *       quảng cáo phình ra.</li>
     * </ul>
     *
     * <p>Không đổ được skeleton cũng không sao — chỉ mất hiệu ứng chờ, ad vẫn hiện bình thường.
     */
    private fun pourShimmer(container: FrameLayout, layoutResId: Int) {
        try {
            val view = LayoutInflater.from(container.context)
                .inflate(R.layout.ads_native_shimmer, container, false)
            val containerParams = container.layoutParams
            val height = if (containerParams != null &&
                containerParams.height == ViewGroup.LayoutParams.WRAP_CONTENT
            ) {
                // wrap_content -> phải tự cho chiều cao, không thì skeleton cao 0 hoặc phình ra.
                val measured = measureAdLayoutHeight(container, layoutResId)
                if (measured > 0) measured else ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }
            view.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height,
            )
            container.removeAllViews()
            container.addView(view)
            // Không dùng attr auto-start trong XML (phụ thuộc phiên bản shimmer) — tự chạy.
            (view as? ShimmerFrameLayout)?.startShimmer()
        } catch (t: Throwable) {
            Log.w(TAG, "pourShimmer lỗi (layout=$layoutResId)", t)
        }
    }

    /**
     * Chiều cao mà [adLayoutResId] sẽ chiếm khi được đổ vào [container] — đo bằng cách inflate
     * thử rồi measure, KHÔNG hardcode số dp nào (layout ad đổi kích thước thì theo luôn).
     *
     * <p>Trả 0 khi không đo được (lúc đó skeleton để `wrap_content`, không phá layout).
     */
    private fun measureAdLayoutHeight(container: FrameLayout, adLayoutResId: Int): Int {
        return try {
            val probe = LayoutInflater.from(container.context)
                .inflate(adLayoutResId, container, false)
            val parent = container.parent as? View
            val width = when {
                container.width > 0 -> container.width
                parent != null && parent.width > 0 -> parent.width
                else -> container.resources.displayMetrics.widthPixels
            }
            probe.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            )
            probe.measuredHeight
        } catch (t: Throwable) {
            Log.w(TAG, "đo layout ad lỗi (layout=$adLayoutResId)", t)
            0
        }
    }

    /**
     * Inflate [layoutResId], gắn [nativeAd] vào, rồi thay nội dung [container] bằng view đó.
     *
     * <p>Luồng giống hệt SDK: `container.removeAllViews()` rồi `addView` — nhờ vậy container luôn
     * sạch, không chồng nhiều lớp ad khi bind lại.
     */
    private fun pourNativeAdView(
        nativeAd: NativeAd,
        container: FrameLayout,
        layoutResId: Int,
        callback: AdsNativeCallback?,
    ) {
        try {
            val cardView =
                LayoutInflater.from(container.context).inflate(layoutResId, null) as FrameLayout
            val adView = cardView.findViewById<NativeAdView>(R.id.uniform)
            if (adView == null) {
                Log.e(TAG, "pourNativeAdView: layout thiếu NativeAdView id=uniform")
                callback?.onLoadFailed(
                    AdsError(AdsError.CODE_NOT_READY, "layout thiếu NativeAdView id=uniform"),
                )
                return
            }
            populateOwnView(nativeAd, adView)
            container.removeAllViews()
            container.addView(cardView)
            callback?.onLoaded()
        } catch (t: Throwable) {
            Log.e(TAG, "pourNativeAdView lỗi", t)
            callback?.onLoadFailed(AdsError(AdsError.CODE_SHOW_FAILED, t.message))
        }
    }

    /**
     * Gắn dữ liệu [nativeAd] vào [adView] — **copy 1:1** `NativeAds.populateNativeAdView` của
     * FSDAds (bản 1.1.36, hàm `private` nên không gọi lại được từ ngoài SDK).
     *
     * <p>Bất biến: id phải khớp bộ id SDK dùng, nếu không `findViewById` trả null và ad sẽ hiện
     * khung rỗng. Khi SDK nâng cấp mà đổi cách populate thì phải cập nhật hàm này theo.
     */
    private fun populateOwnView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        adView.mediaView?.mediaContent = nativeAd.mediaContent
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.tvActionBtnTitle)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)

        (adView.headlineView as? TextView)?.text = nativeAd.headline

        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as? TextView)?.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as? Button)?.text = nativeAd.callToAction
        }

        nativeAd.icon?.let {
            (adView.iconView as? ImageView)?.setImageDrawable(it.drawable)
            adView.iconView?.visibility = View.VISIBLE
        } ?: run {
            adView.iconView?.visibility = View.GONE
        }

        adView.setNativeAd(nativeAd)

        nativeAd.mediaContent?.videoController?.let { vc ->
            if (vc.hasVideoContent()) {
                vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {}
            }
        }
    }

    /** Kiểm tra điều kiện chung rồi dò Activity. Trả null nghĩa là đã báo lỗi và nên dừng. */
    private fun beginShow(slot: AdsSlot, callback: AdsNativeCallback?, context: Context?): Activity? {
        if (!Ads.canUse(slot)) {
            callback?.onLoadFailed(AdsError(AdsError.CODE_BLOCKED, "slot bị chặn: ${slot.alias}"))
            return null
        }
        val activity = Ads.findActivity(context)
        if (activity == null) {
            callback?.onLoadFailed(
                AdsError(AdsError.CODE_NO_ACTIVITY, "không dò được Activity từ container"),
            )
            return null
        }
        return activity
    }
}
