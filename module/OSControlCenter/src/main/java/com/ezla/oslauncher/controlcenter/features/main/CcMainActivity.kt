package com.ezla.oslauncher.controlcenter.features.main

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.core.view.doOnLayout
import com.ezla.oslauncher.appbase.base.BaseActivity
import com.ezla.oslauncher.controlcenter.CcInter
import com.ezla.oslauncher.controlcenter.CcPrefs
import com.ezla.oslauncher.controlcenter.ControlCenterEntry
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.databinding.CcActivityMainBinding
import com.ezla.oslauncher.controlcenter.dialog.AccessibilityDisclosureDialog
import com.ezla.oslauncher.controlcenter.features.controls.CcControlsActivity
import com.ezla.oslauncher.controlcenter.features.onboarding.CcOnboardingActivity
import com.ezla.oslauncher.controlcenter.features.permission.CcPermissionsActivity
import com.ezla.oslauncher.controlcenter.features.position.CcPositionActivity
import com.ezla.oslauncher.controlcenter.system.CcActions
import com.ezla.oslauncher.controlcenter.system.CcPermissions
import com.truongnt.ios.ioslite.common.IosLocale
import java.util.Locale

/** Màn chính Control Center (Figma "Main"). */
class CcMainActivity : BaseActivity<CcActivityMainBinding>() {

    override val setViewBinding get() = CcActivityMainBinding.inflate(layoutInflater)

    // Bật Enable khi chưa có Accessibility -> chờ user quay lại từ Settings rồi tự bật.
    private var pendingEnable = false

    // Mọi đường vào (Home, nút cài đặt trên panel) mở thẳng màn này: lần đầu chuyển sang onboarding.
    override fun redirectBeforeCreate(): Boolean {
        if (CcPrefs.isOnboardingDone(this)) return false
        startActivity(Intent(this, CcOnboardingActivity::class.java))
        return true
    }

    override fun initView() {
        binding.swBlur.isChecked = CcPrefs.isBlur(this)
        binding.swVibration.isChecked = CcPrefs.isVibration(this)
        binding.tvLanguage.text = languageName()
        showBannerOverlay(binding.frBanner, binding.mainLayout, false)
        CcInter.preload()
    }

    override fun viewListener() {
        binding.rowEnable.setOnClickListener { onEnableClicked() }
        binding.guideRow.setOnClickListener { onEnableClicked() }
        binding.guideOverlay.setOnClickListener { hideGuide() }
        binding.rowBlur.setOnClickListener {
            val v = !binding.swBlur.isChecked
            binding.swBlur.isChecked = v
            CcPrefs.setBlur(this, v)
        }
        binding.rowVibration.setOnClickListener {
            val v = !binding.swVibration.isChecked
            binding.swVibration.isChecked = v
            CcPrefs.setVibration(this, v)
        }
        binding.rowPermissions.setOnClickListener { open(CcPermissionsActivity::class.java) }
        binding.rowTheme.setOnClickListener { CcInter.run(this) { ControlCenterEntry.openThemes(this) } }
        binding.rowPosition.setOnClickListener { open(CcPositionActivity::class.java) }
        binding.rowControls.setOnClickListener { open(CcControlsActivity::class.java) }
        // Figma chưa có màn Choose Screen Record: tạm báo sắp có.
        binding.rowRecord.setOnClickListener {
            Toast.makeText(this, R.string.cc_coming_soon, Toast.LENGTH_SHORT).show()
        }
        binding.rowLanguage.setOnClickListener { ControlCenterEntry.openLanguage(this) }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        val a11y = CcPermissions.isAccessibilityEnabled(this)
        if (pendingEnable && a11y) CcPrefs.setEnabled(this, true)
        pendingEnable = false
        // Tắt Accessibility ở Settings thì coi như tắt tính năng (overlay không thể chạy).
        if (!a11y && CcPrefs.isEnabled(this)) CcPrefs.setEnabled(this, false)
        render()
    }

    private fun render() {
        val enabled = CcPrefs.isEnabled(this)
        binding.swEnable.isChecked = enabled
        binding.guideSwitch.isChecked = enabled
        binding.cardPermissions.visibility = if (CcPermissions.allGranted(this)) View.GONE else View.VISIBLE
        if (!enabled && !CcPrefs.isGuideShown(this)) showGuide() else hideGuide()
    }

    private fun onEnableClicked() {
        hideGuide()
        if (CcPrefs.isEnabled(this)) {
            CcPrefs.setEnabled(this, false)
            render()
            return
        }
        if (CcPermissions.isAccessibilityEnabled(this)) {
            CcPrefs.setEnabled(this, true)
            render()
            return
        }
        AccessibilityDisclosureDialog(this) {
            pendingEnable = true
            CcActions.start(this, CcPermissions.accessibilitySettings())
        }.show()
    }

    // Lớp hướng dẫn: đặt ô Enable giả đúng vị trí ô thật để trông như ô thật được tô sáng.
    private fun showGuide() {
        val overlay = binding.guideOverlay
        if (overlay.visibility == View.VISIBLE) return
        overlay.visibility = View.VISIBLE
        binding.cardEnable.doOnLayout {
            val loc = IntArray(2)
            val rootLoc = IntArray(2)
            binding.cardEnable.getLocationInWindow(loc)
            overlay.getLocationInWindow(rootLoc)
            binding.guideContent.translationY = (loc[1] - rootLoc[1]).toFloat()
        }
    }

    private fun hideGuide() {
        if (binding.guideOverlay.visibility != View.VISIBLE) return
        binding.guideOverlay.visibility = View.GONE
        CcPrefs.markGuideShown(this)
    }

    private fun open(target: Class<*>) {
        CcInter.run(this) { startActivity(Intent(this, target)) }
    }

    private fun languageName(): String {
        val code = IosLocale.getSavedLanguage(this).orEmpty()
        val locale = if (code.isEmpty()) resources.configuration.locales[0] else Locale(code)
        return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
    }
}
