package com.amz.ios.launcher.searchview;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Lưu LỊCH SỬ các app người dùng vừa mở TỪ MÀN SEARCH (bấm 1 app trong lưới gợi ý/kết quả/lịch sử).
 *
 * <p>Vì sao là lớp RIÊNG (không dùng {@code com.amz.ios.search.database.SearchHistoryProvider} của
 * module iOSSearch): provider đó là stub rỗng và thuộc module khác; ở đây chỉ cần lưu nhẹ danh sách
 * componentName vào {@link SharedPreferences} là đủ, không kéo theo phụ thuộc DB/module ngoài.
 *
 * <p>Bất biến: phần tử MỚI NHẤT đứng ĐẦU; khử trùng (mở lại app cũ -> nhảy lên đầu); giới hạn
 * {@link #MAX} phần tử.
 */
public final class SearchHistoryStore {

    private static final String PREFS = "search_history_prefs";
    private static final String KEY = "opened_from_search";
    private static final String SEP = "\n";
    /** Số app tối đa giữ trong lịch sử. */
    public static final int MAX = 8;

    private SearchHistoryStore() {
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Đưa 1 app lên ĐẦU lịch sử (khử trùng, cắt về {@link #MAX}). */
    public static void push(Context ctx, ComponentName cn) {
        if (ctx == null || cn == null) return;
        String flat = cn.flattenToString();
        List<String> list = loadRaw(ctx);
        list.remove(flat);            // bỏ bản cũ nếu có -> tránh trùng lặp
        list.add(0, flat);            // mới nhất lên đầu
        while (list.size() > MAX) {
            list.remove(list.size() - 1); // cắt đuôi cho đủ MAX
        }
        prefs(ctx).edit().putString(KEY, TextUtils.join(SEP, list)).apply();
    }

    /** Danh sách componentName theo thứ tự mới nhất trước (có thể rỗng). */
    public static List<ComponentName> load(Context ctx) {
        List<ComponentName> result = new ArrayList<>();
        for (String flat : loadRaw(ctx)) {
            ComponentName cn = ComponentName.unflattenFromString(flat);
            if (cn != null) result.add(cn);
        }
        return result;
    }

    /** Xoá toàn bộ lịch sử. */
    public static void clear(Context ctx) {
        if (ctx == null) return;
        prefs(ctx).edit().remove(KEY).apply();
    }

    private static List<String> loadRaw(Context ctx) {
        List<String> list = new ArrayList<>();
        if (ctx == null) return list;
        String raw = prefs(ctx).getString(KEY, "");
        if (!TextUtils.isEmpty(raw)) {
            for (String s : raw.split(SEP)) {
                if (!TextUtils.isEmpty(s)) list.add(s);
            }
        }
        return list;
    }
}
