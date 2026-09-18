package com.truongnt.ios.ioslite.common.ads

import com.truongnt.ios.ioslite.common.BuildConfig

/**
 * Ad unit ID cho từng [AdsSlot], tách bạch bản TEST và bản THẬT.
 *
 * Bản nào được dùng do [useTestIds] quyết định — build debug lấy ID test của Google,
 * build release lấy ID thật. Nhờ vậy không bao giờ vô tình click vào ad thật khi đang dev.
 */
object AdsIds {

    /** true = dùng ID test của Google (build debug). */
    @JvmField
    val useTestIds: Boolean = BuildConfig.DEBUG

    // ── ID test mẫu của Google ───────────────────────────────────────────
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_REWARD = "ca-app-pub-3940256099942544/5224354917"

    // ── ID thật ──────────────────────────────────────────────────────────
    private const val REAL_BANNER_IN_APP = "ca-app-pub-3607148519095421/8543314319"
    private const val REAL_BANNER_COLLAPSE = "ca-app-pub-3607148519095421/5940850059"
    private const val REAL_INTER_IN_APP = "ca-app-pub-3607148519095421/9728403477"
    private const val REAL_REWARD_IN_APP = "ca-app-pub-3607148519095421/9421281712"
    private const val REAL_NATIVE_IN_APP = "ca-app-pub-3607148519095421/5532183765"
    private const val REAL_APP_OPEN = "ca-app-pub-3607148519095421/4627768380"

    private const val REAL_INTER_SPLASH = "ca-app-pub-3607148519095421/5801249252"
    private const val REAL_NATIVE_LANGUAGE_1 = "ca-app-pub-3607148519095421/3314686714"
    private const val REAL_NATIVE_LANGUAGE_2 = "ca-app-pub-3607148519095421/8108200041"
    private const val REAL_NATIVE_COLLAPSE = "ca-app-pub-3607148519095421/2001605049"
    private const val REAL_NATIVE_SPLASH = "ca-app-pub-3607148519095421/7966775419"
    private const val REAL_NATIVE_FULL_SPLASH = "ca-app-pub-3607148519095421/6569933263"
    private const val REAL_NATIVE_DIALOG_FULL = "ca-app-pub-3607148519095421/3175085916"
    private const val REAL_NATIVE_PERMISSION = "ca-app-pub-3607148519095421/1862004240"
    private const val REAL_NATIVE_ONBOARDING_FULL = "ca-app-pub-3607148519095421/9548922572"
    private const val REAL_NATIVE_ONBOARDING_1 = "ca-app-pub-3607148519095421/8235840907"
    private const val REAL_NATIVE_ONBOARDING_2 = "ca-app-pub-3607148519095421/3370878685"
    private const val REAL_NATIVE_ONBOARDING_3 = "ca-app-pub-3607148519095421/2630688250"
    private const val REAL_NATIVE_ONBOARDING_4 = "ca-app-pub-3607148519095421/9688523372"

    /** Ad unit ID thực tế sẽ dùng cho [slot]. */
    @JvmStatic
    fun idFor(slot: AdsSlot): String = when (slot) {
        AdsSlot.INTER_IN_APP -> pick(TEST_INTERSTITIAL, REAL_INTER_IN_APP)
        AdsSlot.INTER_SPLASH -> pick(TEST_INTERSTITIAL, REAL_INTER_SPLASH)
        AdsSlot.APP_OPEN -> pick(TEST_APP_OPEN, REAL_APP_OPEN)
        AdsSlot.BANNER_IN_APP -> pick(TEST_BANNER, REAL_BANNER_IN_APP)
        AdsSlot.BANNER_COLLAPSE -> pick(TEST_BANNER, REAL_BANNER_COLLAPSE)
        AdsSlot.REWARD_IN_APP -> pick(TEST_REWARD, REAL_REWARD_IN_APP)

        AdsSlot.NATIVE_IN_APP -> pick(TEST_NATIVE, REAL_NATIVE_IN_APP)
        AdsSlot.NATIVE_SPLASH -> pick(TEST_NATIVE, REAL_NATIVE_SPLASH)
        AdsSlot.NATIVE_FULL_SPLASH -> pick(TEST_NATIVE, REAL_NATIVE_FULL_SPLASH)
        AdsSlot.NATIVE_DIALOG_FULL -> pick(TEST_NATIVE, REAL_NATIVE_DIALOG_FULL)
        AdsSlot.NATIVE_LANGUAGE_1 -> pick(TEST_NATIVE, REAL_NATIVE_LANGUAGE_1)
        AdsSlot.NATIVE_LANGUAGE_2 -> pick(TEST_NATIVE, REAL_NATIVE_LANGUAGE_2)
        AdsSlot.NATIVE_PERMISSION -> pick(TEST_NATIVE, REAL_NATIVE_PERMISSION)
        AdsSlot.NATIVE_ONBOARDING_FULL -> pick(TEST_NATIVE, REAL_NATIVE_ONBOARDING_FULL)
        AdsSlot.NATIVE_ONBOARDING_1 -> pick(TEST_NATIVE, REAL_NATIVE_ONBOARDING_1)
        AdsSlot.NATIVE_ONBOARDING_2 -> pick(TEST_NATIVE, REAL_NATIVE_ONBOARDING_2)
        AdsSlot.NATIVE_ONBOARDING_3 -> pick(TEST_NATIVE, REAL_NATIVE_ONBOARDING_3)
        AdsSlot.NATIVE_ONBOARDING_4 -> pick(TEST_NATIVE, REAL_NATIVE_ONBOARDING_4)
        AdsSlot.NATIVE_COLLAPSE -> pick(TEST_NATIVE, REAL_NATIVE_COLLAPSE)
    }

    /**
     * Chọn bản test hay bản thật.
     *
     * Rơi về bản test khi bản thật còn rỗng — để app không chết vì ID chưa điền.
     */
    private fun pick(test: String, real: String): String =
        if (useTestIds || real.isBlank()) test else real
}
