package com.oslauncher.applauncher.themelauncher.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import com.oslauncher.applauncher.themelauncher.databinding.DialogSetDefaultLauncherBinding

/**
 * Dialog "Set as default launcher" hiện lần đầu vào màn Home (theo Figma node 217:2459).
 * "Set default" -> [onSetDefault] (mở dialog chọn launcher mặc định của hệ thống) rồi đóng.
 * "Cancel" -> chỉ đóng.
 */
class SetDefaultLauncherDialog(context: Context) : Dialog(context) {

    private lateinit var binding: DialogSetDefaultLauncherBinding

    /** Bấm "Set default": caller mở dialog chọn launcher mặc định của hệ thống. */
    var onSetDefault: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSetDefaultLauncherBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
        }

        binding.btnSetDefault.setOnClickListener {
            onSetDefault?.invoke()
            dismiss()
        }
        binding.btnCancel.setOnClickListener { dismiss() }
    }
}
