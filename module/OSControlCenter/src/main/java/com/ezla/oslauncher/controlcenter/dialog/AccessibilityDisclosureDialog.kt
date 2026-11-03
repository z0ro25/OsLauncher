package com.ezla.oslauncher.controlcenter.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.databinding.CcDialogDisclosureBinding

// Giải thích + xin đồng ý trước khi đưa user tới màn Accessibility (chính sách Google Play).
class AccessibilityDisclosureDialog(context: Context, private val onAgree: () -> Unit) :
    Dialog(context, R.style.CcDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = CcDialogDisclosureBinding.inflate(layoutInflater)
        setContentView(b.root)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        b.btnDeny.setOnClickListener { dismiss() }
        b.btnAgree.setOnClickListener {
            dismiss()
            onAgree()
        }
    }
}
