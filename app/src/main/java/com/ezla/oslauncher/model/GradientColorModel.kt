package com.ezla.oslauncher.model

import java.io.Serializable

data class GradientColorModel(
    var startColor: String,
    var endColor: String,
    var isSelected: Boolean = false
) : Serializable