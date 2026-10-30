package com.ezla.oslauncher.measure.ar

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt

// Vẽ camera + đo trên GL thread; anchor/session chỉ đụng ở GL thread. UI gửi thao tác
// qua [GLSurfaceView.queueEvent] và nhận [MeasureFrame] mỗi khung hình.
internal class ArMeasureRenderer(
    private val glView: GLSurfaceView,
    private val onFrame: (MeasureFrame) -> Unit,
    private val onLineMeasured: (Float) -> Unit
) : GLSurfaceView.Renderer {

    @Volatile
    var session: Session? = null
        set(value) {
            field = value
            textureBound = false
        }

    private val background = CameraBackground()
    private val main = Handler(Looper.getMainLooper())
    @Volatile
    private var textureBound = false
    private var width = 0
    private var height = 0
    private var displayRotation = 0

    private var pending: Anchor? = null
    private val segments = ArrayList<Pair<Anchor, Anchor>>()
    private var captureCallback: ((Bitmap?) -> Unit)? = null

    private val view = FloatArray(16)
    private val proj = FloatArray(16)
    private val viewProj = FloatArray(16)

    fun setDisplayRotation(rotation: Int) {
        displayRotation = rotation
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        background.create()
        textureBound = false
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w
        height = h
        GLES20.glViewport(0, 0, w, h)
        session?.setDisplayGeometry(displayRotation, w, h)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val s = session ?: return
        if (!textureBound) {
            s.setCameraTextureName(background.textureId)
            s.setDisplayGeometry(displayRotation, width, height)
            textureBound = true
        }
        val frame = try {
            s.update()
        } catch (e: Exception) {
            Log.w(TAG, "session.update failed", e)
            return
        }
        background.draw(frame)
        deliverCapture()
        publish(frame)
    }

    // ---- Thao tác từ UI (đã ở GL thread nhờ queueEvent) ----

    /** Nút "+": điểm đầu -> giữ chờ; điểm thứ hai -> chốt đoạn thẳng. */
    fun addPoint() = glView.queueEvent {
        val frame = lastFrame ?: return@queueEvent
        val hit = centerHit(frame) ?: return@queueEvent
        val anchor = try {
            hit.createAnchor()
        } catch (e: Exception) {
            return@queueEvent
        }
        val first = pending
        if (first == null) {
            pending = anchor
        } else {
            segments.add(first to anchor)
            pending = null
            val d = distance(first.pose, anchor.pose)
            main.post { onLineMeasured(d) }
        }
    }

    /** Undo: bỏ điểm đang chờ, không có thì bỏ đoạn thẳng cuối. */
    fun undo() = glView.queueEvent {
        val p = pending
        if (p != null) {
            p.detach()
            pending = null
        } else if (segments.isNotEmpty()) {
            val last = segments.removeAt(segments.lastIndex)
            last.first.detach()
            last.second.detach()
        }
    }

    fun clear() = glView.queueEvent {
        pending?.detach()
        pending = null
        segments.forEach { it.first.detach(); it.second.detach() }
        segments.clear()
    }

    /** Ảnh khung hình camera kế tiếp (chưa có lớp vẽ đo), trả về main thread. */
    fun captureCamera(callback: (Bitmap?) -> Unit) = glView.queueEvent { captureCallback = callback }

    // ---- Nội bộ ----

    private var lastFrame: Frame? = null

    private fun publish(frame: Frame) {
        lastFrame = frame
        val camera = frame.camera
        val tracking = camera.trackingState == TrackingState.TRACKING
        if (!tracking) {
            main.post { onFrame(MeasureFrame(tracking = false)) }
            return
        }
        camera.getViewMatrix(view, 0)
        camera.getProjectionMatrix(proj, 0, NEAR, FAR)
        Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0)

        val centerPose = centerHit(frame)?.hitPose
        val camPose = camera.pose
        val lines = segments.map { (a, b) -> screenLine(a.pose, b.pose, camPose) }
        val p = pending
        val live = if (p != null && centerPose != null) screenLine(p.pose, centerPose, camPose) else null
        val pendingPoint = if (p != null) project(p.pose) else null

        val result = MeasureFrame(
            tracking = true,
            hasSurface = centerPose != null,
            lines = lines,
            pendingPoint = pendingPoint,
            liveLine = live,
            hasPending = p != null
        )
        main.post { onFrame(result) }
    }

    // Điểm ARCore dưới tâm màn: mặt phẳng (trong vùng đã quét), điểm có pháp tuyến, hoặc điểm độ sâu.
    private fun centerHit(frame: Frame): HitResult? {
        if (width == 0 || height == 0) return null
        val hits = try {
            frame.hitTest(width / 2f, height / 2f)
        } catch (e: Exception) {
            return null
        }
        return hits.firstOrNull { hit ->
            when (val t = hit.trackable) {
                is Plane -> t.isPoseInPolygon(hit.hitPose) && t.trackingState == TrackingState.TRACKING
                is Point -> t.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
                is DepthPoint -> true
                else -> false
            }
        }
    }

    // Thước kẻ (Figma "Ruler") hiện khi camera cách đoạn đo dưới RULER_DISTANCE.
    private fun screenLine(a: Pose, b: Pose, cam: Pose): ScreenLine {
        val d = distance(a, b)
        val mid = Pose.makeTranslation((a.tx() + b.tx()) / 2f, (a.ty() + b.ty()) / 2f, (a.tz() + b.tz()) / 2f)
        val near = distance(mid, cam) < RULER_DISTANCE
        return ScreenLine(project(a), project(b), d, near)
    }

    // Toạ độ màn (px của GL view); null khi điểm nằm sau camera.
    private fun project(pose: Pose): ScreenPoint? {
        val world = floatArrayOf(pose.tx(), pose.ty(), pose.tz(), 1f)
        val clip = FloatArray(4)
        Matrix.multiplyMV(clip, 0, viewProj, 0, world, 0)
        if (clip[3] <= 0f) return null
        val x = (clip[0] / clip[3] + 1f) * 0.5f * width
        val y = (1f - clip[1] / clip[3]) * 0.5f * height
        return ScreenPoint(x, y)
    }

    private fun distance(a: Pose, b: Pose): Float {
        val dx = a.tx() - b.tx()
        val dy = a.ty() - b.ty()
        val dz = a.tz() - b.tz()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    // Đọc framebuffer ngay sau khi vẽ camera; GL lật trục Y nên phải đảo lại từng hàng.
    private fun deliverCapture() {
        val cb = captureCallback ?: return
        captureCallback = null
        val bmp = try {
            val buf = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf)
            val raw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            raw.copyPixelsFromBuffer(buf)
            val m = android.graphics.Matrix().apply { preScale(1f, -1f) }
            Bitmap.createBitmap(raw, 0, 0, width, height, m, false).also { raw.recycle() }
        } catch (e: Throwable) {
            Log.w(TAG, "capture failed", e)
            null
        }
        main.post { cb(bmp) }
    }

    private companion object {
        const val TAG = "ArMeasure"
        const val NEAR = 0.05f
        const val FAR = 100f
        const val RULER_DISTANCE = 0.6f
    }
}

internal data class ScreenPoint(val x: Float, val y: Float)

// [nearRuler]: camera đủ gần -> vẽ vạch thước dọc đoạn (Figma "Ruler").
internal data class ScreenLine(val a: ScreenPoint?, val b: ScreenPoint?, val meters: Float, val nearRuler: Boolean = false)

internal data class MeasureFrame(
    val tracking: Boolean,
    val hasSurface: Boolean = false,
    val lines: List<ScreenLine> = emptyList(),
    val pendingPoint: ScreenPoint? = null,
    val liveLine: ScreenLine? = null,
    val hasPending: Boolean = false
)
