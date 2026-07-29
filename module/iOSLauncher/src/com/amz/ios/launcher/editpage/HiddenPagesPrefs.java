package com.amz.ios.launcher.editpage;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Lưu trạng thái "ẩn page" (kiểu iOS) một cách bền vững ngoài DB.
 *
 * Điểm mấu chốt: page ẩn KHÔNG bị xóa khỏi bảng workspaceScreens/mScreenOrder — nếu xóa,
 * các app gắn theo screenId sẽ thành mồ côi. Thay vào đó ta chỉ tháo CellLayout khỏi cây
 * view của Workspace để PagedView không cuộn tới; danh sách screenId ẩn được lưu ở đây.
 *
 * Dữ liệu = Set<String> các screenId (lưu String vì SharedPreferences chỉ hỗ trợ Set<String>).
 */
public final class HiddenPagesPrefs {

    private static final String PREFS_NAME = "edit_pages_prefs";
    private static final String KEY_HIDDEN = "hidden_screen_ids";

    private HiddenPagesPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Trả về tập screenId đang bị ẩn (dạng Long). Không bao giờ null. */
    public static Set<Long> getHidden(Context context) {
        Set<String> raw = prefs(context).getStringSet(KEY_HIDDEN, null);
        Set<Long> result = new HashSet<>();
        if (raw != null) {
            for (String s : raw) {
                try {
                    result.add(Long.parseLong(s));
                } catch (NumberFormatException ignored) {
                    // bỏ qua giá trị hỏng
                }
            }
        }
        return result;
    }

    /** Ghi đè toàn bộ tập screenId ẩn. */
    public static void setHidden(Context context, Set<Long> hidden) {
        Set<String> raw = new HashSet<>();
        if (hidden != null) {
            for (Long id : hidden) {
                if (id != null) {
                    raw.add(String.valueOf(id));
                }
            }
        }
        // Phải truyền một Set mới mỗi lần: SharedPreferences không tự sao chép và
        // giữ tham chiếu tới Set cũ đã trả về từ getStringSet -> đọc lại có thể sai.
        prefs(context).edit().putStringSet(KEY_HIDDEN, raw).apply();
    }

    public static boolean isHidden(Context context, long screenId) {
        return getHidden(context).contains(screenId);
    }
}
