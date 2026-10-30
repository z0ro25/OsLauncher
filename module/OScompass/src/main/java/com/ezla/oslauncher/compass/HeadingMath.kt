package com.ezla.oslauncher.compass

import kotlin.math.abs
import kotlin.math.roundToInt

/** Phép tính thuần (không Android) cho la bàn — tách riêng để unit test. */
object HeadingMath {

    fun normalize(deg: Float): Float {
        val d = deg % 360f
        return if (d < 0f) d + 360f else d
    }

    // Low-pass theo đường ngắn nhất trên vòng tròn: 359° -> 1° đi qua 0°, không quay ngược 358°.
    fun smooth(current: Float, target: Float, alpha: Float): Float {
        var diff = normalize(target) - normalize(current)
        if (diff > 180f) diff -= 360f else if (diff < -180f) diff += 360f
        return normalize(current + diff * alpha)
    }

    /** 0..7 = N, NE, E, SE, S, SW, W, NW (khớp string-array compass_directions). */
    fun directionIndex(deg: Float): Int = ((normalize(deg) + 22.5f) / 45f).toInt() % 8

    fun displayDegrees(deg: Float): Int = normalize(deg).roundToInt() % 360

    /** Độ / phút / giây của |value|, đã làm tròn giây (60″ dồn sang phút). */
    fun dms(value: Double): IntArray {
        val a = abs(value)
        var d = a.toInt()
        val minutes = (a - d) * 60.0
        var m = minutes.toInt()
        var s = ((minutes - m) * 60.0).roundToInt()
        if (s == 60) { s = 0; m++ }
        if (m == 60) { m = 0; d++ }
        return intArrayOf(d, m, s)
    }

    /** "21°1′42″N  105°51′12″E"; [hemisphere] nhận 0=N 1=E 2=S 3=W (chữ đã localize). */
    fun formatCoordinate(lat: Double, lng: Double, hemisphere: (Int) -> String): String {
        val la = dms(lat)
        val lo = dms(lng)
        val ns = hemisphere(if (lat >= 0) 0 else 2)
        val ew = hemisphere(if (lng >= 0) 1 else 3)
        return "${la[0]}°${la[1]}′${la[2]}″$ns  ${lo[0]}°${lo[1]}′${lo[2]}″$ew"
    }
}
