package com.ezla.oslauncher.controlcenter.data

// Mép đặt thanh kéo mở Control Center (màn Position). Mặc định Top như Figma.
enum class CcEdge {
    LEFT, TOP, BOTTOM, RIGHT;

    val isVertical get() = this == LEFT || this == RIGHT

    companion object {
        fun fromName(name: String?) = values().firstOrNull { it.name == name } ?: TOP
    }
}
