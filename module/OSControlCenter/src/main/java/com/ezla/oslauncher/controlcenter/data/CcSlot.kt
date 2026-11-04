package com.ezla.oslauncher.controlcenter.data

// 1 ô trong lưới 1x1 của panel: chức năng hệ thống cố định ([System]) hoặc lối tắt mở 1 app ([App]).
// Lưu dạng chuỗi "sys:FLASHLIGHT" / "app:com.example"; chuỗi cũ không tiền tố là tên CcTile (bản trước).
sealed class CcSlot {

    abstract val key: String

    data class System(val tile: CcTile) : CcSlot() {
        override val key get() = SYS + tile.name
    }

    data class App(val packageName: String) : CcSlot() {
        override val key get() = APP + packageName
    }

    companion object {
        private const val SYS = "sys:"
        private const val APP = "app:"

        // Chuỗi lạ (enum đổi tên, rỗng) -> null để bỏ qua thay vì crash khi đọc prefs cũ.
        fun fromKey(raw: String): CcSlot? {
            val s = raw.trim()
            return when {
                s.startsWith(APP) -> s.removePrefix(APP).takeIf { it.isNotEmpty() }?.let { App(it) }
                else -> CcTile.values().firstOrNull { it.name == s.removePrefix(SYS) }?.let { System(it) }
            }
        }

        val DEFAULT: List<CcSlot> = CcTile.DEFAULT.map { System(it) }
    }
}
