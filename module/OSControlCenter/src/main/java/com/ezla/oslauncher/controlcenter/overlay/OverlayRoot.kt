package com.ezla.oslauncher.controlcenter.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout

// Gốc cửa sổ overlay: bắt phím Back và cho bên ngoài "nhìn" mọi chạm (không nuốt) để nhận cử chỉ đóng.
@SuppressLint("ViewConstructor")
class OverlayRoot(
    context: Context,
    private val onBack: () -> Unit,
    private val touchSpy: (MotionEvent) -> Unit
) : FrameLayout(context) {

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) onBack()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        touchSpy(ev)
        return super.dispatchTouchEvent(ev)
    }
}
