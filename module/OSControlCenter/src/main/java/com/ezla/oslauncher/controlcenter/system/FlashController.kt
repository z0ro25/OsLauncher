package com.ezla.oslauncher.controlcenter.system

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

// Bật/tắt đèn pin. Trạng thái lấy từ TorchCallback (không tự giữ cờ) để khớp khi bật từ nơi khác.
class FlashController(context: Context, private val onChanged: (Boolean) -> Unit) {

    private val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = runCatching {
        cm.cameraIdList.firstOrNull { id ->
            val ch = cm.getCameraCharacteristics(id)
            ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                ch.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }.getOrNull()

    var isOn = false
        private set

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id != cameraId) return
            isOn = enabled
            onChanged(enabled)
        }
    }

    val isAvailable get() = cameraId != null

    fun start() {
        runCatching { cm.registerTorchCallback(callback, Handler(Looper.getMainLooper())) }
    }

    fun stop() {
        runCatching { cm.unregisterTorchCallback(callback) }
    }

    fun toggle() {
        val id = cameraId ?: return
        runCatching { cm.setTorchMode(id, !isOn) }
    }
}
