package com.ezla.oslauncher.controlcenter.features.permission

import com.ezla.oslauncher.appbase.base.BaseActivity
import com.ezla.oslauncher.controlcenter.databinding.CcActivityPermissionsBinding
import com.ezla.oslauncher.controlcenter.dialog.AccessibilityDisclosureDialog
import com.ezla.oslauncher.controlcenter.system.CcActions
import com.ezla.oslauncher.controlcenter.system.CcPermissions

/** Màn quyền (Figma "Permissions"): công tắc phản ánh trạng thái thật, bấm mở màn hệ thống. */
class CcPermissionsActivity : BaseActivity<CcActivityPermissionsBinding>() {

    override val setViewBinding get() = CcActivityPermissionsBinding.inflate(layoutInflater)

    override fun initView() {
        showBannerOverlay(binding.frBanner, binding.mainLayout, false)
    }

    override fun viewListener() {
        binding.btnBack.setOnClickListener { finish() }
        binding.rowAccessibility.setOnClickListener {
            // Đã bật thì vẫn cho vào Settings để tắt; chưa bật thì phải qua hộp giải thích trước.
            if (CcPermissions.isAccessibilityEnabled(this)) {
                CcActions.start(this, CcPermissions.accessibilitySettings())
            } else {
                AccessibilityDisclosureDialog(this) {
                    CcActions.start(this, CcPermissions.accessibilitySettings())
                }.show()
            }
        }
        binding.rowBattery.setOnClickListener { CcActions.start(this, CcPermissions.batterySettings()) }
        binding.rowNotification.setOnClickListener {
            CcActions.start(this, CcPermissions.notificationListenerSettings())
        }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        binding.swAccessibility.isChecked = CcPermissions.isAccessibilityEnabled(this)
        binding.swBattery.isChecked = CcPermissions.isIgnoringBatteryOptimizations(this)
        binding.swNotification.isChecked = CcPermissions.isNotificationListenerEnabled(this)
    }
}
