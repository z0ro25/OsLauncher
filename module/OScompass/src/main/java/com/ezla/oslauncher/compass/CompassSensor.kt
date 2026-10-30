package com.ezla.oslauncher.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

// Hướng từ (azimuth, so với bắc từ) + vector trọng lực cho bọt thuỷ / vòng hiệu chỉnh.
// Ưu tiên ROTATION_VECTOR (đã fusion gyro); máy thiếu thì ghép ACCELEROMETER + MAGNETIC_FIELD.
class CompassSensor(context: Context, private val listener: Listener) : SensorEventListener {

    interface Listener {
        fun onAzimuth(degrees: Float)
        fun onGravity(x: Float, y: Float, z: Float)
        fun onAccuracy(needsCalibration: Boolean)
    }

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetic = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasMagnetic = false
    private val r = FloatArray(9)
    private val orientation = FloatArray(3)

    val isAvailable: Boolean get() = rotation != null || (accel != null && magnetic != null)

    fun start() {
        rotation?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        accel?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        // Luôn nghe magnetic để biết accuracy (cần hiệu chỉnh) kể cả khi dùng ROTATION_VECTOR.
        magnetic?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() = sm.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(r, event.values)
                emitAzimuth()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                lowPass(event.values, gravity, hasGravity)
                hasGravity = true
                listener.onGravity(gravity[0], gravity[1], gravity[2])
                if (rotation == null) fuse()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                lowPass(event.values, geomagnetic, hasMagnetic)
                hasMagnetic = true
                if (rotation == null) fuse()
            }
        }
    }

    private fun fuse() {
        if (hasGravity && hasMagnetic && SensorManager.getRotationMatrix(r, null, gravity, geomagnetic)) {
            emitAzimuth()
        }
    }

    private fun emitAzimuth() {
        SensorManager.getOrientation(r, orientation)
        listener.onAzimuth(HeadingMath.normalize(Math.toDegrees(orientation[0].toDouble()).toFloat()))
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return
        listener.onAccuracy(accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
            accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW)
    }

    private fun lowPass(input: FloatArray, out: FloatArray, primed: Boolean) {
        for (i in 0..2) out[i] = if (primed) out[i] + ALPHA * (input[i] - out[i]) else input[i]
    }

    private companion object {
        const val ALPHA = 0.15f
    }
}
