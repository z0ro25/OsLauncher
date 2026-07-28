package com.amz.ios.themeclub.tools;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.widget.Button;

import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.launcher.LauncherRouter;
import com.amz.ios.ioslite.common.launcher.LauncherSettingSubject;
import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.ioslite.common.util.DisplayUtil;
import com.amz.ios.ioslite.common.util.ToastUtil;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.intertfaces.IProgressView;
import com.amz.ios.themeclub.util.WallpaperUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by gaol on 16-12-2.
 */

public class WallpaperSetTask extends AsyncTask<Object, Void, Boolean> {
    private final static String TAG = "WallpaperSetTask";

    Activity mActivity;
    IProgressView mBaseView;
    WallpaperManager mWallpaperManager;
    Bitmap mBitmap;
    Button setBtn;
    String mPath = null;
    int whichWallpaper ;
    int mWhickType;



    public WallpaperSetTask(Activity activity, IProgressView baseView, Bitmap bitmap, String path, int type,int whichType) {
        mActivity = activity;
        mBaseView = baseView;
        mWallpaperManager = WallpaperManager.getInstance(activity);
        mPath = path;
        whichWallpaper  = type;
        mBitmap = bitmap;
        mWhickType = whichType;
    }


    @Override
    protected Boolean doInBackground(Object[] params) {
        if(isCancelled()) {
            return false;
        }
        boolean setWallpaperSuccessful = false;
        boolean shouldNotifyLauncher = false;
        if(WallpaperUtil.ATLEAST_N) {
            try {
                InputStream is = getInputStream();
                setStreamForN(mActivity, is, null, true, whichWallpaper);
                int wallPaperScroolEnable = AppConfig.isWallPaperScroolEnable(mActivity);
                if (wallPaperScroolEnable != mWhickType) {
                    shouldNotifyLauncher = true;
                }
                if (mWhickType == 1) {
                    IOSSettings.putBoolean(mActivity.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WALLPAPER_SCROLL_ENABLE, false);
                } else if (mWhickType == 2) {
                    IOSSettings.putBoolean(mActivity.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WALLPAPER_SCROLL_ENABLE, true);
                }
                if (shouldNotifyLauncher){
                    LauncherSettingSubject.handleLauncherSettingChanged(IOSSettings.Launcher.LAUNCHER_WALLPAPER_SCROLL_ENABLE);
                }
                setWallpaperSuccessful = true;
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            setWallpaperSuccessful = applyLauncher();
        }
        return setWallpaperSuccessful;
    }

    @Override
    protected void onPostExecute(Boolean setWallpaperSuccessful) {
        super.onPostExecute(setWallpaperSuccessful);
        LauncherRouter.launch(mActivity);
        mBaseView.closeProgress();
        mActivity.finish();
        if (setWallpaperSuccessful) {
//            ToastUtil.show(mActivity,mActivity.getString(R.string.themeclub_set_wallpaper_succeed));
            LauncherAppState.setApplicationContext(mActivity.getApplicationContext());
            LauncherAppState appState = LauncherAppState.getInstance();
            appState.onWallpaperChanged();

        }else {
            ToastUtil.show(mActivity,mActivity.getString(R.string.themeclub_set_wallpaper_failure));
        }
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mBaseView.showProgress();
        if (setBtn != null) {
            setBtn.setClickable(false);
        }
    }

    private boolean applyLauncher() {
        if (mPath != null) {
            try{
                if(mPath.startsWith("file:///android_asset/wallpapers/")){
                    String[] filename = mPath.split("/");
                    AssetManager assetManager = mActivity.getAssets();
                    InputStream is = null;
                    try {
                        is = assetManager.open("wallpapers/"+filename[5]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(is != null){
                        mWallpaperManager.setStream(is);
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    File file = new File(mPath);
                    InputStream fis = null;
                    fis = new FileInputStream(file);
                    mWallpaperManager.setStream(fis);
                    try {
                        fis.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }else {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                InputStream inputimage = new ByteArrayInputStream(baos.toByteArray());
                final BufferedInputStream inputImage = new BufferedInputStream(inputimage);
                if (mWhickType == 1) {
                    IOSSettings.putBoolean(mActivity.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WALLPAPER_SCROLL_ENABLE, false);
                } else if (mWhickType == 2) {
                    IOSSettings.putBoolean(mActivity.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WALLPAPER_SCROLL_ENABLE, true);
                }
                mWallpaperManager.suggestDesiredDimensions(mBitmap.getWidth(),mBitmap.getHeight());
                if (BuildUtil.ATLEAST_NOUGAT) {
                    mWallpaperManager.setStream(inputImage, null, true, WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM);
                } else {
                    mWallpaperManager.setStream(inputImage);
                }

                baos.close();
                inputImage.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void setStreamForN(Context context, final InputStream data, Rect visibleCropHint, boolean allowBackup, int whichWallpaper) throws IOException {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        DebugLog.i(TAG, "============line(157) +# setStreamForN:" + whichWallpaper);
        wallpaperManager.setStream(data, visibleCropHint, allowBackup, whichWallpaper);
    }


    private InputStream getInputStream() {
        if (mPath != null) {
            try{
                if(mPath.startsWith("file:///android_asset/wallpapers/")){
                    String[] filename = mPath.split("/");
                    AssetManager assetManager = mActivity.getAssets();
                    InputStream is = null;
                    try {
                        is = assetManager.open("wallpapers/"+filename[5]);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        return handleBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }else {
                    File file = new File(mPath);
                    InputStream fis = new FileInputStream(file);
                    Bitmap bitmap = BitmapFactory.decodeStream(fis);
                    fis.close();
                    return handleBitmap(bitmap);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            InputStream inputimage = handleBitmap(mBitmap);
            return inputimage;
        }
        return null;
    }

    @NonNull
    private InputStream handleBitmap(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (mWhickType == 1) {
            bitmap = imageCropWithRect(bitmap);
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    public static InputStream getInputStream(Context context,String mPath,Bitmap mBitmap) {
        if (mPath != null) {
            try{
                if(mPath.startsWith("file:///android_asset/wallpapers/")){
                    String[] filename = mPath.split("/");
                    AssetManager assetManager = context.getAssets();
                    InputStream is = null;
                    try {
                        is = assetManager.open("wallpapers/"+filename[5]);
                        return is;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }else {
                    File file = new File(mPath);
                    InputStream fis = null;
                    fis = new FileInputStream(file);
                    return fis;
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            InputStream inputimage = new ByteArrayInputStream(baos.toByteArray());
            return inputimage;
        }
        return null;
    }

    public Bitmap imageCropWithRect(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int w = bitmap.getWidth(); // 得到图片的宽，高
        int h = bitmap.getHeight();
        int nw, nh, retX, retY;
        if (w > h) {
            nw = h / (DisplayUtil.getScreenHeightInPx(mActivity) / DisplayUtil.getScreenWidthInPx(mActivity));
            nh = h;
            retX = (w - nw) / 2;
            retY = 0;
        }else {
            return bitmap;
        }
        // 下面这句是关键
        Bitmap bmp = Bitmap.createBitmap(bitmap, retX, retY, nw, nh, null,
                false);
        if (bitmap != null && !bitmap.equals(bmp) && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        return bmp;// Bitmap.createBitmap(bitmap, retX, retY, nw, nh, null,
        // false);
    }
}
