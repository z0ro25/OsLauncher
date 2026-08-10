package com.oslauncher.applauncher.themelauncher.Features.general.applibrary.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import com.oslauncher.applauncher.themelauncher.databinding.DialogNewFolderBinding

/**
 * Dialog nhập tên folder mới (Manage Library). Validate ở caller qua [onSave] trả về true nếu hợp lệ
 * (đã lưu) -> đóng; false -> giữ dialog (tên rỗng/trùng).
 */
class NewFolderDialog(context: Context) : Dialog(context) {

    private lateinit var binding: DialogNewFolderBinding

    /** Trả về true nếu tên hợp lệ và đã lưu (dialog sẽ đóng). */
    var onSave: ((name: String) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogNewFolderBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
        }

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener {
            val name = binding.etFolderName.text.toString().trim()
            val handled = onSave?.invoke(name) ?: false
            if (handled) dismiss()
        }
    }
}
