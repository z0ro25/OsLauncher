package com.amz.ios.themeclub.util;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.amz.ios.ioslite.common.launcher.LauncherRouter;
import com.amz.ios.ioslite.common.util.ToastUtil;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.ThemeClubApplication;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.bean.LockscreenInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * Created by ubuntu on 21/06/17.
 */

public class LockScreenUtils {
    public static final String PACKAGE_NAME = "com.oslauncher.applauncher.themelauncher";
    private static final int REQUEST_CODE_WRITE_SETTINGS = 1;
    public static String getLockscreenPackage(Context context) {
        if (context== null) {
            return PACKAGE_NAME;
        }
        String result = Settings.System.getString(context.getContentResolver(), AppConfig.LOCKSCREEN_PACKAGE);
        return TextUtils.isEmpty(result) ? PACKAGE_NAME : result;
    }

    public static boolean extractUXObject(Context context, String packageName, String assetName, String uxPath,
                                          String uxName) {

        ContentResolver resolver = context.getContentResolver();
        Settings.System.putString(resolver, AppConfig.KEY_THEME_LOCKSCREEN_FUN_UX_VALUE, "");

        initFunUXDir();

        String fileName = uxPath + File.separator + uxName;

        try {
            Context mPackageContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            InputStream is = mPackageContext.getResources().getAssets().open(assetName);
            FileOutputStream fos = new FileOutputStream(fileName);
            byte[] buffer = new byte[10240];
            int count = 0;
            while ((count = is.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
            }
            fos.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        chmodFileAccess(fileName);
        return true;
    }

    private static void initFunUXDir() {
        File mFunUXDir = null;
        try {
            if (mFunUXDir.exists()) {
                mFunUXDir.delete();
                Process p = Runtime.getRuntime().exec("rm -rf " + mFunUXDir.getAbsolutePath());
                p.waitFor();
            }

            mFunUXDir.mkdirs();
            chmodFileAccess(mFunUXDir.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final int S_IRWXU = 00700;
    public static final int S_IRWXG = 00070;
    public static final int S_IRWXO = 00007;

    private static void chmodFileAccess(String filePath) {
        setPermissions(filePath, S_IRWXU | S_IRWXG | S_IRWXO, -1, -1);
    }

    public static int setPermissions(String path, int mode, int uid, int gid) {
        try {
            String command = "chmod 777 " + path;
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(command);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    /**
     * @param
     * @return
     * @description apply LockScreen for phone
     */
    public static void applyLockScreen(Context context, LockscreenInfo lockscreenInfo) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            requestWriteSettings((Activity) context, lockscreenInfo);
        } else {
            String lockscreenPackage = lockscreenInfo.getPackageName();
            if (LockScreenUtils.getLockscreenPackage(context).equals(lockscreenPackage)) {
                Toast.makeText(context, context.getString(R.string.themeclub_lockscreen_has_bean_applied), Toast.LENGTH_SHORT).show();
                return;
            }
            Settings.System.putString(context.getContentResolver(), AppConfig.LOCKSCREEN_PACKAGE, lockscreenPackage);
            new ApplyLockscreenTask(context).execute(lockscreenInfo);
        }
    }

    private static File sFunUXDir;
    static class ApplyLockscreenTask extends AsyncTask<LockscreenInfo, String, String> {
        ProgressDialog dialog;
        String message;
        boolean uxLockscreen = false;
        Context mContext;

        public ApplyLockscreenTask(Context context) {
            mContext = context;
            message = context.getString(R.string.themeclub_lockscreen_apply);
            dialog = new ProgressDialog(context);
            dialog.setMessage(message + "...");
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        @SuppressWarnings("all")
        protected String doInBackground(LockscreenInfo... pramas) {
            LockscreenInfo lockscreenInfo = pramas[0];
            publishProgress(lockscreenInfo.getTitle());

            sFunUXDir = new File(ThemeClubApplication.getContext().getFilesDir(), AppConfig.FUN_UX_DIR);

            String uxPath = sFunUXDir.getAbsolutePath();
            String uxName = AppConfig.FUN_UX_DEFAULT_NAME;
            uxLockscreen = LockScreenUtils.extractUXObject(ThemeClubApplication.getContext(), lockscreenInfo.getPackageName(),
                    AppConfig.FUN_UX_ASSET_NAME, uxPath, uxName);

            Bitmap bitmap = lockscreenInfo.getLockscreenWallpaper(lockscreenInfo.getPackageName());

            if (bitmap != null) {
                try {
                    if (WallpaperUtil.ATLEAST_N) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        InputStream inputimage = new ByteArrayInputStream(baos.toByteArray());

                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                        wallpaperManager.setStream(inputimage, null, true, WallpaperManager.FLAG_LOCK);
                        baos.close();
                        inputimage.close();
                    } else {
                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(ThemeClubApplication.getContext());
                        Class classz = wallpaperManager.getClass();
                        Method setLockscreenBitmapMethod = classz.getDeclaredMethod("setLockscreenBitmap", Bitmap.class);
                        setLockscreenBitmapMethod.invoke(wallpaperManager, bitmap);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Intent intent = new Intent(AppConfig.ACTION_LOCKSCREEN_WALLPAPER_CHANGED);
                ThemeClubApplication.getContext().sendBroadcast(intent);
            }
            return null;
        }

        protected void onProgressUpdate(String... values) {
            dialog.setMessage(message + " " + values[0]);
            super.onProgressUpdate(values);
        }

        protected void onPostExecute(String result) {
            if (dialog != null) {
                dialog.dismiss();
            }
            LauncherRouter.launch(ThemeClubApplication.getContext());
            ToastUtil.show(ThemeClubApplication.getContext(),ThemeClubApplication.getContext().getString(R.string.themeclub_set_lockscreen_succeed));
        }
    }

    private static void requestWriteSettings(Activity activity, LockscreenInfo lockscreenInfo) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + lockscreenInfo.getPackageName()));
        activity.startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS);
    }
}
