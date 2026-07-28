package com.amz.ios.themeclub.presenter;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.ThemeClubApplication;
import com.amz.ios.themeclub.bean.WallPapersBean;
import com.amz.ios.themeclub.intertfaces.IProgressView;
import com.amz.ios.themeclub.tools.ThreadManager;
import com.amz.ios.themeclub.tools.WallpaperSetTask;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.CustomUtils;
import com.amz.ios.themeclub.util.NetWorkUtils;
import com.amz.ios.themeclub.util.WallpaperUtil;

import java.io.File;


/**
 * Created by lideqian on 17-3-28.
 */

public class WallpaperPresenter {

    private static String FILE_PATH = "/themes/download/";
    private String mWallpaperPath;
    private Context mContext;
    private WallPapersBean mWallpaper;
    private GetImageCacheTask mGetImageCacheTask;
    private AsyncTask mSetWallpaperTask;
    private PopupWindow applyPopUpWindow;
    //标志滚动壁纸还是固定壁纸
    private int mWhichType = 1;

    public WallpaperPresenter(Context mContext) {
        this.mContext = mContext;
    }

    public WallpaperPresenter(Context mContext, int mWhichType) {
        this.mContext = mContext;
        this.mWhichType = mWhichType;
    }

    public WallpaperPresenter(Context context, WallPapersBean wallPapersBean, int whichType) {
        this.mContext = context;
        this.mWallpaper = wallPapersBean;
        this.mWhichType = whichType;
        initConfig();
    }

    private void initConfig() {
        if(mWallpaper != null) {
            mWallpaperPath = AppUtils.getSDPath() + FILE_PATH + mWallpaper.getName() + mWallpaper.getId() + ".jpg";
        }
     }

    public void applyWallpaper(Activity activity,IProgressView progressView, Bitmap bitmap, String path, View v, int nSelectedBitmapId) {
        if (mWallpaperPath != null) {
            File file = new File(mWallpaperPath);
            if (!file.exists()) {
                downloadWallpaper(bitmap, true);
            }
        }
        if(WallpaperUtil.ATLEAST_N) {
            showPopWindow(activity, progressView, bitmap, path, v, nSelectedBitmapId);
        }else{
            mSetWallpaperTask = new WallpaperSetTask(activity,progressView,bitmap, path,WallpaperManager.FLAG_SYSTEM, mWhichType);
            mSetWallpaperTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void showPopWindow(final Activity activity, final IProgressView progress, final Bitmap bitmap, final String path, View v, final int nSelectedBitmapId) {

        if(applyPopUpWindow != null && applyPopUpWindow.isShowing()){
            applyPopUpWindow.dismiss();
            return;
        }
        View applyView  = LayoutInflater.from(mContext).inflate(R.layout.themeclub_wallpaper_apply,null);
        applyPopUpWindow = new PopupWindow(applyView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT,true);
        applyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (applyPopUpWindow!=null){
                applyPopUpWindow.dismiss();
            }
            }
        });
        applyView.findViewById(R.id.apply_launch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPopUpWindow.dismiss();
                mSetWallpaperTask = new WallpaperSetTask(activity,progress,bitmap,path,WallpaperManager.FLAG_SYSTEM,mWhichType);
                runTask(mSetWallpaperTask);
                IOSSettings.setWallpaperId(activity, nSelectedBitmapId);
            }
        });
        applyView.findViewById(R.id.apply_lockscreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPopUpWindow.dismiss();
                mSetWallpaperTask = new WallpaperSetTask(activity,progress,bitmap,path,WallpaperManager.FLAG_LOCK,mWhichType);
                runTask(mSetWallpaperTask);
            }
        });
        applyView.findViewById(R.id.apply_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPopUpWindow.dismiss();
                mSetWallpaperTask = new WallpaperSetTask(activity,progress,bitmap,path,WallpaperManager.FLAG_LOCK|WallpaperManager.FLAG_SYSTEM,mWhichType);
                runTask(mSetWallpaperTask);
                IOSSettings.setWallpaperId(activity, nSelectedBitmapId);
            }
        });
        applyPopUpWindow.showAtLocation(v,Gravity.CENTER,0,0);
    }

    public void downloadWallpaper(final Bitmap bitmap, boolean setWallPaper) {
        File file = new File(mWallpaperPath);
        if (!file.exists()) {

            NetWorkUtils.getInstance().getDataFromServer(NetWorkUtils.postDownloadCountsFactory(mWallpaper.getId(), 2, ThemeClubApplication.getContext()), new Response.Listener() {
                @Override
                public void onResponse(Object response) {

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });
            if (bitmap == null) {
                mGetImageCacheTask = new GetImageCacheTask();
                mGetImageCacheTask.execute();
            } else {
                ThreadManager.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (WallpaperUtil.saveImageToGallery(mContext, bitmap, mWallpaper.getName() + mWallpaper.getId() + ".jpg")) {
                            Intent downloadSucess = new Intent(WallpaperUtil.WALLPAPER_NEED_UPDATE);
                            mContext.sendBroadcast(downloadSucess);
                        }
                    }
                });
            }
        }
        if (!setWallPaper) {
            CustomUtils.shortToast(mContext.getString(R.string.themeclub_download_sucess));
        }
    }

    public String getWallpaperPath() {
        return  mWallpaperPath;
    }


    private class GetImageCacheTask extends AsyncTask<String, Void, File> {

        @Override
        protected File doInBackground(String... params) {
            try {
                return Glide.with(mContext)
                        .load(mWallpaper.getBigImage().getDownloadUrl())
                        .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get();
            } catch (Exception ex) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(final File result) {
            if (result == null) {
                return;
            }
            ThreadManager.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    WallpaperUtil.copyPicture(result.getPath(),mWallpaperPath);
                }
            });
            Intent downloadSucess = new Intent(WallpaperUtil.WALLPAPER_NEED_UPDATE);
            mContext.sendBroadcast(downloadSucess);
        }
    }


    public void runTask(AsyncTask asyncTask) {
        if(asyncTask != null) {
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    public void onDestroy() {
        if(mGetImageCacheTask != null && mGetImageCacheTask.getStatus() == AsyncTask.Status.RUNNING) {
            mGetImageCacheTask.cancel(true);
            mGetImageCacheTask = null;
        }
        if(mSetWallpaperTask != null && mSetWallpaperTask.getStatus() == AsyncTask.Status.RUNNING) {
            mSetWallpaperTask.cancel(true);
            mSetWallpaperTask = null;
        }
    }

    public void onDestroyPopWindow() {
        if(applyPopUpWindow != null && applyPopUpWindow.isShowing()){
            applyPopUpWindow.dismiss();
        }
    }

}
