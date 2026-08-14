package com.oslauncher.applauncher.themelauncher.Features.general

import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.activity.addCallback
import com.amz.ios.launcher.config.Settings
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.databinding.ActivityGeneralBinding
import com.oslauncher.applauncher.themelauncher.extensions.hideNavigation
import com.oslauncher.applauncher.themelauncher.extensions.showNav
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils
import com.oslauncher.applauncher.themelauncher.utils.Constant
import kotlin.math.roundToInt

class GeneralActivity : BaseActivity<ActivityGeneralBinding>() {
    override val setViewBinding: ActivityGeneralBinding
        get() = ActivityGeneralBinding.inflate(layoutInflater)

    // Seekbar kích cỡ: giữa (50) = cân đối; trái = logo nhỏ/chữ to, phải = logo to/chữ nhỏ.
    // t=(p-50)/50 → iconScale∈[0.85,1.15], textScale∈[0.70,1.30] (nghịch với icon).
    private fun iconScaleOf(progress: Int): Float = 1f + 0.15f * ((progress - 50) / 50f)
    private fun textScaleOf(progress: Int): Float = 1f - 0.30f * ((progress - 50) / 50f)

    override fun initView() {
        onBackPressedDispatcher.addCallback {
            finish()
        }

        // Hide Navigation (nhóm Options) — chức năng cũ.
        binding.swHideNav.isChecked = SharePrefUtils.getBoolean(this, Constant.IS_HIDE_NAV, true)

        // Auto Arrange: engine đọc pref khi thả icon (không cần reload).
        binding.swAutoArrange.isChecked = Settings.isDesktopAlignEnable(this)

        // Blur: chỉ hiện khi máy hỗ trợ cross-window blur; không thì ẩn cả row + divider.
        if (isBlurSupported()) {
            binding.swBlur.isChecked = Settings.isDesktopBlurEnable(this)
        } else {
            binding.llBlurRow.visibility = View.GONE
            binding.dividerBlur.visibility = View.GONE
        }

        // Hide Apps name (ON = ẩn tên app ở desktop + App Library). Ẩn luôn nhãn ở preview theo.
        binding.swSizeHideNav.isChecked = Settings.isHideAppLabel(this)
        applyPreviewLabelVisibility(binding.swSizeHideNav.isChecked)

        // Seekbar kích cỡ: preselect từ tỉ lệ icon đã lưu, đồng thời set preview ban đầu.
        val iconScale = Settings.getWorkspaceIconSizeScale(this)
        val progress = (50f + ((iconScale - 1f) / 0.15f) * 50f).toInt().coerceIn(0, 100)
        binding.sbAppSize.progress = progress
        applySizePreview(progress)
    }

    override fun viewListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.swHideNav.setOnCheckedChangeListener { _, isChecked ->
            SharePrefUtils.putBoolean(
                this,
                Constant.IS_HIDE_NAV,
                isChecked
            )

            if (isChecked) {
                window?.hideNavigation()
            } else window?.showNav()
        }

        binding.swAutoArrange.setOnCheckedChangeListener { _, isChecked ->
            Settings.setDesktopAlignEnable(this, isChecked)
        }

        binding.swBlur.setOnCheckedChangeListener { _, isChecked ->
            Settings.setDesktopBlurEnable(this, isChecked)
        }

        binding.swSizeHideNav.setOnCheckedChangeListener { _, isChecked ->
            Settings.setHideAppLabel(this, isChecked)
            applyPreviewLabelVisibility(isChecked)
        }

        binding.sbAppSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Live preview trực tiếp khi kéo.
                applySizePreview(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Ghi pref khi nhả tay để engine reload 1 lần (tránh giật khi kéo).
                val p = seekBar?.progress ?: return
                Settings.setWorkspaceIconSizeScale(this@GeneralActivity, iconScaleOf(p))
                Settings.setWorkspaceTextSizeScale(this@GeneralActivity, textScaleOf(p))
            }
        })
    }

    override fun dataObservable() {

    }

    /** Cập nhật preview: icon (base 64dp) và nhãn (base 12sp) theo tỉ lệ của seekbar. */
    private fun applySizePreview(progress: Int) {
        val density = resources.displayMetrics.density
        val sizePx = (64f * density * iconScaleOf(progress)).toInt()
        binding.ivSizePreview.layoutParams = binding.ivSizePreview.layoutParams.apply {
            width = sizePx
            height = sizePx
        }
        binding.tvSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f * textScaleOf(progress))
        // Nhãn "%" bên trái slider: hiển thị theo tỉ lệ icon (giữa = 100%, trái 85%, phải 115%).
        binding.tvSizePercent.text = "${(iconScaleOf(progress) * 100).roundToInt()}%"
    }

    /** Ẩn/hiện nhãn "Message" ở preview theo toggle Hide Apps name (ON = ẩn tên → ẩn nhãn). */
    private fun applyPreviewLabelVisibility(hide: Boolean) {
        binding.tvSizePreview.visibility = if (hide) View.INVISIBLE else View.VISIBLE
    }

    /** Máy có hỗ trợ blur cross-window không (API 31+ và bật ở hệ thống). */
    private fun isBlurSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        return wm.isCrossWindowBlurEnabled
    }
}
