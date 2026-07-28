package com.oslauncher.applauncher.themelauncher.model

import java.io.Serializable

data class GradientColorModel(
    var startColor: String,
    var endColor: String,
    var isSelected: Boolean = false
) : Serializable