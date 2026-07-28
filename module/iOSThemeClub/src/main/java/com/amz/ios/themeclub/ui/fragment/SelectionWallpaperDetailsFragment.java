package com.amz.ios.themeclub.ui.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Util;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.amz.ios.ioslite.common.ad.IOSAdConfig;
import com.amz.ios.ioslite.common.ad.NativeAdCardView;
import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.util.ToastUtil;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.bean.ThemesBean;
import com.amz.ios.themeclub.bean.WallPapersBean;
import com.amz.ios.themeclub.intertfaces.IProgressView;
import com.amz.ios.themeclub.presenter.WallpaperPresenter;
import com.amz.ios.themeclub.ui.activity.CropImageActivity;
import com.amz.ios.themeclub.ui.activity.OnlineThemeDetailActivity;
import com.amz.ios.themeclub.ui.activity.OnlineWallpaperDetailActivity;
import com.amz.ios.themeclub.ui.activity.SourceDetailActivity;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.NetWorkUtils;
import com.amz.ios.themeclub.util.ShareUtils;
import com.amz.ios.themeclub.util.WallpaperUtil;

import org.json.JSONException;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;


public class SelectionWallpaperDetailsFragment extends Fragment implements View.OnClickListener {
    public static String TAG = "WallpaperDetailsF";
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
    WallPapersBean mWallpaper;
    private String filePath;
    private String downloadUrl;
    private ImageView resouceImage;
    private View rootView;
    CollapsingToolbarLayout mCollapsingToolbarLayout;
    private WallpaperPresenter mWallpaperPresenter;
    private Bitmap mBitmap;
    private RequestManager mGlide;
    private boolean mIsUserVisibility;
    private boolean mIsViewCreatedEnd;
    private int mLoadSmallCount = 0;
    private int mLoadBigCount = 0;
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

    public static SelectionWallpaperDetailsFragment newInstance(WallPapersBean wallpaper) {
        SelectionWallpaperDetailsFragment wallpaperDetailsFragment = new SelectionWallpaperDetailsFragment();
        Bundle bundle = new Bundle();
        Log.e(TAG, "0x0 = " + wallpaper.toString());
        bundle.putSerializable(WallpaperUtil.ONLINEWALLPAPERBEAN, wallpaper);
        wallpaperDetailsFragment.setArguments(bundle);
        return wallpaperDetailsFragment;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        mIsUserVisibility = isVisibleToUser;
        if (mIsUserVisibility && mIsViewCreatedEnd) {
            initData(rootView);
        }
    }

    private CustomTarget mSmallTarget = new CustomTarget<Bitmap>() {
        @Override
        public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
            DebugLog.w(TAG, "=============SmallImage onResourceReady");
            iVimige.setClickable(false);
            iVimige.setImageBitmap(bitmap);
            iVImageBackground.setVisibility(View.GONE);
            mLoadSmallCount = 0;
            loadBigImage();
        }

        @Override
        public void onLoadCleared(@Nullable Drawable drawable) {
            mLoadSmallCount += 1;
            //Starts any not yet completed or failed requests
            if (mLoadSmallCount < 4) {
                mGlide.resumeRequests();
            } else {
                ToastUtil.show(getActivity(), R.string.themeclub_download_failure);
                mLoadSmallCount = 0;
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
//            mLoadSmallCount = 0;
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
//            mLoadSmallCount += 1;
//            //Starts any not yet completed or failed requests
//            if (mLoadSmallCount < 4) {
//                mGlide.resumeRequests();
//            } else {
//                ToastUtil.show(getActivity(), R.string.themeclub_download_failure);
//                mLoadSmallCount = 0;
//                loadBigImage();
//            }
//        }
//    };

    protected void fragmentLoadData(View v) {
        NativeAdCardView adView = (NativeAdCardView) v.findViewById(R.id.adview);
        adView.setAdvertiseId(IOSAdConfig.ID_WALLPAPER_DETAIL);
        adView.loadAdvertise();
        Log.e(TAG, "load data---------------" + mWallpaper.toString());
        filePath = mWallpaperPresenter.getWallpaperPath();
        downloadUrl = mWallpaper.getSourceLogoUrl();
        source.setText(mWallpaper.getSource());
        author.setText(mWallpaper.getAuthor());
        Log.e(TAG, mWallpaper.toString());

        mGlide.asBitmap().load(mWallpaper.getSmallImage().getDownloadUrl()).priority(Priority.HIGH).into(mSmallTarget);
        Log.e(TAG, mWallpaper.getSmallImage().getDownloadUrl() + "::" + mWallpaper.getBigImage().getDownloadUrl());
    }

    private CustomTarget mBigTarget = new CustomTarget<Bitmap>() {
        @Override
        public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
            DebugLog.w(TAG, "=============BigImage onResourceReady");
            iVimige.setClickable(true);
            iVimige.setImageBitmap(bitmap);
            mBitmap = bitmap;
            mMemoryCache.put(downloadUrl, bitmap);
            progress.setVisibility(View.INVISIBLE);
            downloadButton.setEnabled(true);
            setWallpapweButton.setEnabled(true);
            iVImageBackground.setVisibility(View.GONE);
            mLoadBigCount = 0;
            loadOtherDetails();
        }

        @Override
        public void onLoadCleared(@Nullable Drawable drawable) {

        }

        @Override
        public void onLoadFailed(@Nullable Drawable errorDrawable) {
            super.onLoadFailed(errorDrawable);
            mLoadBigCount += 1;
            //Starts any not yet completed or failed requests
            if (mLoadBigCount < 4) {
                mGlide.resumeRequests();
            } else {
                progress.setVisibility(View.INVISIBLE);
                ToastUtil.show(getActivity(), R.string.themeclub_download_failure);
                loadOtherDetails();
                mLoadBigCount = 0;
            }
        }
    };

//            new SimpleTarget<Bitmap>() {
//        @Override
//        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
//            DebugLog.w(TAG, "=============BigImage onResourceReady");
//            iVimige.setClickable(true);
//            iVimige.setImageBitmap(resource);
//            mBitmap = resource;
//            mMemoryCache.put(downloadUrl, resource);
//            progress.setVisibility(View.INVISIBLE);
//            downloadButton.setEnabled(true);
//            setWallpapweButton.setEnabled(true);
//            iVImageBackground.setVisibility(View.GONE);
//            mLoadBigCount = 0;
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
//            mLoadBigCount += 1;
//            //Starts any not yet completed or failed requests
//            if (mLoadBigCount < 4) {
//                mGlide.resumeRequests();
//            } else {
//                progress.setVisibility(View.INVISIBLE);
//                ToastUtil.show(getActivity(), R.string.themeclub_download_failure);
//                loadOtherDetails();
//                mLoadBigCount = 0;
//            }
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

        NetWorkUtils.getInstance().getDataFromServer(NetWorkUtils.CommonResourceFactory(mWallpaper.getId(), 2, mWallpaper.getSource(), getActivity()), new Response.Listener() {
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle args = getArguments();
        if (args != null) {
            mWallpaper = (WallPapersBean) args.getSerializable(WallpaperUtil.ONLINEWALLPAPERBEAN);
            if (mWallpaper == null) {
                return;
            }
            mWallpaperPresenter = new WallpaperPresenter(getContext(), mWallpaper, AppConfig.isWallPaperScroolEnable(getContext()));
        }
        mGlide = Glide.with(SelectionWallpaperDetailsFragment.this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.e(TAG, "ON CREATEVIEW");
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.themeclub_online_wallpaper_detail, null);
            mCoordinatorLayout = (CoordinatorLayout) rootView.findViewById(R.id.cooddinator);
            mCollapsingToolbarLayout = (CollapsingToolbarLayout) rootView.findViewById(R.id.collapsing_toolbar);
            iVimige = (ImageView) rootView.findViewById(R.id.ivImage);
            iVImageBackground = (ImageView) rootView.findViewById(R.id.ivImageBackaground);
            iVImageBackground.setAlpha((float) 0.35);
        } else {
            if (null != rootView) {
                ViewGroup parent = (ViewGroup) rootView.getParent();
                if (null != parent) {
                    parent.removeView(rootView);
                }
            }
        }
        mIsViewCreatedEnd = true;
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mIsUserVisibility && mIsViewCreatedEnd) {
            initData(rootView);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        downloadButton = (Button) rootView.findViewById(R.id.download_wallpaper);
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

    private void initData(View v) {
        DebugLog.w(TAG, "======================initData");
        mToolbar = (Toolbar) v.findViewById(R.id.toolbar);
        mToolbar.setTitle("");
        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationIcon(R.drawable.source_detail_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        mCollapsingToolbarLayout.setTitle("");
        sourceIcon = (ImageView) v.findViewById(R.id.sources_icon);
        author = (CustomTextView) v.findViewById(R.id.author);
        source = (CustomTextView) v.findViewById(R.id.source_text);
        moreTheme = (CustomTextView) v.findViewById(R.id.more_themes);
        moreThemeImage = (ImageView) v.findViewById(R.id.more_themes_item);
        moreWallpaper = (CustomTextView) v.findViewById(R.id.more_wallpaper);
        itemLeft = (ImageView) v.findViewById(R.id.item_left);
        downloadButton = (Button) v.findViewById(R.id.download_wallpaper);
        itemCenter = (ImageView) v.findViewById(R.id.item_center);
        itemRight = (ImageView) v.findViewById(R.id.item_right);
        setWallpapweButton = (Button) v.findViewById(R.id.set_wallpaper);
        mGoToSource = (LinearLayout) v.findViewById(R.id.source_1);
        progress = (ProgressBar) v.findViewById(R.id.wallpaper_progress_item);
        resouceImage = (ImageView) v.findViewById(R.id.resouce_image);
        mWallpaperProgress = (RelativeLayout) v.findViewById(R.id.progress_set);
        initListener();
        themeBeans = new ArrayList<>();
        wallpapersBeans = new ArrayList<>();
        mMemoryCache = new LruCache<>(CACHE_SIZE);
        fragmentLoadData(v);
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
    public void onDestroy() {
        Glide.get(getContext()).clearMemory();
        if (null != rootView) {
            if (rootView.getParent() != null) {
                ((ViewGroup) rootView.getParent()).removeView(rootView);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_wallpaper_detail, menu);
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

    @Override
    public void onClick(View v) {
        Log.e(TAG, "ON CLICK");
        if (v.getId() == R.id.download_wallpaper) {
            if (filePath == null) {
                return;
            }
            mWallpaperPresenter.downloadWallpaper(mMemoryCache.get(downloadUrl), false);
            downloadButton.setText(R.string.themeclub_wallpaper_has_download);
            downloadButton.setClickable(false);
        } else if (v.getId() == R.id.set_wallpaper) {
            mWallpaperPresenter.applyWallpaper(getActivity(),
                    mIProgressView,
                    mBitmap,
                    null,
                    LayoutInflater.from(getContext()).inflate(R.layout.themeclub_online_wallpaper_detail, null),
                    -1);

        } else if (themeBeans == null || wallpapersBeans == null) {
            return;
        } else if (themeBeans.size() == 0 || wallpapersBeans.size() == 0) {
            Log.e(TAG, themeBeans.toString() + " SIZE IS  NULL" + wallpapersBeans.toString());
            return;
        } else if (v.getId() == R.id.source_1) {
            startMoreWallpaper();
        } else if (v.getId() == R.id.more_themes) {
            Intent intent = new Intent(getActivity(), SourceDetailActivity.class);
            intent.putExtra("id", themeBeans.get(0).getId());
            intent.putExtra("isTheme", true);
            intent.putExtra("source", themeBeans.get(0).getSource());
            intent.putExtra("url", themeBeans.get(0).getSourceLogoUrl());
            startActivity(intent);
        } else if (v.getId() == R.id.more_themes_item) {
            Intent themeIntent = new Intent(getActivity(), OnlineThemeDetailActivity.class);
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
        Intent intent = new Intent(getActivity(), SourceDetailActivity.class);
        intent.putExtra("id", mWallpaper.getId());
        intent.putExtra("isTheme", false);
        intent.putExtra("source", mWallpaper.getSource());
        intent.putExtra("url", mWallpaper.getSourceLogoUrl());
        Log.e(TAG, "url=" + mWallpaper.getSourceLogoUrl());
        startActivity(intent);
    }

    public void startWallpaperDetail(WallPapersBean wallPapersBean) {
        Intent intent = new Intent(getContext(), OnlineWallpaperDetailActivity.class);
        intent.putExtra(WallpaperUtil.ONLINEWALLPAPERBEAN, (Serializable) wallPapersBean);
        startActivity(intent);
    }

    private void displayPreview() {
        final Dialog dialog = new Dialog(getActivity(), R.style.Dialog_Fullscreen);
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        lp.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        lp.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        lp.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        dialog.getWindow().setAttributes(lp);
        RelativeLayout layout = (RelativeLayout) getActivity().getLayoutInflater().inflate(R.layout.themeclub_wallpaper_popwindow, null);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.share_wallpaper) {
            AnalyticsDelegate.onWallpaperEvent(getContext(), UMEventConstants.WALLPAPER_SHARE);
            File mFile = new File(filePath);
            if (mFile.exists()) {
                ShareUtils.SharePic(getContext(), getActivity().getTitle().toString(), filePath);
            } else {
                ShareUtils.ShareText(getContext(), getString(R.string.themeclub_share_wallpaper));
            }
        }
        if (id == R.id.crop_wallpaper) {
            Intent cropIntent = new Intent(getActivity(), CropImageActivity.class);
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
        return true;
    }
}