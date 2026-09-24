package com.ezla.oslauncher.beautylauncher.Features.subs

import androidx.annotation.DrawableRes

// 1 icon app minh hoạ trong dải "Ad-Free" ở màn IAP. label là tên app (không dịch).
data class IapAppIcon(
    @DrawableRes val iconRes: Int,
    val label: String,
)
