package com.oslauncher.applauncher.themelauncher.theme.pack

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Mô tả 1 GÓI THEME: gộp logo mặc định của một số app + hình nền vào cùng một đơn vị, để sau này
 * thêm tính năng đổi theme thì chỉ cần đổi/ thêm [LauncherTheme] mới, không phải sửa rải rác.
 *
 * Đây là MODEL chuẩn bị sẵn (structure only) — CHƯA nối cơ chế áp theme vào launcher engine.
 *
 * LƯU Ý nguồn logo: ảnh logo iOS nằm trong `module/iOSLauncher/res/drawable*` (không phải :app).
 * Res của module library được merge vào app khi build, nên [appLogos] tham chiếu theo TÊN resource
 * (String) rồi resolve qua [Context.getResources] + getIdentifier — KHÔNG dùng R.drawable của :app
 * (sẽ không thấy drawable của module).
 *
 * @property id            Khoá duy nhất, bền (lưu vào SharePref để nhớ theme đang chọn). VD "default".
 * @property nameRes       Tên hiển thị (strings.xml để đủ đa ngôn ngữ khi có UI đổi theme).
 * @property appLogos      Map: packageName -> TÊN drawable logo (vd "ic_app_facebook") của app đó.
 *                         Thiếu package -> app đó dùng icon hệ thống (không ép logo).
 * @property backgroundAsset Đường dẫn asset hình nền của theme (vd "wallpapers/wp1.png"), tính từ gốc
 *                         assets. null = theme không kèm nền (giữ nền hiện tại).
 */
data class LauncherTheme(
    val id: String,
    @StringRes val nameRes: Int,
    val appLogos: Map<String, String>,
    val backgroundAsset: String?
) {
    /**
     * @DrawableRes id logo cho [packageName] trong theme này (resolve tên resource qua res đã merge),
     * hoặc 0 nếu theme không định nghĩa / không tìm thấy drawable.
     */
    @DrawableRes
    fun logoFor(context: Context, packageName: String): Int {
        val name = appLogos[packageName] ?: return 0
        return context.resources.getIdentifier(name, "drawable", context.packageName)
    }
}
