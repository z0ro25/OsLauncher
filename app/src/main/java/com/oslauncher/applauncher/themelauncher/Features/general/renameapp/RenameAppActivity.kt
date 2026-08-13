package com.oslauncher.applauncher.themelauncher.Features.general.renameapp

import android.graphics.drawable.Drawable
import android.view.inputmethod.EditorInfo
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.general.changeicon.data.ChangeAppIconRepository
import com.oslauncher.applauncher.themelauncher.Features.general.renameapp.data.RenameAppRepository
import com.oslauncher.applauncher.themelauncher.databinding.ActivityRenameAppBinding
import com.oslauncher.applauncher.themelauncher.extensions.hideKeyboard
import com.oslauncher.applauncher.themelauncher.extensions.tap

/**
 * Màn đổi tên chi tiết: 1 ô nhập tên (icon + EditText) + nút xanh "Reset name to default".
 *
 * Cơ chế lưu (đã chốt với user): LƯU khi bấm Done bàn phím + khi THOÁT màn (onPause) — không có nút
 * Save riêng (khớp Figma). Reset -> đưa tên về nhãn gốc + cập nhật ô nhập ngay.
 * Chỉ lưu khi tên khác tên hiện đang áp dụng và không rỗng (tránh broadcast thừa).
 */
class RenameAppActivity : BaseActivity<ActivityRenameAppBinding>() {

    override val setViewBinding: ActivityRenameAppBinding
        get() = ActivityRenameAppBinding.inflate(layoutInflater)

    private val repo by lazy { RenameAppRepository(this) }
    // Tái dùng nguồn icon custom CÓ SẴN (đọc file custom trong filesDir) — khớp màn list + desktop.
    private val iconRepo by lazy { ChangeAppIconRepository(this) }

    private var component: String = ""
    private var packageName: String = ""
    private var defaultLabel: String = ""

    override fun initView() {
        component = intent.getStringExtra(EXTRA_COMPONENT).orEmpty()
        packageName = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        defaultLabel = intent.getStringExtra(EXTRA_DEFAULT_LABEL).orEmpty()

        val currentLabel = repo.getCustomLabel(component) ?: defaultLabel
        binding.tvTitle.text = currentLabel
        binding.etAppName.setText(currentLabel)
        binding.etAppName.setSelection(binding.etAppName.text?.length ?: 0)

        // Icon custom (đã đặt ở màn đổi icon) nếu có -> khớp màn list + desktop; ngược lại icon gốc hệ thống.
        val customIcon = iconRepo.customIconFile(component)?.let { Drawable.createFromPath(it.absolutePath) }
        if (customIcon != null) {
            binding.ivAppIcon.setImageDrawable(customIcon)
        } else {
            try {
                binding.ivAppIcon.setImageDrawable(packageManager.getApplicationIcon(packageName))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun viewListener() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Done bàn phím -> lưu + ẩn bàn phím.
        binding.etAppName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commitName()
                binding.etAppName.hideKeyboard()
                true
            } else false
        }

        binding.btnReset.tap {
            repo.resetName(component, defaultLabel)
            binding.etAppName.setText(defaultLabel)
            binding.etAppName.setSelection(defaultLabel.length)
            binding.tvTitle.text = defaultLabel
        }
    }

    override fun dataObservable() {}

    override fun onPause() {
        // Thoát màn (back / chuyển màn) -> lưu tên đang gõ.
        commitName()
        super.onPause()
    }

    /** Lưu tên trong ô nhập nếu hợp lệ và khác tên đang áp dụng. */
    private fun commitName() {
        val newName = binding.etAppName.text?.toString()?.trim().orEmpty()
        if (newName.isEmpty()) return
        val current = repo.getCustomLabel(component) ?: defaultLabel
        if (newName == current) return
        repo.saveName(component, newName)
        binding.tvTitle.text = newName
    }

    companion object {
        const val EXTRA_COMPONENT = "extra_component"
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_DEFAULT_LABEL = "extra_default_label"
    }
}
