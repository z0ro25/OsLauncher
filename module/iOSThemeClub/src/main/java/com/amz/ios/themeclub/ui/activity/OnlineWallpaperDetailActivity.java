package com.amz.ios.themeclub.ui.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Util;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.ioslite.common.ad.IOSAdConfig;
import com.amz.ios.ioslite.common.ad.NativeAdCardView;
import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.util.ToastUtil;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.ThemeClubApplication;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.bean.ThemesBean;
import com.amz.ios.themeclub.bean.WallPapersBean;
import com.amz.ios.themeclub.intertfaces.IProgressView;
import com.amz.ios.themeclub.presenter.WallpaperPresenter;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.NetWorkUtils;
import com.amz.ios.themeclub.util.ShareUtils;
import com.amz.ios.themeclub.util.WallpaperUtil;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;

public class OnlineWallpaperDetailActivity extends CommonAppCompatActivity implements View.OnClickListener {

    public static String TAG = "Wallpaper";
    private int CACHE_SIZE = 10;
    Toolbar mToolbar;
    ImageView iVimige;
    ImageView sourceIcon;
    ProgressBar progress;
    CustomTextView author;
    CustomTextView source;
    CustomTextView moreTheme;
    ImageView moreThemeImage;
    CustomTextView moreWallpaper;
    ImageView itemLeft;
    ImageView itemCenter;
    ImageView itemRight;
    Button downloadButton;
    Button setWallpapweButton;
    LinearLayout mGoToSource;
    LruCache<String, Bitmap> mMemoryCache;
    RelativeLayout mWallpaperProgress;
    CoordinatorLayout mCoordinatorLayout;
    ImageView iVImageBackground;
    private ArrayList<ThemesBean> themeBeans;
    private ArrayList<WallPapersBean> wallpapersBeans;
    Intent intent;
    WallPapersBean mWallpaper;
    private String filePath;
    private String downloadUrl;
    private ImageView resouceImage;
    private WallpaperPresenter mWallpaperPresenter;
    private RequestManager mGlide;
    private int mLoadCount;
    private IProgressView mIProgressView = new IProgressView() {
        @Override
        public void showProgress() {
            if (mWallpaperProgress != null) {
                mWallpaperProgress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void closeProgress() {
            if (mWallpaperProgress != null) {
                mWallpaperProgress.setVisibility(View.INVISIBLE);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsDelegate.onWallpaperEvent(this, UMEventConstants.WALLPAPER_PERWALLPAPER_CLICK);
        AppUtils.immersive(this);
        setContentView(R.layout.themeclub_online_wallpaper_detail);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.cooddinator);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationIcon(R.drawable.source_detail_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        CollapsingToolbarLayout mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        mCollapsingToolbarLayout.setTitle("");
        themeBeans = new ArrayList<>();
        wallpapersBeans = new ArrayList<>();
        mMemoryCache = new LruCache<>(CACHE_SIZE);
        initView();
        initConfigs();
    }

    private CustomTarget mSmallTarget = new CustomTarget<Bitmap>() {
        @Override
        public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
            iVimige.setClickable(false);
            iVimige.setImageBitmap(bitmap);
            iVImageBackground.setVisibility(View.GONE);
            mLoadCount = 0;
            loadBigImage();
        }

        @Override
        public void onLoadCleared(@Nullable Drawable drawable) {

        }

        @Override
        public void onLoadFailed(@Nullable Drawable errorDrawable) {
            super.onLoadFailed(errorDrawable);
            mLoadCount += 1;
            if (mLoadCount < 4) {
                mGlide.resumeRequests();
            } else {
                ToastUtil.show(OnlineWallpaperDetailActivity.this, R.string.themeclub_download_failure);
                mLoadCount = 0;
                loadBigImage();
            }
        }
    };
//            new SimpleTarget<Bitmap>() {
//        @Override
//        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
//            DebugLog.w(TAG, "=============SmallImage onResourceReady");
//            iVimige.setClickable(false);
//            iVimige.setImageBitmap(resource);
//            iVImageBackground.setVisibility(View.GONE);
//            mLoadCount = 0;
//            loadBigImage();
//        }
//
//        @Override
//        public void onLoadStarted(Drawable placeholder) {
//            super.onLoadStarted(placeholder);
//            DebugLog.w(TAG, "=====================SmallImage OnLoadStarted:");
//        }
//
//        @Override
//        public void onLoadFailed(Exception e, Drawable errorDrawable) {
//            super.onLoadFailed(e, errorDrawable);
//            DebugLog.w(TAG, "===============SmallImage onLoadFailed:" + e.toString());
//            mLoadCount += 1;
//            if (mLoadCount < 4) {
//                mGlide.resumeRequests();
//            } else {
//                ToastUtil.show(OnlineWallpaperDetailActivity.this, R.string.themeclub_download_failure);
//                mLoadCount = 0;
//                loadBigImage();
//            }
//
//        }
//    };

    private void loadData() {
        if (mWallpaper == null) {
            return;
        }
        mWallpaperPresenter = new WallpaperPresenter(this, mWallpaper, AppConfig.isWallPaperScroolEnable(this));
        filePath = mWallpaperPresenter.getWallpaperPath();
        downloadUrl = mWallpaper.getSourceLogoUrl();
        source.setText(mWallpaper.getSource());
        author.setText(mWallpaper.getAuthor());

        mGlide.asBitmap().load(mWallpaper.getSmallImage().getDownloadUrl()).into(mSmallTarget);
        Log.e(TAG, mWallpaper.getSmallImage().getDownloadUrl() + "::" + mWallpaper.getBigImage().getDownloadUrl());
    }

    private CustomTarget mBigTarget = new CustomTarget<Bitmap>() {
        @Override
        public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
            DebugLog.w(TAG, "=============BigImage onResourceReady");
            iVimige.setClickable(true);
            iVimige.setImageBitmap(bitmap);
            mMemoryCache.put(downloadUrl, bitmap);
            progress.setVisibility(View.INVISIBLE);
            downloadButton.setEnabled(true);
            setWallpapweButton.setEnabled(true);
            iVImageBackground.setVisibility(View.GONE);
            mLoadCount = 0;
            loadOtherDetails();
        }

        @Override
        public void onLoadCleared(@Nullable Drawable drawable) {

        }

        @Override
        public void onLoadFailed(@Nullable Drawable errorDrawable) {
            super.onLoadFailed(errorDrawable);

            DebugLog.w(TAG, "===============BigImage onLoadFailed:" + errorDrawable.toString());
            mLoadCount += 1;
            if (mLoadCount < 4) {
                mGlide.resumeRequests();
            } else {
                progress.setVisibility(View.INVISIBLE);
                ToastUtil.show(OnlineWallpaperDetailActivity.this, R.string.themeclub_download_failure);
                loadOtherDetails();
                mLoadCount = 0;
            }
        }
    };
//            new SimpleTarget<Bitmap>() {
//        @Override
//        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
//            DebugLog.w(TAG, "=============BigImage onResourceReady");
//            iVimige.setClickable(true);
//            iVimige.setImageBitmap(resource);
//            mMemoryCache.put(downloadUrl, resource);
//            progress.setVisibility(View.INVISIBLE);
//            downloadButton.setEnabled(true);
//            setWallpapweButton.setEnabled(true);
//            iVImageBackground.setVisibility(View.GONE);
//            mLoadCount = 0;
//            loadOtherDetails();
//        }
//
//        @Override
//        public void onLoadStarted(Drawable placeholder) {
//            super.onLoadStarted(placeholder);
//            DebugLog.w(TAG, "=====================BigImage OnLoadStarted:");
//        }
//
//        @Override
//        public void onLoadFailed(Exception e, Drawable errorDrawable) {
//            super.onLoadFailed(e, errorDrawable);
//            DebugLog.w(TAG, "===============BigImage onLoadFailed:" + e.toString());
//            mLoadCount += 1;
//            if (mLoadCount < 4) {
//                mGlide.resumeRequests();
//            } else {
//                progress.setVisibility(View.INVISIBLE);
//                ToastUtil.show(OnlineWallpaperDetailActivity.this, R.string.themeclub_download_failure);
//                loadOtherDetails();
//                mLoadCount = 0;
//            }
//
//        }
//    };

    private void loadBigImage() {
        mGlide.asBitmap().load(mWallpaper.getBigImage().getDownloadUrl()).into(mBigTarget);
    }

    private void loadOtherDetails() {
        if (mWallpaper.getSourceLogoUrl() != null) {
            mGlide.load(mWallpaper.getSourceLogoUrl()).into(sourceIcon);
        }
        mGlide.load(mWallpaper.getSourceHintUrl()).into(resouceImage);

        NetWorkUtils.getInstance().getDataFromServer(NetWorkUtils.CommonResourceFactory(mWallpaper.getId(), 2, mWallpaper.getSource(), ThemeClubApplication.getContext()), new Response.Listener() {
            @Override
            public void onResponse(Object response) {

                try {
                    themeBeans = AppUtils.getCommonTheme(response.toString());

                    wallpapersBeans = AppUtils.getCommonWallpapers(response.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    loadMoreResource();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.toString());
            }
        });
    }

    private void initView() {
        iVimige = (ImageView) findViewById(R.id.ivImage);
        sourceIcon = (ImageView) findViewById(R.id.sources_icon);
        author = (CustomTextView) findViewById(R.id.author);
        source = (CustomTextView) findViewById(R.id.source_text);
        moreTheme = (CustomTextView) findViewById(R.id.more_themes);
        moreThemeImage = (ImageView) findViewById(R.id.more_themes_item);
        moreWallpaper = (CustomTextView) findViewById(R.id.more_wallpaper);
        itemLeft = (ImageView) findViewById(R.id.item_left);
        itemCenter = (ImageView) findViewById(R.id.item_center);
        itemRight = (ImageView) findViewById(R.id.item_right);
        downloadButton = (Button) findViewById(R.id.download_wallpaper);
        setWallpapweButton = (Button) findViewById(R.id.set_wallpaper);
        mGoToSource = (LinearLayout) findViewById(R.id.source_1);
        progress = (ProgressBar) findViewById(R.id.wallpaper_progress_item);
        iVImageBackground = (ImageView) findViewById(R.id.ivImageBackaground);
        resouceImage = (ImageView) findViewById(R.id.resouce_image);
        mWallpaperProgress = (RelativeLayout) findViewById(R.id.progress_set);
        iVImageBackground.setAlpha((float) 0.35);
        initAdView();
        initListener();
    }

    private void initConfigs() {
        mGlide = Glide.with(this);
        intent = getIntent();
        if (intent != null) {
            mWallpaper = (WallPapersBean) intent.getSerializableExtra(WallpaperUtil.ONLINEWALLPAPERBEAN);
            loadData();
        }
    }

    private void initAdView() {
        NativeAdCardView adView = (NativeAdCardView) findViewById(R.id.adview);
        adView.setAdvertiseId(IOSAdConfig.ID_WALLPAPER_DETAIL);
        adView.loadAdvertise();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_wallpaper_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final String wallpaperPath = mWallpaperPresenter.getWallpaperPath();
        final File file = new File(wallpaperPath);
        if (file.exists()) {
            downloadButton.setText(getString(R.string.themeclub_wallpaper_has_download));
            downloadButton.setClickable(false);
        } else {
            downloadButton.setText(getString(R.string.themeclub_download_theme));
            downloadButton.setClickable(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.share_wallpaper) {
            AnalyticsDelegate.onWallpaperEvent(this, UMEventConstants.WALLPAPER_SHARE);
            File mFile = new File(filePath);
            if (mFile.exists()) {
                ShareUtils.SharePic(this, getTitle().toString(), filePath);
            } else {
                ShareUtils.ShareText(this, getString(R.string.themeclub_share_wallpaper));
            }
        }
        if (id == R.id.crop_wallpaper) {
            Intent cropIntent = new Intent(this, CropImageActivity.class);
            if (AppUtils.fileIsExists(filePath)) {
                Log.e(TAG, "filePath = " + filePath);
                cropIntent.putExtra("path", filePath);
                cropIntent.putExtra("isUriOrPath", false);
            } else if (downloadUrl != null) {
                Log.i(TAG, "URL = " + mWallpaper.getBigImage().getDownloadUrl());
                cropIntent.putExtra("bitmapurl", mWallpaper.getBigImage().getDownloadUrl());
            }
            startActivity(cropIntent);
        }

        return super.onOptionsItemSelected(item);
    }


    private void loadMoreResource() {
        if (Util.isOnMainThread()) {
            if (themeBeans == null || wallpapersBeans == null) {
                return;
            }
            if (wallpapersBeans.size() == 3) {
                mGlide.load(wallpapersBeans.get(0).getSmallImage().getDownloadUrl()).into(itemLeft);
                mGlide.load(wallpapersBeans.get(1).getSmallImage().getDownloadUrl()).into(itemCenter);
                mGlide.load(wallpapersBeans.get(2).getSmallImage().getDownloadUrl()).into(itemRight);
            }
            if (themeBeans.size() != 0) {
                mGlide.load(themeBeans.get(0).getPreview().getDownloadUrl()).into(moreThemeImage);
            }
        }
    }

    private void initListener() {
        iVimige.setOnClickListener(this);
        downloadButton.setOnClickListener(this);
        setWallpapweButton.setOnClickListener(this);
        moreTheme.setOnClickListener(this);
        moreThemeImage.setOnClickListener(this);
        moreWallpaper.setOnClickListener(this);
        itemLeft.setOnClickListener(this);
        itemCenter.setOnClickListener(this);
        itemRight.setOnClickListener(this);
        source.setOnClickListener(this);
        mGoToSource.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.download_wallpaper) {
            if (filePath == null) {
                return;
            }
            mWallpaperPresenter.downloadWallpaper(mMemoryCache.get(downloadUrl), false);
            downloadButton.setText(R.string.themeclub_wallpaper_has_download);
            downloadButton.setClickable(false);
        } else if (v.getId() == R.id.set_wallpaper) {
            mWallpaperPresenter.applyWallpaper(this,
                    mIProgressView,
                    mMemoryCache.get(downloadUrl),
                    null,
                    LayoutInflater.from(this).inflate(R.layout.themeclub_online_wallpaper_detail, null),
                    -1);
        } else if (themeBeans == null || wallpapersBeans == null) {
            return;
        } else if (themeBeans.size() == 0 || wallpapersBeans.size() == 0) {
            return;
        } else if (v.getId() == R.id.source_1) {
            startMoreWallpaper();
        } else if (v.getId() == R.id.more_themes) {
            Intent intent = new Intent(this, SourceDetailActivity.class);
            intent.putExtra("id", themeBeans.get(0).getId());
            intent.putExtra("isTheme", true);
            intent.putExtra("source", themeBeans.get(0).getSource());
            intent.putExtra("url", themeBeans.get(0).getSourceLogoUrl());
            startActivity(intent);
        } else if (v.getId() == R.id.more_themes_item) {
            Intent themeIntent = new Intent(this, OnlineThemeDetailActivity.class);
            themeIntent.putExtra("themebean", themeBeans.get(0));
            startActivity(themeIntent);
        } else if (v.getId() == R.id.more_wallpaper) {
            startMoreWallpaper();
        } else if (v.getId() == R.id.item_left) {
            startWallpaperDetail(wallpapersBeans.get(0));
        } else if (v.getId() == R.id.item_center) {
            startWallpaperDetail(wallpapersBeans.get(1));
        } else if (v.getId() == R.id.item_right) {
            startWallpaperDetail(wallpapersBeans.get(2));
        } else if (v.getId() == R.id.ivImage) {
            displayPreview();
        }

    }

    private void startMoreWallpaper() {
        Intent intent = new Intent(this, SourceDetailActivity.class);
        intent.putExtra("id", mWallpaper.getId());
        intent.putExtra("isTheme", false);
        intent.putExtra("source", mWallpaper.getSource());
        intent.putExtra("url", mWallpaper.getSourceLogoUrl());
        Log.e(TAG, "url=" + mWallpaper.getSourceLogoUrl());
        startActivity(intent);
    }


    public void startWallpaperDetail(WallPapersBean o) {
        Intent wallpaperIntent = new Intent(this, OnlineWallpaperDetailActivity.class);
        wallpaperIntent.putExtra(WallpaperUtil.ONLINEWALLPAPERBEAN, o);
        startActivity(wallpaperIntent);
    }


    private void displayPreview() {
        final Dialog dialog = new Dialog(this, R.style.Dialog_Fullscreen);
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        lp.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        lp.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        lp.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        dialog.getWindow().setAttributes(lp);
        RelativeLayout layout = (RelativeLayout) getLayoutInflater().inflate(R.layout.themeclub_wallpaper_popwindow, null);
        ImageView displayImage = (ImageView) layout.findViewById(R.id.wallpaper_display);
        displayImage.setImageBitmap(mMemoryCache.get(downloadUrl));
        displayImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(layout);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Glide.get(this).clearMemory();
        mWallpaperPresenter.onDestroy();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        iVImageBackground.setVisibility(View.VISIBLE);
        if (progress != null) {
            progress.setVisibility(View.VISIBLE);
        }
        initConfigs();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}
