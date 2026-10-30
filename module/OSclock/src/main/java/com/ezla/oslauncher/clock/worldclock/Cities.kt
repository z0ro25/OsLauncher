package com.ezla.oslauncher.clock.worldclock

import java.util.Locale
import java.util.TimeZone

/** Thành phố = 1 múi giờ IANA dạng "Vùng/Thành_phố"; tên nước lấy qua ICU (API 24+). */
data class City(val zoneId: String, val name: String, val country: String) {
    val display: String get() = if (country.isBlank()) name else "$name, $country"
}

object Cities {

    private val REGIONS = setOf("Africa", "America", "Antarctica", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific")

    // Thành phố không có zone IANA riêng (Hà Nội dùng Asia/Ho_Chi_Minh): lưu id dạng "zone#Tên".
    private val ALIASES = listOf("Asia/Ho_Chi_Minh#Hanoi")

    /** Zone IANA thật của id đã lưu (bỏ phần "#Tên" của thành phố bí danh). */
    fun zoneOf(id: String): String = id.substringBefore('#')

    fun nameOf(id: String): String =
        if ('#' in id) id.substringAfter('#') else id.substringAfterLast('/').replace('_', ' ')

    fun of(zoneId: String, locale: Locale = Locale.getDefault()): City {
        val region = runCatching { android.icu.util.TimeZone.getRegion(zoneOf(zoneId)) }.getOrNull()
        val country = if (region == null || region == "001") "" else Locale("", region).getDisplayCountry(locale)
        return City(zoneId, nameOf(zoneId), country)
    }

    /** Toàn bộ thành phố, sắp theo tên; bỏ id kiểu "Etc/GMT+1", "SystemV/..." và id trùng tên. */
    fun all(locale: Locale = Locale.getDefault()): List<City> =
        TimeZone.getAvailableIDs()
            .filter { id -> id.substringBefore('/') in REGIONS && id.count { it == '/' } >= 1 }
            .plus(ALIASES)
            .map { of(it, locale) }
            .distinctBy { it.display }
            .sortedBy { it.name }

    /** Khớp đầu từ của tên thành phố hoặc tên nước (iOS: gõ "Par" ra Paris, Paramaribo, Paraná). */
    fun search(all: List<City>, query: String): List<City> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter { c ->
            c.name.lowercase().split(' ', '-').any { it.startsWith(q) } || c.name.lowercase().startsWith(q) ||
                c.country.lowercase().startsWith(q)
        }.sortedBy { if (it.name.lowercase().startsWith(q)) 0 else 1 }
    }
}
