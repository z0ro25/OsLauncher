package com.ezla.oslauncher.measure.data

import java.util.Locale
import kotlin.math.roundToInt

// Định dạng số đo theo Figma: "86 cm" (dòng chính), "2 ft 9.9 in" (dòng phụ ở thẻ chi tiết).
object MeasureFormat {

    fun cm(meters: Float): String {
        val cm = meters * 100f
        return if (cm < 10f) String.format(Locale.US, "%.1f cm", cm) else "${cm.roundToInt()} cm"
    }

    fun feetInches(meters: Float): String {
        val totalIn = meters / 0.0254f
        val ft = (totalIn / 12f).toInt()
        val inch = totalIn - ft * 12
        return if (ft > 0) String.format(Locale.US, "%d ft %.1f in", ft, inch)
        else String.format(Locale.US, "%.1f in", inch)
    }
}
