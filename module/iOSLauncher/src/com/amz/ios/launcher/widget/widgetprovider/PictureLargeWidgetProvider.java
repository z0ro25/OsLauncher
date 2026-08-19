package com.amz.ios.launcher.widget.widgetprovider;

/**
 * Widget Photos LARGE (4x4). Kế thừa toàn bộ cơ chế nạp ảnh/quyền/overlay của
 * {@link PictureAppWidgetProvider} (hàm static {@code bindInflatedView} tìm view theo id nên
 * dùng chung được cho mọi layout). Chỉ khác span; span thực tế vẫn lấy từ minWidth/minHeight
 * trong res/xml/picture_large_widget_provider_info.xml, các getter dưới đây chỉ mang tính
 * danh nghĩa cho đồng bộ với gia đình widget.
 */
public class PictureLargeWidgetProvider extends PictureAppWidgetProvider {

    @Override
    public int getSpanX() {
        return 4;
    }

    @Override
    public int getSpanY() {
        return 4;
    }

    @Override
    public int getMinSpanX() {
        return 4;
    }

    @Override
    public int getMinSpanY() {
        return 4;
    }
}
