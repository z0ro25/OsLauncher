package com.amz.ios.themeclub.ui.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.provider.Settings;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.adapter.OnlineGalleryAdapter;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.base.BaseDetailActivity;
import com.amz.ios.themeclub.bean.LockScreenBean;
import com.amz.ios.themeclub.bean.LockScreenMoreBean;
import com.amz.ios.themeclub.bean.LockscreenInfo;
import com.amz.ios.themeclub.bean.request.LockScreenDetailRequest;
import com.amz.ios.themeclub.presenter.LockScreenDetailPresenter;
import com.amz.ios.themeclub.tools.DownloadHelper;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.LockScreenUtils;
import com.amz.ios.themeclub.util.PreferencesUtils;
import com.amz.ios.themeclub.util.ShareUtils;
import com.amz.ios.themeclub.view.LockScreenItemView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ubuntu on 16/06/17.
 */

public class LockScreenDetailActivity extends BaseDetailActivity<LockScreenBean, LockScreenMoreBean> {
    private OnlineGalleryAdapter mGalleryAdapter;
    private RecyclerView mRecyclerView;
    private ArrayList<String> listPics;
    private LockScreenBean mLockScreenBean;
    private CustomTextView mTitleText;
    private CustomTextView mAuthorText;
    private CustomTextView mSourcesText;
    private ImageView mLockBack;
    private ImageView mLockShare;
    private String mFilePath;
    private static final int REQUEST_CODE_WRITE_SETTINGS = 1;
    private CardView mMoreLockScreenContainer;
    private CardView mMoreWallpapers;
    private CardView mMoreThemes;
    private int id;
    private List<LockScreenBean> mScreens;
    private ScrollView mScroolView;
    private LockScreenItemView mMoreLockScreen;
    private ImageView mAuthorGuidanceIv;

    @Override
    public int setLayoutId() {
        return R.layout.themeclub_activity_lock_screen_detail;
    }

    @Override
    public void findViewById() {
        mRecyclerView = (RecyclerView) findViewById(R.id.lock_particular_info);
        mTitleText = (CustomTextView) findViewById(R.id.theme_name);
        mAuthorText = (CustomTextView) findViewById(R.id.author);
        mSourcesText = (CustomTextView) findViewById(R.id.source_text);
        mDownloadBtn = (Button) findViewById(R.id.download_lock);
        mDownloadHelper = new DownloadHelper(this);
        mProgressBar = (ProgressBar) findViewById(R.id.lock_progressbar_id);
        mLockBack = (ImageView) findViewById(R.id.back);
        mLockShare = (ImageView) findViewById(R.id.share_detail);
        mMoreLockScreenContainer = (CardView) findViewById(R.id.more_lock_screen_constainer);
        mMoreLockScreenContainer.setVisibility(View.VISIBLE);
        mMoreWallpapers = (CardView) findViewById(R.id.wallpaper_detail_more_wallpapers);
        mMoreWallpapers.setVisibility(View.GONE);
        mMoreThemes = (CardView) findViewById(R.id.detail_more_themes);
        mMoreThemes.setVisibility(View.GONE);
        mMoreLockScreen = (LockScreenItemView) findViewById(R.id.more_lock_screen);
        mScroolView = (ScrollView) findViewById(R.id.ScroolView);
        mAuthorGuidanceIv = (ImageView) findViewById(R.id.iv_author_guidance);
    }

    @Override
    public void setupView(LockScreenBean lockScreenBean) {
        if (lockScreenBean != null) {
            mTitleText.setText(lockScreenBean.getName());
            mAuthorText.setText(lockScreenBean.getAuthor());
            mSourcesText.setText(lockScreenBean.getIntro());
            mAuthorGuidanceIv.setVisibility(View.GONE);
            displayGallery(lockScreenBean);
        }
    }

    @Override
    public void addClick() {
        mDownloadBtn.setOnClickListener(this);
        mLockBack.setOnClickListener(this);
        mLockShare.setOnClickListener(this);
    }

    @Override
    public LockScreenBean handleIntent(Intent intent) {
        if (intent != null) {
            mLockScreenBean = (LockScreenBean) intent.getSerializableExtra("lockbean");
        }
        mScroolView.fullScroll(View.FOCUS_UP);
        mFilePath = AppUtils.getSDPath() + "/lock/" + mLockScreenBean.getPackageName() + mLockScreenBean.getId() + ".apk";
        return mLockScreenBean;
    }

    @Override
    public void loadData(LockScreenBean lockScreenBean) {
        if (lockScreenBean == null) {
            return;
        }
        LockScreenDetailPresenter lockScreenDetailPresenter = new LockScreenDetailPresenter(this);
        final LockScreenDetailRequest lockScreenDetailRequest = new LockScreenDetailRequest(this, mLockScreenBean.getId());
        lockScreenDetailPresenter.getDatas(lockScreenDetailRequest);
    }

    @Override
    protected long getDownLoadId() {
        try {
            final int id = mLockScreenBean.getId();
            DebugLog.w(TAG, "===============getDownLoadId:" + id);
            final long downloadId = PreferencesUtils.getLong(this, id + "");
            DebugLog.w(TAG, "===============getDownLoadId:" + downloadId);
            return downloadId;
        } catch (Exception e) {
            DebugLog.w(TAG, "===============getDownLoadId error:" + e);
            return -1;
        }
    }

    @Override
    protected boolean checkIfDownSuccessByMD5() {
        return AppUtils.checkIfDownSuccessByMD5(mLockScreenBean.getFileMd5(), new File(mFilePath));
    }

    @Override
    protected boolean checkInstalled() {
        return AppUtils.checkInstalled(this, mLockScreenBean.getPackageName());
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        id = v.getId();
        if (id == R.id.download_lock) {
            final CharSequence text = ((Button) v).getText();
            if (TextUtils.equals(text, getResources().getString(R.string.themeclub_download_theme))) {
                downloadLock();
            } else if (TextUtils.equals(text, getResources().getString(R.string.themeclub_install))) {
                AppUtils.AppInstall(mFilePath, this);
            } else if (TextUtils.equals(text, getResources().getString(R.string.themeclub_apply))) {
                final LockscreenInfo lockscreenInfo = new LockscreenInfo(this, mLockScreenBean.getName(), mLockScreenBean.getPackageName(), mFilePath);
                LockScreenUtils.applyLockScreen(this, lockscreenInfo);
            }
            sendHandleMessage();
        } else if (id == R.id.back) {
            finish();
        } else if (id == R.id.share_detail) {
            if (mLockScreenBean != null) {
                ShareUtils.ShareText(this, mLockScreenBean.getName() + getString(R.string.themeclub_share_lockscreen));
            }
        }
    }

    @Override
    public void showDatas(LockScreenMoreBean bean) {
        mScreens = bean.getScreens();
        mMoreLockScreen.setData(mScreens);
        mMoreLockScreen.requestLayout();
    }

    @Override
    @TargetApi(23)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS) {
            if (Settings.System.canWrite(this)) {
                DebugLog.w(TAG, "onActivityResult write settings granted");
                final LockscreenInfo lockscreenInfo = new LockscreenInfo(this, mLockScreenBean.getName(), mLockScreenBean.getPackageName(), mFilePath);
                LockScreenUtils.applyLockScreen(this, lockscreenInfo);
            }
        }
    }

    /**
     * @param
     * @return
     * @description download lockScreen apk
     */
    private void downloadLock() {
        mProgressBar.setVisibility(View.VISIBLE);
        mDownloadBtn.setVisibility(View.GONE);
        mDownloadHelper.downloadLock(mLockScreenBean,mFilePath);
        mDownloadId = PreferencesUtils.getLong(this, mLockScreenBean.getId() + "");
        PreferencesUtils.putString(this, mDownloadId + "", mFilePath);
    }

    /**
     * 显示预览图
     */
    private void displayGallery(LockScreenBean lockScreenBean) {
        int count = lockScreenBean.getScreenshotList().size();
        listPics = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            listPics.add(mLockScreenBean.getScreenshotList().get(i).getDownloadUrl());
        }
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mGalleryAdapter = new OnlineGalleryAdapter(this, listPics, AppConfig.TAB_LOCK);
        mRecyclerView.setAdapter(mGalleryAdapter);
    }
}
