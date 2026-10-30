package com.ezla.oslauncher.clock.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.ezla.oslauncher.clock.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// Sheet tối Figma (nền #1c1c1e, bo 34, cách đỉnh 56dp so với frame 800): mở thẳng full,
// không dừng ở peek. Lớp con cấp binding + bind().
abstract class ClockSheet<B : ViewBinding> : BottomSheetDialogFragment() {

    private var _b: B? = null
    protected val b: B get() = _b!!

    abstract fun inflate(inflater: LayoutInflater, container: ViewGroup?): B
    abstract fun bind()

    override fun getTheme() = R.style.OsclockBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = inflate(inflater, container)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = bind()

    override fun onStart() {
        super.onStart()
        val dm = resources.displayMetrics
        val h = dm.heightPixels - (TOP_GAP_DP * dm.density).toInt()
        _b?.root?.layoutParams = _b?.root?.layoutParams?.apply { height = h }
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            peekHeight = h
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    private companion object {
        // Figma: sheet bắt đầu ở y=56 (status bar 48 + 8).
        const val TOP_GAP_DP = 56
    }
}
