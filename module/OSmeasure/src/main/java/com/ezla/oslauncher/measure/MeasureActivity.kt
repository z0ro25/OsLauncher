package com.ezla.oslauncher.measure

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.ezla.oslauncher.appbase.base.BaseActivity

import com.ezla.oslauncher.measure.ar.ArMeasureRenderer
import com.ezla.oslauncher.measure.ar.MeasureFrame
import com.ezla.oslauncher.measure.data.MeasureFormat
import com.ezla.oslauncher.measure.data.MeasureStore
import com.ezla.oslauncher.measure.databinding.MeasureActivityMeasureBinding
import com.ezla.oslauncher.measure.ui.MeasurementsSheet
import com.ezla.oslauncher.measure.level.LevelSensor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException
import java.util.concurrent.Executors

/** Thước đo kiểu iOS (Figma "14 · Measure"): đo đoạn thẳng bằng ARCore + thước thuỷ (Level). */
class MeasureActivity : BaseActivity<MeasureActivityMeasureBinding>() {

    override val setViewBinding get() = MeasureActivityMeasureBinding.inflate(layoutInflater)

    // Camera tràn dưới status bar như Figma (toạ độ layout đã gồm status bar 48).
    override val padStatusBar = false

    private val b get() = binding
    private lateinit var store: MeasureStore
    private lateinit var renderer: ArMeasureRenderer
    private lateinit var level: LevelSensor
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private var session: Session? = null
    // requestInstall chỉ được gọi với true một lần: lần sau ARCore tự biết user đã từ chối.
    private var installRequested = false
    private var arUnavailable = false
    // Chỉ tự xin quyền camera 1 lần mỗi phiên: bị từ chối thì hiện chữ hướng dẫn, không xin lặp.
    private var cameraAsked = false
    // false khi redirectBeforeCreate đã finish: chưa có binding nên onDestroy không được đụng tới view.
    private var viewReady = false
    private var levelTab = false
    private var frame = MeasureFrame(tracking = false)
    private var coachAnim: ValueAnimator? = null
    private val hideToast = Runnable {
        b.measureToast.animate().alpha(0f).setDuration(200).withEndAction {
            b.measureToast.visibility = View.GONE
            b.measureThumb.visibility = View.GONE
            b.measureBtnList.visibility = View.VISIBLE
        }
    }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Được cấp thì onResume ngay sau đó tự mở AR; ở đây chỉ xử lý bị từ chối.
        if (!granted) showUnavailable(getString(R.string.measure_camera_permission), openSettings = true)
    }

    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) capture() else Toast.makeText(this, R.string.measure_save_failed, Toast.LENGTH_SHORT).show()
    }

    // Mọi đường vào (icon desktop, MeasureEntry) đều mở thẳng màn này: lần đầu chuyển sang onboarding.
    override fun redirectBeforeCreate(): Boolean {
        if (MeasureEntry.isOnboardingDone(this)) return false
        startActivity(Intent(this, MeasureOnboardingActivity::class.java))
        return true
    }

    override fun initView() {
        viewReady = true
        store = MeasureStore(this)
        renderer = ArMeasureRenderer(b.measureGl, ::onArFrame) { meters -> store.add(meters) }
        b.measureGl.apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(renderer)
            renderMode = android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        level = LevelSensor(this) { b.measureLevel.update(it) }
        selectTab(level = false)
        renderControls()
    }

    override fun viewListener() {
        b.measureTabMeasure.setOnClickListener { selectTab(level = false) }
        b.measureTabLevel.setOnClickListener { selectTab(level = true) }
        b.measureBtnAdd.setOnClickListener { if (frame.hasSurface) renderer.addPoint() }
        b.measureBtnUndo.setOnClickListener { renderer.undo() }
        b.measureBtnClear.setOnClickListener { renderer.clear() }
        b.measureBtnList.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(MeasurementsSheet.TAG) == null) {
                MeasurementsSheet().show(supportFragmentManager, MeasurementsSheet.TAG)
            }
        }
        b.measureBtnCapture.setOnClickListener { requestCapture() }
        b.measureDetailDone.setOnClickListener { hideDetail() }
        b.measureDetail.setOnClickListener { hideDetail() }
        b.measureTvUnsupported.setOnClickListener { if (it.tag == TAG_OPEN_SETTINGS) openAppSettings() }
        // Chạm nhãn số đo -> thẻ chi tiết (Figma "Measurement Detail").
        b.measureOverlay.setOnTouchListener { _, e ->
            if (e.action != MotionEvent.ACTION_UP) return@setOnTouchListener true
            b.measureOverlay.lineAt(e.x, e.y)?.let { showDetail(it.meters) }
            true
        }
        onBackPressedDispatcher.addCallback(this) {
            if (b.measureDetail.visibility == View.VISIBLE) hideDetail()
            else finish()
        }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        if (levelTab) level.start() else resumeAr()
    }

    override fun onPause() {
        super.onPause()
        level.stop()
        pauseAr()
    }

    override fun onDestroy() {
        coachAnim?.cancel()
        if (viewReady) b.measureToast.removeCallbacks(hideToast)
        session?.close()
        session = null
        ioExecutor.shutdown()
        super.onDestroy()
    }

    // ---- AR ----

    private fun resumeAr() {
        if (arUnavailable) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (!cameraAsked) {
                cameraAsked = true
                cameraPermission.launch(Manifest.permission.CAMERA)
            } else {
                showUnavailable(getString(R.string.measure_camera_permission), openSettings = true)
            }
            return
        }
        startAr()
    }

    private fun startAr() {
        if (session == null) {
            try {
                when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        // Màn cài ARCore của Google Play mở ra; quay lại sẽ vào onResume lần nữa.
                        installRequested = true
                        return
                    }
                    else -> Unit
                }
                session = Session(this).also { configure(it) }
            } catch (e: UnavailableException) {
                Log.w(TAG, "ARCore unavailable", e)
                arUnavailable = true
                showUnavailable(getString(R.string.measure_ar_unsupported), openSettings = false)
                return
            } catch (e: Exception) {
                Log.w(TAG, "ARCore session failed", e)
                arUnavailable = true
                showUnavailable(getString(R.string.measure_ar_unsupported), openSettings = false)
                return
            }
        }
        b.measureTvUnsupported.visibility = View.GONE
        val s = session ?: return
        try {
            s.resume()
        } catch (e: Exception) {
            Log.w(TAG, "session.resume failed", e)
            showUnavailable(getString(R.string.measure_ar_unsupported), openSettings = false)
            return
        }
        renderer.setDisplayRotation(currentRotation())
        renderer.session = s
        b.measureGl.onResume()
    }

    private fun pauseAr() {
        b.measureGl.onPause()
        session?.pause()
    }

    private fun configure(s: Session) {
        val config = Config(s).apply {
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            focusMode = Config.FocusMode.AUTO
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            // Có depth thì chấm được cả bề mặt chưa thành mặt phẳng (mặt mèo, đồ vật).
            if (s.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) depthMode = Config.DepthMode.AUTOMATIC
        }
        s.configure(config)
    }

    @Suppress("DEPRECATION")
    private fun currentRotation(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display?.rotation ?: 0
        else windowManager.defaultDisplay.rotation

    private fun onArFrame(f: MeasureFrame) {
        if (isFinishing || levelTab) return
        frame = f
        b.measureOverlay.update(f)
        renderControls()
    }

    // ---- UI ----

    private fun renderControls() {
        val measuring = !levelTab && !arUnavailable && b.measureTvUnsupported.visibility != View.VISIBLE &&
            b.measureDetail.visibility != View.VISIBLE
        val hasItems = frame.lines.isNotEmpty()
        val started = frame.tracking && (frame.hasSurface || frame.hasPending || hasItems)
        setCoach(measuring && !started)
        b.measureBtnUndo.visibility = if (measuring && started) View.VISIBLE else View.GONE
        b.measureBtnClear.visibility = if (measuring && hasItems) View.VISIBLE else View.GONE
        b.measureBtnAdd.visibility = if (levelTab) View.INVISIBLE else View.VISIBLE
        b.measureBtnAdd.alpha = if (measuring && frame.hasSurface) 1f else DISABLED_ALPHA
        // Figma "Ruler": có đoạn đo mà camera còn xa -> nhắc lại gần để hiện vạch thước.
        val hint = measuring && b.measureOverlay.hasLines && !b.measureOverlay.showingRuler
        b.measureTvRulerHint.visibility = if (hint) View.VISIBLE else View.GONE
    }

    // Figma "Move iPhone to start": mũi tên đưa qua lại trong lúc ARCore chưa tìm được bề mặt.
    private fun setCoach(show: Boolean) {
        val visible = b.measureCoach.visibility == View.VISIBLE
        if (show == visible) return
        b.measureCoach.visibility = if (show) View.VISIBLE else View.GONE
        coachAnim?.cancel()
        coachAnim = if (show) ObjectAnimator.ofFloat(
            b.measureCoachArrow, View.TRANSLATION_X, -COACH_SWING, COACH_SWING
        ).apply {
            duration = COACH_MS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        } else null
    }

    private fun selectTab(level: Boolean) {
        levelTab = level
        b.measureTabMeasure.isSelected = !level
        b.measureTabLevel.isSelected = level
        b.measureArLayer.visibility = if (level) View.GONE else View.VISIBLE
        b.measureLevel.visibility = if (level) View.VISIBLE else View.GONE
        hideDetail()
        if (level) {
            setCoach(false)
            pauseAr()
            this.level.start()
        } else {
            this.level.stop()
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) resumeAr()
        }
        renderControls()
    }

    private fun showUnavailable(message: String, openSettings: Boolean) {
        b.measureTvUnsupported.text = message
        b.measureTvUnsupported.tag = if (openSettings) TAG_OPEN_SETTINGS else null
        b.measureTvUnsupported.visibility = View.VISIBLE
        renderControls()
    }

    private fun showDetail(meters: Float) {
        val value = MeasureFormat.cm(meters)
        b.measureDetailValue.text = value
        b.measureDetailSub.text = MeasureFormat.feetInches(meters)
        b.measureDetailCopy.setOnClickListener { copyText(value) }
        b.measureDetail.visibility = View.VISIBLE
        // Figma "Measurement Detail": chỉ còn thẻ trên camera, ẩn hết nút phía trên và thanh dưới.
        setChromeVisible(false)
    }

    private fun hideDetail() {
        b.measureDetail.visibility = View.GONE
        setChromeVisible(true)
    }

    // Undo / Clear / gợi ý thước do renderControls() lo (chạy mỗi frame AR) nên chỉ cần gọi lại nó.
    private fun setChromeVisible(show: Boolean) {
        b.measureControls.visibility = if (show) View.VISIBLE else View.INVISIBLE
        renderControls()
    }

    fun copyText(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(getString(R.string.measure_app_name), text))
        Toast.makeText(this, R.string.measure_copied, Toast.LENGTH_SHORT).show()
    }

    private fun openAppSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        }
    }

    // ---- Chụp ảnh lưu Photos (Figma "Captured") ----

    private fun requestCapture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        capture()
    }

    private fun capture() {
        flash()
        if (levelTab || session == null || b.measureTvUnsupported.visibility == View.VISIBLE) {
            val target = if (levelTab) b.measureLevel else b.measureArLayer
            save(renderView(target, null))
            return
        }
        // Ảnh camera đọc từ GL rồi vẽ thêm lớp đường đo lên trên.
        renderer.captureCamera { camera -> save(renderView(b.measureOverlay, camera)) }
    }

    private fun renderView(view: View, base: Bitmap?): Bitmap? {
        if (view.width == 0 || view.height == 0) return base
        return runCatching {
            val out = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            if (base != null) {
                c.drawBitmap(base, null, android.graphics.Rect(0, 0, out.width, out.height), null)
                base.recycle()
            }
            view.draw(c)
            out
        }.getOrNull()
    }

    private fun save(bmp: Bitmap?) {
        if (bmp == null) {
            Toast.makeText(this, R.string.measure_save_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val appCtx = applicationContext
        ioExecutor.execute {
            val ok = writeToGallery(appCtx, bmp)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (ok) showSavedToast(bmp) else {
                    Toast.makeText(this, R.string.measure_save_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun writeToGallery(context: Context, bmp: Bitmap): Boolean = try {
        val name = "Measure_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Measure")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("insert failed")
        resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            ?: error("open failed")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    } catch (e: Exception) {
        Log.w(TAG, "save to gallery failed", e)
        false
    }

    private fun flash() {
        b.measureFlash.visibility = View.VISIBLE
        b.measureFlash.alpha = 1f
        b.measureFlash.animate().alpha(0f).setDuration(FLASH_MS)
            .withEndAction { b.measureFlash.visibility = View.GONE }.start()
    }

    private fun showSavedToast(bmp: Bitmap) {
        b.measureToast.removeCallbacks(hideToast)
        b.measureThumb.setImageBitmap(bmp)
        b.measureThumb.visibility = View.VISIBLE
        b.measureBtnList.visibility = View.INVISIBLE
        b.measureToast.visibility = View.VISIBLE
        b.measureToast.animate().cancel()
        b.measureToast.animate().alpha(1f).setDuration(180).start()
        b.measureToast.postDelayed(hideToast, TOAST_MS)
    }

    private companion object {
        const val TAG = "MeasureActivity"
        const val TAG_OPEN_SETTINGS = "settings"
        const val DISABLED_ALPHA = 0.5f
        const val COACH_SWING = 24f
        const val COACH_MS = 900L
        const val FLASH_MS = 350L
        const val TOAST_MS = 2000L
        const val JPEG_QUALITY = 92
    }
}
