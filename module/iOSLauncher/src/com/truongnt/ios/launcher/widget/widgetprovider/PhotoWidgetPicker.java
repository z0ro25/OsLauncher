package com.truongnt.ios.launcher.widget.widgetprovider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.truongnt.ios.launcher.Launcher;
import com.truongnt.ios.launcher.leftpage.widgets.PhotoWidget_2x2;

/**
 * Chọn ảnh cho widget ảnh bằng Photo Picker hệ thống — không cần quyền đọc thư viện ảnh.
 * Máy chưa có Photo Picker thì contract tự rơi về ACTION_OPEN_DOCUMENT, cũng không cần quyền.
 */
public final class PhotoWidgetPicker {

    public static final int REQUEST_PICK_PHOTO = 4212;
    // Widget qua RemoteViews chỉ chạy được PendingIntent -> mở Launcher kèm action này để Launcher mở picker.
    public static final String ACTION_PICK_PHOTO = "com.truongnt.ios.launcher.action.PICK_WIDGET_PHOTO";

    private PhotoWidgetPicker() {
    }

    public static void launch(Activity activity) {
        try {
            PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build();
            Intent intent = new ActivityResultContracts.PickVisualMedia()
                    .createIntent(activity, request);
            activity.startActivityForResult(intent, REQUEST_PICK_PHOTO);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static Intent buildLauncherPickIntent(Context context) {
        Intent intent = new Intent(context, Launcher.class);
        intent.setAction(ACTION_PICK_PHOTO);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /** Trả true nếu intent là yêu cầu chọn ảnh từ widget RemoteViews (đã mở picker). */
    public static boolean handleLauncherIntent(Activity activity, Intent intent) {
        if (intent == null || !ACTION_PICK_PHOTO.equals(intent.getAction())) return false;
        launch(activity);
        return true;
    }

    /** Lưu ảnh ở luồng nền rồi vẽ lại mọi widget ảnh trên UI thread. */
    public static void handleResult(final Activity activity, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) return;
        final Uri uri = data.getData();
        if (uri == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!PhotoWidgetStore.save(activity, uri)) return;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        PictureAppWidgetProvider.invalidatePhotoCache();
                        PictureAppWidgetProvider.refreshAll(activity);
                        reloadLeftPageWidgets(activity.getWindow().getDecorView());
                    }
                });
            }
        }, "photo-widget-save").start();
    }

    private static void reloadLeftPageWidgets(View view) {
        if (view instanceof PhotoWidget_2x2) {
            ((PhotoWidget_2x2) view).reloadPhoto();
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                reloadLeftPageWidgets(group.getChildAt(i));
            }
        }
    }
}
