package com.ezla.oslauncher.controlcenter.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.PointF
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.controlcenter.CcPrefs
import com.ezla.oslauncher.controlcenter.ControlCenterEntry
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.data.CcEdge
import com.ezla.oslauncher.controlcenter.databinding.CcOverlayNotificationsBinding
import com.ezla.oslauncher.controlcenter.databinding.CcOverlayPanelBinding
import com.ezla.oslauncher.controlcenter.skin.CcAccent
import com.ezla.oslauncher.controlcenter.skin.CcSkinSource
import com.ezla.oslauncher.controlcenter.panel.ControlPanelPages
import com.ezla.oslauncher.controlcenter.service.CcNotificationListener
import com.ezla.oslauncher.controlcenter.service.CcNotificationStore
import com.ezla.oslauncher.controlcenter.system.CcActions
import com.ezla.oslauncher.controlcenter.system.CcHaptics
import com.ezla.oslauncher.controlcenter.system.CcPermissions
import com.ezla.oslauncher.controlcenter.system.CcWallpaper
import com.ezla.oslauncher.controlcenter.system.FlashController
import kotlin.math.abs

// Quản lý mọi cửa sổ overlay: 2 thanh kéo (Control Center theo Position, Thông báo ở góc trái trên)
// và 2 panel toàn màn. Bất biến: mỗi cửa sổ chỉ add 1 lần, detach() gỡ sạch (lỗi chồng thanh của Theme4k).
class OverlayController(private val context: Context) {

    // Phát lại chạm không phải vuốt mở xuống app bên dưới (cần context là AccessibilityService).
    private val passThrough = (context as? AccessibilityService)?.let { svc ->
        GesturePassThrough(svc) { touchable -> setHandlesTouchable(touchable) }
    }
    // false trong lúc đang phát lại cử chỉ: thanh kéo phải để chạm xuyên qua.
    private var handlesTouchable = true

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    // View overlay dựng bằng context có theme Material (context service không có theme app).
    private val themed = ContextThemeWrapper(context, R.style.CcTheme)
    private val inflater = LayoutInflater.from(themed)
    private val density = context.resources.displayMetrics.density

    private var ccHandle: EdgeHandle? = null
    private var ncHandle: EdgeHandle? = null

    private var panelRoot: OverlayRoot? = null
    private var panelBinding: CcOverlayPanelBinding? = null
    private var pages: ControlPanelPages? = null
    private var panelOpen = false
    private var openedFrom = CcEdge.TOP

    private var ncRoot: OverlayRoot? = null
    private var ncBinding: CcOverlayNotificationsBinding? = null
    private var ncAdapter: NotificationAdapter? = null
    private var ncFlash: FlashController? = null
    private var ncOpen = false

    private val notificationsChanged: () -> Unit = { if (ncOpen) renderNotifications() }

    fun attach() {
        if (ccHandle != null) return
        // Không add trước ở 0x0: applyHandles() add 1 lần với đúng kích thước.
        ccHandle = EdgeHandle(themed) { openPanel() }.apply { onPassThrough = ::replay }
        ncHandle = EdgeHandle(themed) { openNotifications() }.apply { onPassThrough = ::replay }
        applyHandles()
        CcNotificationStore.addListener(notificationsChanged)
    }

    fun detach() {
        CcNotificationStore.removeListener(notificationsChanged)
        passThrough?.release()
        closeAll(animate = false)
        listOf<View?>(ccHandle, ncHandle, panelRoot, ncRoot).forEach { v ->
            if (v != null && v.isAdded) runCatching { wm.removeViewImmediate(v) }
        }
        pages?.stop()
        ncFlash?.stop()
        ccHandle = null
        ncHandle = null
        panelRoot = null
        panelBinding = null
        pages = null
        ncRoot = null
        ncBinding = null
        ncFlash = null
    }

    fun onSettingsChanged() {
        applyHandles()
        pages?.reloadTiles()
        pages?.applySkin(CcSkinSource.installed(context))
        pages?.applyAccent(CcAccent.installed(context))
        if (panelOpen) applyPanelBackground()
    }

    fun onConfigurationChanged() {
        // Panel thiết kế cho màn dọc: xoay ngang thì đóng và ẩn thanh kéo.
        if (isLandscape()) closeAll(animate = false)
        applyHandles()
    }

    fun closeAll(animate: Boolean = true) {
        closePanel(animate)
        closeNotifications(animate)
    }

    // ===== Thanh kéo =====

    private fun applyHandles() {
        val cc = ccHandle ?: return
        val nc = ncHandle ?: return
        val s = CcPrefs.edgeSettings(context)
        val hidden = isLandscape() || panelOpen || ncOpen
        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels

        // Thanh trượt 0..100 -> dày 2..12dp, dài 20..160dp, mờ 5..50% (mặc định 90dp / 27%:
        // bản trước dài và đậm gấp đôi, bị chê vướng), vị trí dọc theo mép.
        val thickness = dp(2 + s.width / 10f)
        val length = dp(20 + s.length * 1.4f)
        val alpha = if (s.hide) 0f else 0.05f + s.opacity * 0.0045f
        val touch = maxOf(thickness, dp(22f))
        val lp = cc.layoutParams as? WindowManager.LayoutParams ?: EdgeHandle.params()
        val along = s.offset / 100f
        // Như iOS 26: dải trên cùng chia đôi tại camera, tai phải = Control Center, tai trái = Thông báo.
        // Dải chạm nằm NGAY DƯỚI status bar: đặt đè lên status bar thì hệ thống bắt cử chỉ kéo
        // thanh thông báo Android trước, overlay không nhận được vuốt.
        val sbH = statusBarHeight()
        val bandH = maxOf(sbH, touch)
        val split = cameraCenterX(screenW)
        if (s.edge == CcEdge.TOP) {
            // Vùng chạm cả tai phải; thanh vạch chỉ để gợi ý, vẽ theo Edge Settings trong vùng đó.
            lp.width = screenW - split
            lp.height = bandH
            lp.x = split
            lp.y = sbH
            // Thanh lệch ra mép ngoài tai như iOS: Position Vertical 0 = giữa tai, 100 = sát mép,
            // mặc định 50 = 3/4 về phía mép ngoài.
            val earW = screenW - split
            val barLen = minOf(length, earW)
            cc.style(s.edge, thickness, barLen, s.color, alpha, ((earW - barLen) * earFrac(along)).toInt())
        } else {
            cc.style(s.edge, thickness, length, s.color, alpha)
            if (s.edge.isVertical) {
                lp.width = touch
                lp.height = length
                lp.x = if (s.edge == CcEdge.LEFT) 0 else screenW - touch
                lp.y = ((screenH - length) * along).toInt()
            } else {
                lp.width = length
                lp.height = touch
                lp.x = ((screenW - length) * along).toInt()
                lp.y = screenH - touch
            }
        }
        updateHandle(cc, lp, hidden)

        // Trung tâm thông báo: luôn là tai trái (mép trái -> camera), vạch cùng kiểu thanh CC và đặt
        // đối xứng qua camera. Tai phải khi CC không ở Top được trả lại cho hệ thống.
        val ncBarLen = minOf(length, split)
        nc.style(CcEdge.TOP, thickness, ncBarLen, s.color, alpha, ((split - ncBarLen) * (1 - earFrac(along))).toInt())
        val nlp = nc.layoutParams as? WindowManager.LayoutParams ?: EdgeHandle.params()
        nlp.width = split.coerceAtLeast(1)
        nlp.height = bandH
        nlp.x = 0
        nlp.y = sbH
        updateHandle(nc, nlp, hidden)

    }

    // Ẩn = thêm NOT_TOUCHABLE: cửa sổ GONE vẫn chặn chạm trong vùng của nó nếu còn touchable.
    private fun updateHandle(h: View, lp: WindowManager.LayoutParams, hidden: Boolean) {
        h.visibility = if (hidden) View.GONE else View.VISIBLE
        lp.flags = if (hidden || !handlesTouchable) lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        else lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        val r = if (h.isAdded) runCatching { wm.updateViewLayout(h, lp) }
        else runCatching { wm.addView(h, lp) }
        r.onFailure { Log.e(EdgeHandle.TAG, "update/add handle lỗi", it) }
    }

    private fun replay(points: List<PointF>, durationMs: Long) {
        passThrough?.replay(points, durationMs)
    }

    private fun setHandlesTouchable(touchable: Boolean) {
        if (handlesTouchable == touchable) return
        handlesTouchable = touchable
        applyHandles()
    }

    // ===== Control Center =====

    private fun ensurePanel(): OverlayRoot {
        panelRoot?.let { return it }
        val root = OverlayRoot(themed, onBack = { closePanel() }, touchSpy = ::onPanelTouch)
        val b = CcOverlayPanelBinding.inflate(inflater, root, true)
        panelBinding = b
        pages = ControlPanelPages(themed, b.ccPager, editMode = false, host = object : ControlPanelPages.Host {
            override fun closePanel() = this@OverlayController.closePanel()
            override fun onPageChanged(page: Int) = renderDots(page)
        })
        b.ccBtnSettings.setOnClickListener {
            CcHaptics.tap(it)
            closePanel()
            ControlCenterEntry.openFromService(context)
        }
        listOf(b.ccDot0, b.ccDot1, b.ccDot2).forEachIndexed { i, dot ->
            dot.setOnClickListener { pages?.showPage(i) }
        }
        renderDots(0)
        panelRoot = root
        return root
    }

    private fun openPanel() {
        if (panelOpen || isLandscape()) return
        closeNotifications(false)
        // Lỗi dựng panel chỉ ghi log, không để crash service (crash = hệ thống tắt Accessibility).
        val root = runCatching { ensurePanel() }
            .onFailure { Log.e(EdgeHandle.TAG, "ensurePanel lỗi", it) }.getOrNull() ?: return
        val s = CcPrefs.edgeSettings(context)
        openedFrom = s.edge
        if (!root.isAdded) runCatching { wm.addView(root, panelParams()) }
            .onFailure { Log.e(EdgeHandle.TAG, "addView panel lỗi", it) }
        root.visibility = View.VISIBLE
        panelOpen = true
        applyHandles()
        applyPanelBackground()
        pages?.reloadTiles()
        pages?.applySkin(CcSkinSource.installed(context))
        pages?.applyAccent(CcAccent.installed(context))
        pages?.showPage(ControlPanelPages.PAGE_MAIN, false)
        pages?.start()
        CcHaptics.tap(root)
        animateIn(root, openedFrom)
    }

    private fun closePanel(animate: Boolean = true) {
        val root = panelRoot ?: return
        if (!panelOpen) return
        panelOpen = false
        pages?.stop()
        // Gỡ hẳn cửa sổ: để lại cửa sổ toàn màn (dù GONE) sẽ nuốt mọi chạm + focus của app bên dưới.
        val done = {
            if (root.isAdded) runCatching { wm.removeViewImmediate(root) }
            applyHandles()
        }
        if (animate) animateOut(root, openedFrom, done) else done()
    }

    private fun applyPanelBackground() {
        val b = panelBinding ?: return
        val blur = CcPrefs.isBlur(context)
        b.ccScrim.setBackgroundColor(ContextCompat.getColor(context,
            if (blur) R.color.cc_panel_scrim else R.color.cc_panel_scrim_no_blur))
        CcWallpaper.load(context, blur) { bmp -> if (bmp != null) b.ccBg.setImageBitmap(bmp) }
    }

    private fun renderDots(page: Int) {
        val b = panelBinding ?: return
        listOf(b.ccDot0, b.ccDot1, b.ccDot2).forEachIndexed { i, dot ->
            dot.setColorFilter(if (i == page) 0xFFFFFFFF.toInt() else
                ContextCompat.getColor(context, R.color.cc_page_dot_off))
        }
    }

    // Vuốt theo hướng ngược lúc mở để đóng; mép trên/dưới chỉ đóng khi pager đã hết trang theo hướng đó.
    private val panelGesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (e1 == null) return false
            val dx = e2.rawX - e1.rawX
            val dy = e2.rawY - e1.rawY
            val pager = panelBinding?.ccPager ?: return false
            val close = when (openedFrom) {
                CcEdge.TOP -> dy < -dp(80f) && abs(dy) > abs(dx) && !pager.canScrollVertically(1)
                CcEdge.BOTTOM -> dy > dp(80f) && abs(dy) > abs(dx) && !pager.canScrollVertically(-1)
                CcEdge.LEFT -> dx < -dp(80f) && abs(dx) > abs(dy)
                CcEdge.RIGHT -> dx > dp(80f) && abs(dx) > abs(dy)
            }
            if (close) closePanel()
            return close
        }
    })

    private fun onPanelTouch(e: MotionEvent) {
        if (panelOpen) panelGesture.onTouchEvent(e)
    }

    // ===== Trung tâm thông báo =====

    private fun ensureNotifications(): OverlayRoot {
        ncRoot?.let { return it }
        val root = OverlayRoot(themed, onBack = { closeNotifications() }, touchSpy = ::onNcTouch)
        val b = CcOverlayNotificationsBinding.inflate(inflater, root, true)
        ncBinding = b
        val adapter = NotificationAdapter(themed,
            onOpen = ::openNotification,
            onDismiss = { list -> list.forEach { CcNotificationListener.dismiss(it.key) } })
        ncAdapter = adapter
        b.ncList.layoutManager = LinearLayoutManager(context)
        b.ncList.adapter = adapter
        adapter.attachSwipe(b.ncList)
        b.ncClose.setOnClickListener { closeNotifications() }
        b.ncContent.setOnClickListener { closeNotifications() }
        ncFlash = FlashController(context) { on -> renderNcFlash(on) }
        b.ncFlash.setOnClickListener { CcHaptics.tap(it); ncFlash?.toggle() }
        b.ncCamera.setOnClickListener {
            CcHaptics.tap(it)
            closeNotifications()
            CcActions.camera(context)
        }
        ncRoot = root
        return root
    }

    private fun openNotifications() {
        if (ncOpen || isLandscape()) return
        closePanel(false)
        val root = ensureNotifications()
        if (!root.isAdded) runCatching { wm.addView(root, panelParams()) }
        root.visibility = View.VISIBLE
        ncOpen = true
        applyHandles()
        val b = ncBinding ?: return
        CcWallpaper.load(context, true) { bmp -> if (bmp != null) b.ncBg.setImageBitmap(bmp) }
        ncFlash?.start()
        renderNcFlash(ncFlash?.isOn == true)
        renderNotifications()
        CcHaptics.tap(root)
        animateIn(root, CcEdge.TOP)
    }

    private fun closeNotifications(animate: Boolean = true) {
        val root = ncRoot ?: return
        if (!ncOpen) return
        ncOpen = false
        ncFlash?.stop()
        val done = {
            if (root.isAdded) runCatching { wm.removeViewImmediate(root) }
            applyHandles()
        }
        if (animate) animateOut(root, CcEdge.TOP, done) else done()
    }

    private fun renderNotifications() {
        val b = ncBinding ?: return
        val adapter = ncAdapter ?: return
        if (!CcPermissions.isNotificationListenerEnabled(context)) {
            adapter.submit(emptyList())
            b.ncEmpty.setText(R.string.cc_need_notification)
            b.ncEmpty.visibility = View.VISIBLE
            b.ncEmpty.setOnClickListener {
                closeNotifications()
                CcActions.start(context, CcPermissions.notificationListenerSettings())
            }
            return
        }
        adapter.submit(CcNotificationListener.active())
        b.ncEmpty.setText(R.string.cc_no_notifications)
        b.ncEmpty.setOnClickListener(null)
        b.ncEmpty.visibility = if (adapter.isEmpty) View.VISIBLE else View.GONE
    }

    private fun renderNcFlash(on: Boolean) {
        val b = ncBinding ?: return
        b.ncFlash.setBackgroundResource(if (on) R.drawable.cc_bg_tile_on else R.drawable.cc_bg_nc_button)
        b.ncFlash.setColorFilter(if (on) 0xFF000000.toInt() else 0xFFD9D9D9.toInt())
    }

    private fun openNotification(sbn: StatusBarNotification) {
        closeNotifications()
        val pi = sbn.notification.contentIntent
        val ok = pi != null && CcActions.sendPendingIntent(pi)
        if (!ok) {
            context.packageManager.getLaunchIntentForPackage(sbn.packageName)?.let { CcActions.start(context, it) }
        }
        // Thông báo autoCancel: hệ thống không tự xoá khi mở từ ngoài shade nên xoá tay.
        if (sbn.notification.flags and android.app.Notification.FLAG_AUTO_CANCEL != 0) {
            CcNotificationListener.dismiss(sbn.key)
        }
    }

    private val ncGesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (e1 == null) return false
            val dx = e2.rawX - e1.rawX
            val dy = e2.rawY - e1.rawY
            val list = ncBinding?.ncList ?: return false
            // Vuốt lên để đóng khi danh sách đã cuộn tới cuối (không tranh với cuộn list).
            val close = dy < -dp(80f) && abs(dy) > abs(dx) * 1.5f && !list.canScrollVertically(1)
            if (close) closeNotifications()
            return close
        }
    })

    private fun onNcTouch(e: MotionEvent) {
        if (ncOpen) ncGesture.onTouchEvent(e)
    }

    // ===== Chung =====

    private fun panelParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        // Không NOT_FOCUSABLE để nhận phím Back; vẽ tràn cả status bar / nav bar.
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun animateIn(v: View, from: CcEdge) {
        val (tx, ty) = offscreen(v, from)
        v.translationX = tx
        v.translationY = ty
        v.alpha = 0f
        v.animate().translationX(0f).translationY(0f).alpha(1f).setDuration(ANIM_MS).start()
    }

    private fun animateOut(v: View, to: CcEdge, end: () -> Unit) {
        val (tx, ty) = offscreen(v, to)
        v.animate().translationX(tx).translationY(ty).alpha(0f).setDuration(ANIM_MS)
            .withEndAction { end() }.start()
    }

    private fun offscreen(v: View, edge: CcEdge): Pair<Float, Float> {
        val w = (if (v.width > 0) v.width else context.resources.displayMetrics.widthPixels).toFloat()
        val h = (if (v.height > 0) v.height else context.resources.displayMetrics.heightPixels).toFloat()
        // Trượt 1/3 quãng + mờ dần: mượt mà không kéo cả màn đi xa.
        return when (edge) {
            CcEdge.TOP -> 0f to -h / 3
            CcEdge.BOTTOM -> 0f to h / 3
            CcEdge.LEFT -> -w / 3 to 0f
            CcEdge.RIGHT -> w / 3 to 0f
        }
    }

    private fun earFrac(along: Float) = 0.5f + along * 0.5f

    private fun statusBarHeight(): Int {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else dp(24f)
    }

    // Tâm camera (tai thỏ) theo chiều ngang từ DisplayCutout; máy không có cutout thì lấy giữa màn.
    // Bọc runCatching: context service (không phải visual context) có thể ném lỗi khi hỏi window
    // metrics -> service crash và hệ thống tự TẮT Accessibility của app.
    @Suppress("DEPRECATION")
    private fun cameraCenterX(screenW: Int): Int = runCatching {
        val top = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.currentWindowMetrics.windowInsets.displayCutout?.boundingRectTop
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wm.defaultDisplay.cutout?.boundingRectTop
        } else null
        if (top != null && !top.isEmpty) top.centerX() else screenW / 2
    }.onFailure { Log.e(EdgeHandle.TAG, "cameraCenterX lỗi", it) }.getOrDefault(screenW / 2)

    // Đã add vào WindowManager hay chưa. Không dùng isAttachedToWindow: cờ đó chỉ bật ở frame sau,
    // gọi addView lần 2 ngay sau lần 1 sẽ ném "has already been added" (lỗi thanh kéo 0x0).
    private val View.isAdded get() = parent != null

    private fun isLandscape() =
        context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun dp(v: Float) = (v * density).toInt()

    private companion object {
        const val ANIM_MS = 220L
    }
}
