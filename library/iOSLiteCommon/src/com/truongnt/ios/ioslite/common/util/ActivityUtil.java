package com.truongnt.ios.ioslite.common.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

// SDK quảng cáo đòi Activity, mà view trong RecyclerView/ContentProvider thường chỉ giữ
// ContextThemeWrapper — nên phải đi ngược chuỗi ContextWrapper để lấy Activity thật.
public final class ActivityUtil {

    private ActivityUtil() {
    }

    /** Activity chứa {@code context}, hoặc {@code null} nếu context không nằm trong Activity nào. */
    public static Activity findActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }
}
