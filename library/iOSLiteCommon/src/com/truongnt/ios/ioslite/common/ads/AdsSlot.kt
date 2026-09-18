package com.truongnt.ios.ioslite.common.ads

/**
 * Mọi vị trí đặt quảng cáo. Mỗi slot ứng với đúng một ad unit ID ở [AdsIds].
 *
 * [alias] là khoá FSDAds dùng để đánh dấu native ad đã preload; đặt trùng tên ad unit
 * để khi đọc log/console map được ngay. Với slot không phải native thì alias chỉ mang
 * tính định danh.
 *
 * Lưu ý: mọi vị trí native trong launcher (App Library, Search, chi tiết theme/wallpaper,
 * các list) đều dùng chung [NATIVE_IN_APP] — phía AdMob chỉ có một ad unit native in-app.
 */
enum class AdsSlot(val alias: String) {

    // ── Không phải native ────────────────────────────────────────────────
    INTER_IN_APP("int_inapp"),
    INTER_SPLASH("int_splash"),
    APP_OPEN("aoa"),
    BANNER_IN_APP("bn_inapp"),
    BANNER_COLLAPSE("bn_clp"),
    REWARD_IN_APP("rv_inapp"),

    // ── Native ───────────────────────────────────────────────────────────

    /** Native dùng chung cho mọi vị trí trong launcher. */
    NATIVE_IN_APP("nt_inapp"),
    NATIVE_SPLASH("nt_splash"),
    NATIVE_FULL_SPLASH("nt_full_splash"),
    NATIVE_DIALOG_FULL("nt_dialog_full"),
    NATIVE_LANGUAGE_1("nt_lang1"),
    NATIVE_LANGUAGE_2("nt_lang2"),
    NATIVE_PERMISSION("nt_permission"),
    NATIVE_ONBOARDING_FULL("nt_onb_full"),
    NATIVE_ONBOARDING_1("nt_onb1"),
    NATIVE_ONBOARDING_2("nt_onb2"),
    NATIVE_ONBOARDING_3("nt_onb3"),
    NATIVE_ONBOARDING_4("nt_onb4"),
    NATIVE_COLLAPSE("nt_clp"),
}
