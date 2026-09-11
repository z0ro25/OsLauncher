/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.truongnt.ios.launcher.searchlauncher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import com.truongnt.ios.launcher.Launcher;

public class SearchLauncher extends Launcher {

    private final SearchLauncherCallbacks mCallbacks;

    // Cờ 1-lần: user bấm "Go to launcher" ở màn Home khi CHƯA default -> sau khi desktop hiển thị XONG
    // (finishBindingItems) mới nhắc lại dialog "Set as default launcher". Đọc & xoá cờ persist ở onCreate.
    private boolean mPromptSetDefaultOnDesktop;

    /**
     * Desktop đã bind xong toàn bộ item chưa (finishBindingItems đã chạy).
     *
     * Dialog "Set as default" CHỈ được bung khi cờ này bật — nếu không nó nổi lên lúc màn hình còn
     * trống/đang loading, che mất quá trình desktop hiện ra.
     */
    private boolean mDesktopReady;

    public SearchLauncher() {
        mCallbacks = new SearchLauncherCallbacks(this);
        setLauncherCallbacks(mCallbacks);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Gọi super đầy đủ trước (init desktop hoàn chỉnh) rồi mới điều hướng, để onDestroy không NPE
        // do view chưa được tạo. Sau khi user chọn app làm default launcher, :app HomeActivity đặt cờ
        // hello_pending -> lần đầu desktop khởi động sẽ hiện màn Hello 1 lần rồi mới vào desktop. Clear
        // cờ ngay để chỉ hiện đúng 1 lần; Hello xong tự mở lại SearchLauncher -> lúc đó cờ đã tắt nên
        // vào desktop bình thường (không lặp). Dùng ComponentName (string) để không phụ thuộc ngược :app.
        super.onCreate(savedInstanceState);
        SharedPreferences pref = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        if (pref.getBoolean("hello_pending", false)) {
            pref.edit().putBoolean("hello_pending", false).commit();
            Intent helloIntent = new Intent();
            helloIntent.setComponent(new ComponentName(getPackageName(),
                    "com.ezla.oslauncher.Features.hello.HelloActivity"));
            startActivity(helloIntent);
            overridePendingTransition(0, 0); // không nháy desktop trước khi sang Hello
            finish();
            return;
        }
        // Không cần đọc cờ ở đây: onResume/finishBindingItems sẽ tự đọc cờ persist khi desktop
        // thật sự hiển thị. Đọc sớm ở onCreate từng là nguyên nhân mất lượt nhắc (instance đầu
        // chết mang theo cờ) — xem ghi chú ở clearPromptSetDefaultFlag().
    }

    /**
     * [FIX] "Vào launcher lần đầu nhưng KHÔNG thấy dialog Set default."
     *
     * SearchLauncher khai báo {@code launchMode="singleTask"}: nếu instance ĐÃ TỒN TẠI (thường
     * gặp vì nó là launcher, hệ thống giữ sẵn trong task), startActivity chỉ đưa nó lên trước —
     * onCreate KHÔNG chạy lại, nên cờ persist không bao giờ được đọc và dialog không hiện.
     * onNewIntent là callback được gọi THAY onCreate trong trường hợp đó.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Instance đã sống từ trước -> desktop bind xong từ lâu, finishBindingItems sẽ KHÔNG nổ
        // lại. Đánh dấu sẵn sàng rồi tự bung dialog thay vì chờ nó.
        mDesktopReady = true;
        showSetDefaultPromptWhenReady();
    }

    /**
     * CHỐT CHẶN: desktop lên foreground thì thử bung dialog.
     *
     * Cần vì các mốc kia đều có đường thoát: onCreate không chạy khi instance đã tồn tại
     * (singleTask), onNewIntent chỉ nổ khi có intent mới, finishBindingItems có thể bị
     * waitUntilResume() hoãn lại rồi gọi vào bản của lớp CHA (Launcher.finishBindingItems) chứ
     * không phải bản override này.
     *
     * Lần resume ĐẦU TIÊN thường chạy TRƯỚC khi bind xong nên lời gọi này bị hoãn (cờ
     * mDesktopReady còn false) — đúng ý đồ: dialog chỉ hiện khi desktop đã loading xong.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // showSetDefaultPromptWhenReady() tự đọc cờ persist + tự kiểm tra desktop đã sẵn sàng chưa.
        showSetDefaultPromptWhenReady();
    }

    /**
     * Xoá cờ persist — gọi khi ĐÃ mở được dialog, để lần vào desktop sau không hiện lại.
     *
     * [FIX] "Dialog Set default không bao giờ hiện."
     *   Đo bằng log trên máy thật: SearchLauncher.onCreate chạy HAI LẦN cách nhau ~0,36s —
     *   instance đầu bị huỷ ngay sau khi tạo. Bản cũ xoá cờ NGAY LÚC ĐỌC ở onCreate, nên instance
     *   đầu "tiêu thụ" mất cờ rồi chết (kéo theo cờ bộ nhớ), còn instance thứ hai — cái thật sự
     *   sống và vẽ desktop — đọc ra false và không bung dialog.
     *
     *   Nay cờ chỉ bị xoá SAU KHI đã thực sự mở dialog, nên instance nào sống tới lúc hiển thị
     *   cũng dùng được.
     */
    private void clearPromptSetDefaultFlag() {
        getSharedPreferences(getPackageName(), MODE_PRIVATE)
                .edit().putBoolean("prompt_set_default_on_desktop", false).commit();
    }

    /**
     * Độ trễ (ms) từ lúc bind xong tới lúc bung dialog. Chừa thời gian cho icon/wallpaper vẽ xong
     * để người dùng thấy desktop hoàn chỉnh trước. Chỉnh số này nếu thấy dialog vẫn sớm/muộn quá.
     */
    private static final long PROMPT_DELAY_AFTER_BIND_MS = 600L;

    /** Bung dialog nhắc đặt default sau frame vẽ kế tiếp (để user thấy desktop trước). */
    private void showSetDefaultPromptWhenReady() {
        // Đọc THẲNG cờ persist, không tin cờ bộ nhớ: instance trước có thể đã chết mang theo cờ đó
        // (onCreate chạy 2 lần — xem ghi chú ở clearPromptSetDefaultFlag).
        SharedPreferences pref = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        boolean raised = pref.getBoolean("prompt_set_default_on_desktop", false);
        if (!raised) {
            return;
        }
        // CHƯA bind xong -> chờ. finishBindingItems() sẽ gọi lại hàm này khi desktop sẵn sàng,
        // nên không mất lượt nhắc; cờ persist vẫn còn nguyên (chỉ xoá sau khi mở được dialog).
        if (!mDesktopReady) {
            return;
        }
        if (isDefaultLauncher()) {
            // Đã là default -> không cần nhắc nữa, dọn cờ luôn.
            clearPromptSetDefaultFlag();
            mPromptSetDefaultOnDesktop = false;
            return;
        }
        // Chờ thêm một nhịp sau khi bind xong: bind chỉ nghĩa là DỮ LIỆU đã sẵn, còn icon/wallpaper
        // vẫn đang vẽ ra vài frame đầu. Nổi dialog ngay lúc đó vẫn thấy màn hình "loading dở".
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent promptIntent = new Intent();
                promptIntent.setComponent(new ComponentName(getPackageName(),
                        "com.ezla.oslauncher.dialog.SetDefaultLauncherPromptActivity"));
                try {
                    startActivity(promptIntent);
                    overridePendingTransition(0, 0);
                    // Mở được rồi mới hạ cờ -> instance bị huỷ giữa chừng không làm mất lượt nhắc.
                    clearPromptSetDefaultFlag();
                    mPromptSetDefaultOnDesktop = false;
                } catch (Throwable t) {
                }
            }
        }, PROMPT_DELAY_AFTER_BIND_MS);
    }

    @Override
    public void finishBindingItems() {
        super.finishBindingItems();
        // Desktop đã bind xong toàn bộ item -> giao diện đã sẵn sàng. Mở cổng cho dialog Set
        // default (trước mốc này mọi lời gọi showSetDefaultPromptWhenReady đều bị hoãn, để dialog
        // không nổi lên lúc màn hình còn đang loading).
        mDesktopReady = true;
        showSetDefaultPromptWhenReady();
    }

    /**
     * App hiện có đang là launcher mặc định không. Resolve HOME intent rồi so package -> đúng ở MỌI API
     * (kể cả < Q, nơi RoleManager không tồn tại nên cách cũ luôn trả false dù app ĐÃ là default).
     */
    private boolean isDefaultLauncher() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo res = getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return res != null && res.activityInfo != null
                && getPackageName().equals(res.activityInfo.packageName);
    }

    public SearchLauncherCallbacks getCallbacks() {
        return mCallbacks;
    }
}
