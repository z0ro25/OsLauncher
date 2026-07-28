package com.amz.ios.themeclub.base;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.util.ToastUtil;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.intertfaces.IViewShowDatas;
import com.amz.ios.themeclub.tools.DownloadHelper;
import com.amz.ios.themeclub.tools.DownloadHelper.DownLoadStateBean;

/*
* this base class is for LockScreenDetailActivity and OnlineThemeDetailActivity
* */
public abstract class BaseDetailActivity<T, W> extends CommonAppCompatActivity implements View.OnClickListener, IViewShowDatas<W> {
    public final String TAG = "BaseDetailActivity";
    private DownloadChangeObserver mDownloadObserver = new DownloadChangeObserver();
    private ContentResolver mContentResolver;
    private T mData;
    public DownloadHelper mDownloadHelper;
    private final int DOWNLOAD_CODE = 0;
    public final int MSG_LCOKSREEN_WALLPAPER_CHANGE = 1;
    public long mDownloadId = -1;
    public Button mDownloadBtn;
    public ProgressBar mProgressBar;
    private final int PROGRESS_MAX = 100;
    private final int PROGRESS_MIN = 0;

    private class DownloadChangeObserver extends ContentObserver {

        public DownloadChangeObserver() {
            super(mHandler);
        }

        @Override
        public void onChange(boolean selfChange) {
            sendHandleMessage();
        }
    }

    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DOWNLOAD_CODE:
                    DownLoadStateBean downLoadStateBean = (DownLoadStateBean) msg.obj;
                    final int[] bytesAndStatus = downLoadStateBean.bytesAndStatus;
                    int status = bytesAndStatus[2];
                    final Boolean isFromResume = downLoadStateBean.isFromResume;
                    DebugLog.w(TAG, "==============line(56) +#handleMessage:" + status);
                    if (mDownloadHelper.isDownloading(status)) {
                        downloading(bytesAndStatus);
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        downloadSuccessful(false,isFromResume);
                    } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        downloadSuccessful(true,isFromResume);
                    } else {
                        unDownload();
                    }
                    break;
//                case MSG_LCOKSREEN_WALLPAPER_CHANGE:
//                    Intent intent = new Intent(AppConfig.ACTION_LOCKSCREEN_WALLPAPER_CHANGED);
//                    sendBroadcast(intent);
//                    break;
            }
        }
    };

    public void sendHandleMessage() {
        sendHandleMessage(false);
    }

    public void sendHandleMessage(boolean isFromResume) {      //没有网络时,status=8;
        DebugLog.w(TAG, "=====================sendHandleMessage:" + mDownloadId);
        int[] bytesAndStatus = mDownloadHelper.getBytesAndStatus(mDownloadId);
        //0 COLUMN_BYTES_DOWNLOADED_SO_FAR;1 COLUMN_TOTAL_SIZE_BYTES;2 COLUMN_STATUS
        final DownLoadStateBean downLoadStateBean = new DownLoadStateBean();
        downLoadStateBean.bytesAndStatus = bytesAndStatus;
        downLoadStateBean.isFromResume = isFromResume;
        mHandler.sendMessage(mHandler.obtainMessage(DOWNLOAD_CODE, downLoadStateBean));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int layoutId = setLayoutId();
        setContentView(layoutId);
        findViewById();
        mData = handleIntent(getIntent());
        setupView(mData);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initCommonConfig();
    }

    public void initCommonConfig() {
        mContentResolver = getContentResolver();
        mContentResolver.registerContentObserver(DownloadHelper.CONTENT_URI, true, mDownloadObserver);
        mDownloadHelper = new DownloadHelper(this);
        mDownloadId = getDownLoadId();
    }

    @Override
    protected void onResume() {
        super.onResume();
        addClick();
        sendHandleMessage(true);
        loadData(mData);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mContentResolver.unregisterContentObserver(mDownloadObserver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mData = handleIntent(intent);
        sendHandleMessage();
        setupView(mData);
    }

    @Override
    public void showDatas(W data) {
        //let child implements
    }

    @Override
    public void showProgress() {

    }

    @Override
    public void showNoNetConnectPage() {

    }

    @Override
    public void closeProgress() {

    }

    @Override
    public void closeNoNetConnectPage() {

    }

    @Override
    public void onClick(View v) {
        //let child implements
    }

    /**
     * @param
     * @return layout id
     * @description set layout id,invoke before method setContentView
     */
    public abstract int setLayoutId();

    /**
     * @param
     * @return
     * @description bind view
     */
    public abstract void findViewById();

    /**
     * @param
     * @return
     * @description display initialization UI
     */
    public abstract void setupView(T t);

    /**
     * @param
     * @return
     * @description set onClickListener
     */
    public abstract void addClick();

    /**
     * @param
     * @return intent data
     * @description get data from intent
     */
    public abstract T handleIntent(Intent intent);

    /**
     * @param t from intent data
     * @return
     * @description load data from server
     */
    public abstract void loadData(T t);

    /**
     * @param bytesAndStatus the message of handler reveive
     * @return
     * @description Set the bottom button status depending on the download status
     */
    protected void downloading(int[] bytesAndStatus) {
        DebugLog.w(TAG, "==============downloading:" + bytesAndStatus[0] + "/" +bytesAndStatus[1]);
        mProgressBar.setVisibility(View.VISIBLE);
        mDownloadBtn.setVisibility(View.GONE);
        mProgressBar.setMax(PROGRESS_MAX);
        mProgressBar.setProgress(PROGRESS_MIN);
        if (bytesAndStatus[1] >= 0) {
            mProgressBar.setMax(bytesAndStatus[1]);
            mProgressBar.setProgress(bytesAndStatus[0]);
//                if (message.arg1 >= message.arg2) {
//                    if (AppUtils.fileIsExists(mFilePath)) {
//                        mProgressBar.setVisibility(View.GONE);
//                        mDownloadBtn.setText(getString(R.string.themeclub_install));
//                    } else {
//                        mProgressBar.setVisibility(View.GONE);
//                        mDownloadBtn.setText(getString(R.string.themeclub_download_theme));
//                    }
//                }
        }
    }

    /**
     * @param isDownLoadSuccessful the flag for download successful or failed
     * @return
     * @description Set the bottom button status depending on the download status
     */
    protected void downloadSuccessful(boolean isDownLoadSuccessful, boolean isFromResume) {
        DebugLog.w(TAG, "==============downloadSuccessful:" + isDownLoadSuccessful);
        if (isDownLoadSuccessful) {
            mProgressBar.setVisibility(View.GONE);
            DebugLog.w(TAG, "==============checkIfDownSuccessByMD5:" + checkIfDownSuccessByMD5());
            if (checkInstalled()) {
                mDownloadBtn.setText(getString(R.string.themeclub_apply));
            } else if (checkIfDownSuccessByMD5()) {
                mDownloadBtn.setText(getString(R.string.themeclub_install));
            } else {
                mDownloadBtn.setText(getString(R.string.themeclub_download_theme));
            }
//            if (checkIfDownSuccessByMD5()) {
//                if (!checkInstalled()) {
//                    mDownloadBtn.setText(getString(R.string.themeclub_install));
//                } else {
//                    mDownloadBtn.setText(getString(R.string.themeclub_apply));
//                }
//            } else {
//                DebugLog.w(TAG, "==============checkInstalled:" + checkInstalled());
//                if (checkInstalled()) {
//                    mDownloadBtn.setText(getString(R.string.themeclub_apply));
//                } else {
//                    mDownloadBtn.setText(getString(R.string.themeclub_download_theme));
//                }
//            }
            mDownloadBtn.setVisibility(View.VISIBLE);
        } else {
            if (!isFromResume){
                ToastUtil.show(this,getString(R.string.themeclub_load_theme_or_lockscreen_error_notification));
            }
            mProgressBar.setVisibility(View.GONE);
            mDownloadBtn.setText(R.string.themeclub_download_theme);
            mDownloadBtn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * @description Set the bottom button status depending on the download status,it is not related to the download state
     */
    protected void unDownload() {
        DebugLog.w(TAG, "==============unDownload:");
        try {
            mProgressBar.setVisibility(View.GONE);
            if (checkInstalled()) {
                mDownloadBtn.setText(getString(R.string.themeclub_apply));
            } else {
                if (checkIfDownSuccessByMD5()) {
                    mDownloadBtn.setText(getString(R.string.themeclub_install));
                } else {
                    mDownloadBtn.setText(getString(R.string.themeclub_download_theme));
                }
            }
        } catch (Exception e) {
            DebugLog.e(TAG, "===============unDownload error:" + e);
        }
    }


    /**
     * @param
     * @return the downloadId of DownloadManager generated
     * @description get download id for progressbar
     */
    protected abstract long getDownLoadId();

    protected abstract boolean checkIfDownSuccessByMD5();

    protected abstract boolean checkInstalled();
}
