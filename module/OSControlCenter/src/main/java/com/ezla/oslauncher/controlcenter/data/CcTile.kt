package com.ezla.oslauncher.controlcenter.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ezla.oslauncher.controlcenter.R

// Ô 1x1 thêm/xoá được ở Custom Controls. Khối trên (kết nối, nhạc, xoay, im lặng, focus, 2 thanh)
// cố định như Figma nên không nằm trong enum này.
enum class CcTile(@DrawableRes val icon: Int, @StringRes val label: Int) {
    FLASHLIGHT(R.drawable.cc_ic_flashlight, R.string.cc_tile_flashlight),
    ALARM(R.drawable.cc_ic_alarm, R.string.cc_tile_alarm),
    CALCULATOR(R.drawable.cc_ic_calculator, R.string.cc_tile_calculator),
    CAMERA(R.drawable.cc_ic_camera, R.string.cc_tile_camera),
    SCREEN_RECORD(R.drawable.cc_ic_record, R.string.cc_tile_screen_record),
    DARK_MODE(R.drawable.cc_ic_contrast, R.string.cc_tile_dark_mode),
    LOW_POWER(R.drawable.cc_ic_battery, R.string.cc_tile_low_power),
    VOICE_MEMO(R.drawable.cc_ic_mic, R.string.cc_tile_voice_memo),
    SETTINGS(R.drawable.cc_ic_gear, R.string.cc_tile_settings);

    companion object {
        // 3 hàng x 4 ô dưới khối cố định (Figma Custom Controls có đúng 12 chỗ).
        const val MAX = 12
        val DEFAULT = values().toList()
    }
}
