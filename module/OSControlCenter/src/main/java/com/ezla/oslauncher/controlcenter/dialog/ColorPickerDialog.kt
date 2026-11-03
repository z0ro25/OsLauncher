package com.ezla.oslauncher.controlcenter.dialog

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.databinding.CcDialogColorPickerBinding
import com.ezla.oslauncher.controlcenter.databinding.CcItemSwatchBinding

// Hộp chọn màu thanh kéo (Figma "Color setting"). Mọi ô nhập / thanh kéo đồng bộ qua [setColor].
class ColorPickerDialog(
    context: Context,
    initial: Int,
    private val onApply: (Int) -> Unit
) : Dialog(context, R.style.CcDialog) {

    private lateinit var b: CcDialogColorPickerBinding
    private val hsv = FloatArray(3)
    private var alpha = 255
    private var syncing = false

    init {
        Color.colorToHSV(initial, hsv)
        alpha = Color.alpha(initial)
    }

    private val color get() = Color.HSVToColor(alpha, hsv)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = CcDialogColorPickerBinding.inflate(layoutInflater)
        setContentView(b.root)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        b.alphaBar.isAlpha = true
        b.satVal.onChanged = { s, v ->
            hsv[1] = s
            hsv[2] = v
            render(fromInputs = false)
        }
        b.hueBar.onChanged = { p ->
            hsv[0] = p * 360f
            render(fromInputs = false)
        }
        b.alphaBar.onChanged = { p ->
            alpha = (p * 255).toInt()
            render(fromInputs = false)
        }
        b.etHex.addTextChangedListener(watcher { parseHex() })
        listOf(b.etR, b.etG, b.etB, b.etA).forEach { it.addTextChangedListener(watcher { parseRgba() }) }

        b.rvSwatches.layoutManager = GridLayoutManager(context, 8)
        b.rvSwatches.adapter = SwatchAdapter { c ->
            Color.colorToHSV(c, hsv)
            alpha = 255
            render(fromInputs = false)
        }
        b.btnCancel.setOnClickListener { dismiss() }
        b.btnApply.setOnClickListener {
            onApply(color)
            dismiss()
        }
        render(fromInputs = false)
    }

    private fun render(fromInputs: Boolean) {
        syncing = true
        val c = color
        b.satVal.hue = hsv[0]
        b.satVal.set(hsv[1], hsv[2])
        b.hueBar.position = hsv[0] / 360f
        b.alphaBar.baseColor = Color.HSVToColor(hsv)
        b.alphaBar.position = alpha / 255f
        b.preview.backgroundTintList = ColorStateList.valueOf(c)
        if (!fromInputs) {
            setText(b.etHex, String.format("#%06X", c and 0xFFFFFF))
            setText(b.etR, Color.red(c).toString())
            setText(b.etG, Color.green(c).toString())
            setText(b.etB, Color.blue(c).toString())
            setText(b.etA, (alpha * 100 / 255).toString())
        }
        syncing = false
    }

    private fun setText(et: EditText, s: String) {
        if (et.text.toString() != s) et.setText(s)
    }

    private fun parseHex() {
        val raw = b.etHex.text.toString().trim().removePrefix("#")
        if (raw.length != 6 && raw.length != 8) return
        val c = raw.toLongOrNull(16) ?: return
        val argb = if (raw.length == 6) (0xFF000000 or c).toInt() else c.toInt()
        Color.colorToHSV(argb, hsv)
        alpha = Color.alpha(argb)
        render(fromInputs = true)
        // Hex đổi thì cập nhật luôn r/g/b/a (không đụng ô hex đang gõ).
        syncing = true
        setText(b.etR, Color.red(argb).toString())
        setText(b.etG, Color.green(argb).toString())
        setText(b.etB, Color.blue(argb).toString())
        setText(b.etA, (alpha * 100 / 255).toString())
        syncing = false
    }

    private fun parseRgba() {
        val r = b.etR.text.toString().toIntOrNull()?.coerceIn(0, 255) ?: return
        val g = b.etG.text.toString().toIntOrNull()?.coerceIn(0, 255) ?: return
        val bl = b.etB.text.toString().toIntOrNull()?.coerceIn(0, 255) ?: return
        val a = b.etA.text.toString().toIntOrNull()?.coerceIn(0, 100) ?: return
        Color.RGBToHSV(r, g, bl, hsv)
        alpha = a * 255 / 100
        render(fromInputs = true)
        syncing = true
        setText(b.etHex, String.format("#%06X", color and 0xFFFFFF))
        syncing = false
    }

    private fun watcher(onChange: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) = Unit
        override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            if (!syncing) onChange()
        }
    }

    private class SwatchAdapter(private val onPick: (Int) -> Unit) : RecyclerView.Adapter<SwatchAdapter.H>() {
        class H(val b: CcItemSwatchBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemCount() = SWATCHES.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            H(CcItemSwatchBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(h: H, position: Int) {
            val c = SWATCHES[position]
            h.b.swatch.backgroundTintList = ColorStateList.valueOf(c)
            h.b.swatch.setOnClickListener { onPick(c) }
        }
    }

    private companion object {
        // 16 màu mẫu theo thứ tự Figma (2 hàng x 8); ô 15 Figma trùng #9B9B9B nên đổi #C4C4C4.
        val SWATCHES = intArrayOf(
            0xFFD0021B.toInt(), 0xFFF5A623.toInt(), 0xFFF8E71C.toInt(), 0xFF8B572A.toInt(),
            0xFF7ED321.toInt(), 0xFF417505.toInt(), 0xFFBD10E0.toInt(), 0xFF9013FE.toInt(),
            0xFF4A90E2.toInt(), 0xFF50E3C2.toInt(), 0xFFB8E986.toInt(), 0xFF000000.toInt(),
            0xFF4A4A4A.toInt(), 0xFF9B9B9B.toInt(), 0xFFC4C4C4.toInt(), 0xFFFFFFFF.toInt()
        )
    }
}
