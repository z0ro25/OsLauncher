package com.ezla.oslauncher.controlcenter.skin

// Loại ô panel nhận ảnh nền từ theme (control_systems của server). Server chỉ phân biệt bằng name
// (vd "cc bg 2x2") nên nhận diện theo hậu tố trong name; tỉ lệ ảnh mẫu khớp đúng kích thước từng ô.
enum class CcSkinSlot(private val key: String) {
    TILE_1X1("1x1"),
    TILE_1X2("1x2"),
    TILE_1X4("1x4"),
    TILE_2X1("2x1"),
    TILE_2X2("2x2"),
    MUSIC("music");

    companion object {
        fun fromName(name: String?): CcSkinSlot? {
            val n = name?.lowercase() ?: return null
            return values().firstOrNull { n.contains(it.key) }
        }
    }
}
