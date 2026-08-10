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
import android.os.Bundle;

import com.amz.ios.launcher.Launcher;

public class SearchLauncher extends Launcher {

    private final SearchLauncherCallbacks mCallbacks;

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
        }
    }

    public SearchLauncherCallbacks getCallbacks() {
        return mCallbacks;
    }
}
