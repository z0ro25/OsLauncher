package com.oslauncher.applauncher.themelauncher.Features.general.screengrid

import android.view.View
import android.widget.TextView
import androidx.activity.addCallback
import com.amz.ios.launcher.LauncherAppState
import com.amz.ios.launcher.config.Settings
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.databinding.ActivityScreenGridBinding
import com.oslauncher.applauncher.themelauncher.extensions.tap

/**
 * Màn Screen Grid (dựng theo Figma light 115-1557 / dark 80-604).
 *
 * Segmented control 4x5 / 4x6 / 4x7 = số CỘT cố định 4, số HÀNG 5/6/7. Chọn 1 mức:
 * đổi pill + ảnh preview VÀ ghi [Settings.PREFER_DESKTOP_GRID] -> engine reload desktop theo grid mới
 * (engine tự reset sắp xếp icon về layout mặc định của grid đó khi reload).
 *
 * Mở màn: preselect theo grid ĐANG chạy thật (đọc InvariantDeviceProfile.numRows), không ghi pref.
 *
 * Lưu ý: id view/binding giữ nguyên llSeg4x4/4x5/4x6 để không phá binding, nhưng seg 0/1/2 nay
 * ánh xạ tới grid 5x4 (index 1) / 6x4 (index 4) / 7x4 (index 5) trong GridConfig.PRE_DESKTOP_GRID.
 */
class ScreenGridActivity : BaseActivity<ActivityScreenGridBinding>() {
    override val setViewBinding: ActivityScreenGridBinding
        get() = ActivityScreenGridBinding.inflate(layoutInflater)

    override fun initView() {
        onBackPressedDispatcher.addCallback {
            finish()
        }
        // Preselect theo grid đang active, KHÔNG ghi pref (chỉ phản ánh trạng thái hiện tại).
        setCurrentGrid(currentSegmentIndex(), persist = false)
    }

    override fun viewListener() {
        binding.apply {
            ivBack.tap {
                onBackPressedDispatcher.onBackPressed()
            }

            llSeg4x4.tap { setCurrentGrid(0) }
            llSeg4x5.tap { setCurrentGrid(1) }
            llSeg4x6.tap { setCurrentGrid(2) }
        }
    }

    override fun dataObservable() {

    }

    /**
     * Suy ra segment nên chọn khi mở màn.
     * Ưu tiên PREF user vừa chọn (nguồn sự thật; IDP có thể chưa dựng lại vì desktop đang nền):
     * pref index engine 1->5x4(seg0), 4->6x4(seg1), 5->7x4(seg2).
     * Chưa từng chọn -> đọc grid đang chạy thật: numRows 5/6/7 -> seg 0/1/2. Cuối cùng fallback seg0.
     */
    private fun currentSegmentIndex(): Int {
        if (Settings.hasDesktopGrid(this)) {
            return when (Settings.getDesktopGrid(this)) {
                4 -> 1
                5 -> 2
                else -> 0
            }
        }
        val rows = try {
            LauncherAppState.getInstance(this).invariantDeviceProfile?.numRows ?: 0
        } catch (e: Exception) {
            0
        }
        return when (rows) {
            6 -> 1
            7 -> 2
            else -> 0
        }
    }

    /**
     * Chọn biến thể grid: đổi pill + ảnh preview. Nếu [persist] true (user bấm) thì ghi
     * [Settings.PREFER_DESKTOP_GRID] (index engine: seg0->1=5x4, seg1->4=6x4, seg2->5=7x4) để engine
     * reload desktop.
     */
    private fun setCurrentGrid(index: Int, persist: Boolean = true) {
        if (persist) {
            val prefIndex = when (index) {
                1 -> 4
                2 -> 5
                else -> 1
            }
            Settings.setDesktopGrid(this, prefIndex)
        }
        binding.apply {
            unselectSeg(llSeg4x4, tvSeg4x4)
            unselectSeg(llSeg4x5, tvSeg4x5)
            unselectSeg(llSeg4x6, tvSeg4x6)

            // seg0 = 4x5, seg1 = 4x6, seg2 = 4x7 (preview khớp số HÀNG 5/6/7).
            when (index) {
                0 -> {
                    selectSeg(llSeg4x4, tvSeg4x4)
                    ivPreview.setImageResource(R.drawable.screengrid_preview_4x5)
                }

                1 -> {
                    selectSeg(llSeg4x5, tvSeg4x5)
                    ivPreview.setImageResource(R.drawable.screengrid_preview_4x6)
                }

                2 -> {
                    selectSeg(llSeg4x6, tvSeg4x6)
                    ivPreview.setImageResource(R.drawable.screengrid_preview_4x7)
                }
            }
        }
    }

    /** Nút đang chọn: pill trắng (screengrid_seg_selected) + chữ tối (sg_seg_text_selected). */
    private fun selectSeg(cell: View, tv: TextView) {
        cell.setBackgroundResource(R.drawable.screengrid_seg_selected)
        tv.setTextColor(getColorStateList(R.color.sg_seg_text_selected))
    }

    /** Nút thường: không nền, chữ theo token tiêu đề (đổi theo light/dark). */
    private fun unselectSeg(cell: View, tv: TextView) {
        cell.background = null
        tv.setTextColor(getColorStateList(R.color.onb_text_primary))
    }
}
