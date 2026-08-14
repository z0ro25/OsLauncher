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

package com.amz.ios.launcher.searchlauncher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import com.amz.ios.launcher.Launcher;

public class SearchLauncher extends Launcher {

    private final SearchLauncherCallbacks mCallbacks;

    // Cờ 1-lần: user bấm "Go to launcher" ở màn Home khi CHƯA default -> sau khi desktop hiển thị XONG
    // (finishBindingItems) mới nhắc lại dialog "Set as default launcher". Đọc & xoá cờ persist ở onCreate.
    private boolean mPromptSetDefaultOnDesktop;

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
                    "com.oslauncher.applauncher.themelauncher.Features.hello.HelloActivity"));
            startActivity(helloIntent);
            overridePendingTransition(0, 0); // không nháy desktop trước khi sang Hello
            finish();
            return;
        }
        // Đọc & xoá cờ ngay (dùng-1-lần) để lần vào desktop sau (bấm Home) không hiện lại. Việc mở dialog
        // hoãn tới finishBindingItems() để desktop hiển thị XONG rồi mới bung dialog.
        if (pref.getBoolean("prompt_set_default_on_desktop", false)) {
            pref.edit().putBoolean("prompt_set_default_on_desktop", false).commit();
            mPromptSetDefaultOnDesktop = true;
        }
    }

    @Override
    public void finishBindingItems() {
        super.finishBindingItems();
        // Desktop đã bind xong toàn bộ item -> giao diện đã hiển thị. Giờ mới nhắc lại dialog Set default
        // (activity trong suốt của :app, mở qua ComponentName để không phụ thuộc ngược :app). post() để
        // chạy sau frame vẽ đầu tiên, đảm bảo user thấy desktop trước khi dialog nổi lên.
        if (mPromptSetDefaultOnDesktop) {
            mPromptSetDefaultOnDesktop = false;
            if (!isDefaultLauncher()) {
                getWindow().getDecorView().post(new Runnable() {
                    @Override
                    public void run() {
                        Intent promptIntent = new Intent();
                        promptIntent.setComponent(new ComponentName(getPackageName(),
                                "com.oslauncher.applauncher.themelauncher.dialog.SetDefaultLauncherPromptActivity"));
                        startActivity(promptIntent);
                        overridePendingTransition(0, 0);
                    }
                });
            }
        }
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
