package com.ezla.oslauncher.controlcenter.system

import android.view.HapticFeedbackConstants
import android.view.View
import com.ezla.oslauncher.controlcenter.CcPrefs

// Rung nhẹ khi bấm ô panel, chỉ khi bật công tắc Vibration. performHapticFeedback không cần quyền VIBRATE.
object CcHaptics {
    fun tap(view: View) {
        if (CcPrefs.isVibration(view.context)) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}
