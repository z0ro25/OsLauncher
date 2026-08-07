package com.oslauncher.applauncher.themelauncher.Features.appearance

import android.view.View
import android.widget.CompoundButton
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ActivityAppearanceBinding
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.oslauncher.applauncher.themelauncher.theme.AppThemeManager

/**
 * Màn Appearance (theo Figma): chọn Light/Dark THẬT + 2 toggle Auto/System loại trừ nhau.
 *
 * Ba policy loại trừ nhau (xem [AppThemeManager]):
 *  - MANUAL: card Light/Dark. AUTO: switch Auto (đổi theo giờ). SYSTEM: switch System (theo máy).
 *  - Bật 1 switch tự tắt switch kia + bỏ chọn thủ công; tắt switch -> quay lại Light/Dark thủ công.
 *
 * setPolicy/setManualMode có thể gọi setDefaultNightMode -> activity recreate -> initView chạy lại
 * -> refreshSelection() hiển thị đúng. Trường hợp không recreate thì refreshSelection() ngay sau đó.
 */
class AppearanceActivity : BaseActivity<ActivityAppearanceBinding>() {

    override val setViewBinding: ActivityAppearanceBinding
        get() = ActivityAppearanceBinding.inflate(layoutInflater)

    private val autoListener = CompoundButton.OnCheckedChangeListener { _, checked ->
        if (checked) {
            AppThemeManager.setPolicy(this, AppThemeManager.POLICY_AUTO)
        } else {
            // Tắt Auto -> quay lại chọn thủ công theo mode đang lưu.
            AppThemeManager.setManualMode(this, AppThemeManager.getMode(this))
        }
        refreshSelection()
    }

    private val systemListener = CompoundButton.OnCheckedChangeListener { _, checked ->
        if (checked) {
            AppThemeManager.setPolicy(this, AppThemeManager.POLICY_SYSTEM)
        } else {
            AppThemeManager.setManualMode(this, AppThemeManager.getMode(this))
        }
        refreshSelection()
    }

    override fun initView() {
        onBackPressedDispatcher.addCallback { finish() }
        refreshSelection()
    }

    override fun viewListener() {
        binding.ivBack.tap { finish() }

        // Light/Dark: chọn thủ công (chuyển policy về MANUAL, tự tắt Auto/System).
        binding.llLight.tap {
            AppThemeManager.setManualMode(this, AppThemeManager.MODE_LIGHT)
            refreshSelection()
        }
        binding.llDark.tap {
            AppThemeManager.setManualMode(this, AppThemeManager.MODE_DARK)
            refreshSelection()
        }

        binding.swAuto.setOnCheckedChangeListener(autoListener)
        binding.swSystem.setOnCheckedChangeListener(systemListener)
    }

    override fun dataObservable() {}

    /**
     * Đồng bộ UI theo policy hiện hành: trạng thái 2 switch (loại trừ nhau) + viền/label card
     * theo dark thực tế. Tạm gỡ listener khi set isChecked để không kích hoạt vòng lặp.
     */
    private fun refreshSelection() {
        val policy = AppThemeManager.getPolicy(this)

        binding.swAuto.setOnCheckedChangeListener(null)
        binding.swSystem.setOnCheckedChangeListener(null)
        binding.swAuto.isChecked = policy == AppThemeManager.POLICY_AUTO
        binding.swSystem.isChecked = policy == AppThemeManager.POLICY_SYSTEM
        binding.swAuto.setOnCheckedChangeListener(autoListener)
        binding.swSystem.setOnCheckedChangeListener(systemListener)

        val isDark = AppThemeManager.isDark(this)
        val accent = ContextCompat.getColor(this, R.color.onb_accent)
        val primary = ContextCompat.getColor(this, R.color.onb_text_primary)

        binding.vLightBorder.visibility = if (isDark) View.GONE else View.VISIBLE
        binding.vDarkBorder.visibility = if (isDark) View.VISIBLE else View.GONE
        binding.tvLight.setTextColor(if (isDark) primary else accent)
        binding.tvDark.setTextColor(if (isDark) accent else primary)
    }
}
