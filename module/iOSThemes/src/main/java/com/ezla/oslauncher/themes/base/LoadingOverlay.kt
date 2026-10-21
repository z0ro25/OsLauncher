package com.ezla.oslauncher.themes.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.ezla.oslauncher.themes.R

// Loading full màn khi tải + áp theme/icon/nền. Gắn vào android.R.id.content (không vào layout của
// màn) để phủ cả banner; đang hiện thì chặn Back để không thoát giữa lúc áp dở.
class LoadingOverlay(private val activity: ComponentActivity) {

    private var view: View? = null

    private val blockBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = Unit
    }

    init {
        activity.onBackPressedDispatcher.addCallback(activity, blockBack)
    }

    fun show() {
        if (view != null) return
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        view = LayoutInflater.from(activity).inflate(R.layout.themes_view_loading, root, false)
            .also { root.addView(it) }
        blockBack.isEnabled = true
    }

    fun hide() {
        view?.let { (it.parent as? ViewGroup)?.removeView(it) }
        view = null
        blockBack.isEnabled = false
    }
}
