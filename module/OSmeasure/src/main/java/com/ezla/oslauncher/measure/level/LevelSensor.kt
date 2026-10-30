package com.ezla.oslauncher.measure.level

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

// Thước thuỷ (tab Level): nằm ngửa -> [Mode.FLAT] (lệch x/y kiểu bọt nước);
// dựng đứng -> [Mode.EDGE] (góc xoay quanh trục vuông góc màn).
class LevelSensor(context: Context, private val listener: (LevelReading) -> Unit) : SensorEventListener {

    enum class Mode { FLAT, EDGE }

    data class LevelReading(val mode: Mode, val degrees: Float, val tiltX: Float, val tiltY: Float)

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val g = FloatArray(3)
    private var hasValue = false

    val isAvailable get() = sensor != null

    fun start() {
        sensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() {
        sm.unregisterListener(this)
        hasValue = false
    }

    override fun onSensorChanged(e: SensorEvent) {
        // Lọc thông thấp cho accelerometer thô; gravity sensor đã mượt sẵn nhưng lọc thêm không hại.
        if (!hasValue) {
            e.values.copyInto(g, 0, 0, 3)
            hasValue = true
        } else for (i in 0..2) g[i] += ALPHA * (e.values[i] - g[i])

        val norm = sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]).takeIf { it > 0f } ?: return
        val x = g[0] / norm
        val y = g[1] / norm
        val z = g[2] / norm
        val reading = if (abs(z) > FLAT_THRESHOLD) {
            // Góc giữa pháp tuyến màn và phương thẳng đứng.
            val deg = Math.toDegrees(Math.acos(abs(z).toDouble())).toFloat()
            LevelReading(Mode.FLAT, deg, x, y)
        } else {
            // Góc của trọng lực trong mặt phẳng màn so với cạnh gần nhất (bội 90°).
            var deg = Math.toDegrees(atan2(x.toDouble(), y.toDouble())).toFloat()
            deg = ((deg % 90f) + 90f) % 90f
            if (deg > 45f) deg -= 90f
            LevelReading(Mode.EDGE, -deg, x, y)
        }
        listener(reading)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private companion object {
        const val ALPHA = 0.2f
        // cos(~50°): nghiêng quá mức này coi như đang dựng đứng.
        const val FLAT_THRESHOLD = 0.64f
    }
}
