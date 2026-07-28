package com.oslauncher.applauncher.themelauncher.model

import java.io.Serializable
import kotlin.random.Random

data class YourWallpaper(
    var id: Int = 1 + Random.nextInt(),
    var homeBGPath: String? = "",
    var lockBGPath: String? = "",
    var listEmoji: String = "",
    var emojiBgColor: String = "",
    var emojiType: Int = 0
) : Serializable
