package com.oslauncher.applauncher.themelauncher.Features.wallpaperonboarding

import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * PageTransformer cho carousel hình nền: trang giữa hiển thị nguyên kích thước,
 * 2 trang kề bị thu nhỏ (chủ yếu theo chiều cao) và mờ nhẹ để thò mép ra 2 bên — giống thiết kế.
 *
 * [minScale]: tỉ lệ nhỏ nhất của trang ở xa tâm (0..1).
 * [minAlpha]: độ mờ nhỏ nhất của trang ở xa tâm.
 */
class SelectBackgroundPageTransformer(
    private val minScale: Float = 0.82f,
    private val minAlpha: Float = 0.5f
) : ViewPager2.PageTransformer {

    override fun transformPage(page: android.view.View, position: Float) {
        // position: 0 = chính giữa, -1 = trang trái liền kề, 1 = trang phải liền kề.
        val distance = abs(position).coerceAtMost(1f)
        val scale = minScale + (1f - minScale) * (1f - distance)
        // Thu nhỏ cả 2 chiều để giữ tỉ lệ ảnh; neo scale về tâm ngang để 2 mép co đều.
        page.scaleX = scale
        page.scaleY = scale
        page.alpha = minAlpha + (1f - minAlpha) * (1f - distance)
    }
}
