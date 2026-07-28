package com.amz.ios.themeclub.ui.activity;

import android.content.Intent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.util.Util;
import com.amz.ios.ioslite.common.ad.IOSAdConfig;
import com.amz.ios.ioslite.common.ad.NativeAdCardView;
import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.launcher.LauncherRouter;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.ThemeClubApplication;
import com.amz.ios.themeclub.adapter.OnlineGalleryAdapter;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.base.BaseDetailActivity;
import com.amz.ios.themeclub.bean.ThemesBean;
import com.amz.ios.themeclub.bean.WallPapersBean;
import com.amz.ios.themeclub.bean.request.OneThemesRequest;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.NetWorkUtils;
import com.amz.ios.themeclub.util.PreferencesUtils;
import com.amz.ios.themeclub.util.ShareUtils;
import com.amz.ios.themeclub.util.SpaceItemUtils;
import com.amz.ios.themeclub.util.WallpaperUtil;
import com.amz.ios.themeclub.view.CircleImageView;

import org.json.JSONException;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class OnlineThemeDetailActivity<W> extends BaseDetailActivity<ThemesBean, W> {
    private OnlineGalleryAdapter mGalleryAdapter;
    private RecyclerView mRecyclerView;
    private CustomTextView mThemeName;
    private ImageView mShareImage;
    private ImageView mBack;
    private CustomTextView mMoreTheme;
    private ImageView mMoreThemeImage;
    private CustomTextView mMoreWallpaper;
    private ImageView mItemLeft;
    private ImageView mItemCenter;
    private ImageView mItemRight;
    private CircleImageView mSourceIcon;
    private CustomTextView mAuthor;
    private CustomTextView mSource;
    private ArrayList<String> mListPics;
    private ArrayList<ThemesBean> mThemeBeans;
    private ArrayList<WallPapersBean> mWallpapersBeans;
    private ThemesBean mTheme;
    private String mFilePath;
    private RequestManager mGlide;
    private LinearLayout mGoToSourceDetail;
    private ScrollView mScroolView;
    private NativeAdCardView mAdView;
    private int mId;

    @Override
    public int setLayoutId() {
        return R.layout.activity_online_theme_detail;
    }

    @Override
    public void findViewById() {
        mRecyclerView = (RecyclerView) findViewById(R.id.particular_info);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.themeclub_gallery_space);
        mRecyclerView.addItemDecoration(new SpaceItemUtils(spacingInPixels));
        mThemeName = (CustomTextView) findViewById(R.id.theme_name);
        mShareImage = (ImageView) findViewById(R.id.share_detail);
        mBack = (ImageView) findViewById(R.id.back);
        mDownloadBtn = (Button) findViewById(R.id.download_theme);
        mMoreTheme = (CustomTextView) findViewById(R.id.more_themes);
        mMoreThemeImage = (ImageView) findViewById(R.id.more_themes_item);
        mMoreWallpaper = (CustomTextView) findViewById(R.id.more_wallpaper);
        mItemLeft = (ImageView) findViewById(R.id.item_left);
        mItemCenter = (ImageView) findViewById(R.id.item_center);
        mItemRight = (ImageView) findViewById(R.id.item_right);
        mSourceIcon = (CircleImageView) findViewById(R.id.sources_icon);
        mAuthor = (CustomTextView) findViewById(R.id.author);
        mSource = (CustomTextView) findViewById(R.id.source_text);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar_id);
        mGoToSourceDetail = (LinearLayout) findViewById(R.id.source_1);
        mScroolView = (ScrollView) findViewById(R.id.scroolView);
        mAdView = (NativeAdCardView) findViewById(R.id.adview);
    }

    @Override
    public void setupView(ThemesBean themesBean) {
        mAdView.setAdvertiseId(IOSAdConfig.ID_THEME_DETAIL);
        mAdView.loadAdvertise();
    }

    @Override
    public void addClick() {
        mShareImage.setOnClickListener(this);
        mBack.setOnClickListener(this);
        mDownloadBtn.setOnClickListener(this);
        mMoreTheme.setOnClickListener(this);
        mMoreThemeImage.setOnClickListener(this);
        mMoreWallpaper.setOnClickListener(this);
        mItemLeft.setOnClickListener(this);
        mItemCenter.setOnClickListener(this);
        mItemRight.setOnClickListener(this);
        mSourceIcon.setOnClickListener(this);
        mSource.setOnClickListener(this);
        mGoToSourceDetail.setOnClickListener(this);
    }

    @Override
    public ThemesBean handleIntent(Intent intent) {
        if (intent != null) {
            mId = intent.getIntExtra("theme_id", -1);
            mTheme = (ThemesBean) intent.getSerializableExtra("themebean");
            handleIntentId();
            mFilePath = AppUtils.getSDPath() + "/themes/" + mTheme.getName() + mTheme.getId() + ".apk";
        }
        mGlide = Glide.with(this);
        mScroolView.fullScroll(View.FOCUS_UP);
        return mTheme;
    }

    @Override
    public void loadData(ThemesBean themesBean) {
        if (mTheme == null) {
            return;
        }
        NetWorkUtils.getInstance().getDataFromServer(NetWorkUtils.CommonResourceFactory(mTheme.getId(), 1, mTheme.getSource(), ThemeClubApplication.getContext()), new Response.Listener() {
            @Override
            public void onResponse(Object response) {
                DebugLog.w(TAG,"====================onResponse:"+response.toString());
                try {
                    mThemeBeans = AppUtils.getCommonTheme(response.toString());
                    mWallpapersBeans = AppUtils.getCommonWallpapers(response.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    loadView();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.toString());
            }
        });
        displayGallery();
    }

    @Override
    protected long getDownLoadId() {
        return PreferencesUtils.getLong(this, mTheme.getId() + "");
    }

    @Override
    protected boolean checkIfDownSuccessByMD5() {
        DebugLog.w(TAG, "=================checkIfDownSuccessByMD5:" + mTheme.getFileMD5() + mFilePath);
        return AppUtils.checkIfDownSuccessByMD5(mTheme.getFileMD5(), new File(mFilePath));
    }

    @Override
    protected boolean checkInstalled() {
        return AppUtils.checkInstalled(this, mTheme.getPackageName());
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        final int id = v.getId();
        if (id == R.id.share_detail) {
            ShareUtils.ShareText(this, getString(R.string.themeclub_share_theme));
            AnalyticsDelegate.onThemeEvent(this, UMEventConstants.THEME_SHARE);
        } else if (id == R.id.back) {
            finish();
        } else if (id == R.id.download_theme) {
            final CharSequence text = ((Button) v).getText();
            if (TextUtils.equals(text, getResources().getString(R.string.themeclub_download_theme))) {
                downloadTheme();
            } else if (TextUtils.equals(text, getResources().getString(R.string.themeclub_install))) {
                AppUtils.AppInstall(mFilePath, this);
            } else if (TextUtils.equals(text, getResources().getString(R.string.themeclub_apply))) {
                applyTheme();
            }
            sendHandleMessage();
        } else if (mThemeBeans == null || mWallpapersBeans == null) {
            return;
        } else if (mThemeBeans.size() == 0 || mWallpapersBeans.size() == 0) {
            return;
        } else if (id == R.id.more_themes) {
            startMoreTheme();
        } else if (id == R.id.more_themes_item) {
            Intent themeIntent = new Intent(this, OnlineThemeDetailActivity.class);
            themeIntent.putExtra("themebean", mThemeBeans.get(0));
            startActivity(themeIntent);
        } else if (id == R.id.more_wallpaper) {
            startMoreWallpaper();
        } else if (id == R.id.source_1) {
            Intent intent = new Intent(this, SourceDetailActivity.class);
            intent.putExtra("id", mTheme.getId());
            intent.putExtra("source", mTheme.getSource());
            intent.putExtra("isTheme", true);
            intent.putExtra("url", mTheme.getSourceLogoUrl());
            startActivity(intent);
        } else if (id == R.id.item_left) {
            startWallpaperDetail(mWallpapersBeans, 0);
        } else if (v.getId() == R.id.item_center) {
            startWallpaperDetail(mWallpapersBeans, 1);
        } else if (v.getId() == R.id.item_right) {
            startWallpaperDetail(mWallpapersBeans, 2);
        }
    }

    public void startWallpaperDetail(ArrayList<WallPapersBean> wallpaperList, int position) {
        Intent wallpaperIntent = new Intent(this, OnlineWallpapersDetailActivity.class);
        wallpaperIntent.putExtra(WallpaperUtil.ONLINEWALLPAPERLIST, (Serializable) wallpaperList);
        wallpaperIntent.putExtra(WallpaperUtil.ONLINEWALLPAPER_POSITION, position);
        startActivity(wallpaperIntent);
    }

    /**
     * 开始更多壁纸
     */
    private void startMoreWallpaper() {
        Intent intent = new Intent(this, SourceDetailActivity.class);
        intent.putExtra("id", mWallpapersBeans.get(0).getId());
        intent.putExtra("source", mWallpapersBeans.get(0).getSource());
        intent.putExtra("isTheme", false);
        intent.putExtra("url", mWallpapersBeans.get(0).getSourceLogoUrl());
        startActivity(intent);
    }

    public void startMoreTheme() {
        Intent intent = new Intent(this, SourceDetailActivity.class);
        intent.putExtra("id", mTheme.getId());
        intent.putExtra("source", mTheme.getSource());
        intent.putExtra("isTheme", true);
        intent.putExtra("url", mTheme.getSourceLogoUrl());
        startActivity(intent);
    }

    /**
     * 应用主题
     */
    private void applyTheme() {
        LauncherRouter.launchToNewTheme(this, mTheme.getPackageName());
//        Toast.makeText(this, getString(R.string.themeclub_set_theme_succeed), Toast.LENGTH_LONG).show();
    }

    /**
     * 下载主题
     */
    private void downloadTheme() {
        mProgressBar.setVisibility(View.VISIBLE);
        mDownloadBtn.setVisibility(View.GONE);
        mDownloadHelper.download(mTheme);
        mDownloadId = PreferencesUtils.getLong(this, mTheme.getId() + "");
        DebugLog.w(TAG, "================downloadTheme:" + mDownloadId);
        PreferencesUtils.putString(this, mDownloadId + "", mFilePath);
    }

    /**
     * 显示预览图
     */
    private void displayGallery() {
        int count = mTheme.getScreenshotList().size();
        mListPics = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            mListPics.add(mTheme.getScreenshotList().get(i).getDownloadUrl());
        }
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mGalleryAdapter = new OnlineGalleryAdapter(this, mListPics, AppConfig.TAB_THEME);
        mRecyclerView.setAdapter(mGalleryAdapter);
    }

    /**
     * 代表从福主题过来的，唤起桌面
     */
    public void wakeIOSLite() {
        if (mId != -1) {
            Intent intent = new Intent();
            intent.setAction("ioslite.intent.action.IOSLITE");
            startActivity(intent);
        }
    }

    private boolean handleIntentId() {
        if (mId != -1) {
            //come from CommonTheme
            OneThemesRequest request = new OneThemesRequest(this, mId, 1);
            NetWorkUtils.getInstance().getDataFromServer(request, new Response.Listener() {
                @Override
                public void onResponse(Object o) {
                    try {
                        mTheme = AppUtils.spliteOneTheme(o.toString());
                        DebugLog.w(TAG, "=====================handleIntentId:" + o.toString() + "/" + mTheme.toString());
                        loadData(mTheme);
                    } catch (JSONException e) {
                        DebugLog.w(TAG, "=====================handleIntentId error:" + e);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    volleyError.printStackTrace();
                    Toast.makeText(ThemeClubApplication.getContext(), getString(R.string.themeclub_try), Toast.LENGTH_SHORT).show();
                    wakeIOSLite();
                    finish();
                }
            });
            return true;
        }
        return false;
    }

    private void loadView() {
        if (mTheme != null) {
            mThemeName.setText(mTheme.getName());
            mSource.setText(mTheme.getSource());
            mAuthor.setText(mTheme.getAuthor());
        }
        if (Util.isOnMainThread()) {
            mGlide.load(mTheme.getSourceLogoUrl()).into(mSourceIcon);
            if (mThemeBeans == null || mWallpapersBeans == null) {
                return;
            }
            if (mThemeBeans.size() != 0) {
                mGlide.load(mThemeBeans.get(0).getPreview().getDownloadUrl()).into(mMoreThemeImage);
            }

            if (mWallpapersBeans.size() == 3) {
                mGlide.load(mWallpapersBeans.get(0).getSmallImage().getDownloadUrl()).into(mItemLeft);
                mGlide.load(mWallpapersBeans.get(1).getSmallImage().getDownloadUrl()).into(mItemCenter);
                mGlide.load(mWallpapersBeans.get(2).getSmallImage().getDownloadUrl()).into(mItemRight);
            }
        }
    }
}