package com.ezla.oslauncher.appbase.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class OnboardingPage(
    @DrawableRes val imageRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int
)
