package com.amz.ios.themeclub.tools;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.util.NetworkStateUtil;
import com.amz.ios.ioslite.common.util.ToastUtil;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.ThemeClubApplication;
import com.amz.ios.themeclub.bean.LockScreenBean;
import com.amz.ios.themeclub.bean.ThemesBean;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.PreferencesUtils;

import java.io.File;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * Created by lideqian on 16-11-19.
 */
public class DownloadHelper {

    public static class DownLoadStateBean{
        public int[] bytesAndStatus;
        public boolean isFromResume;
    }

    private final String TAG = getClass().getSimpleName();

    public static final Uri CONTENT_URI = Uri.parse("content://downloads/my_downloads");
    /** represents downloaded file above api 11 **/
    public static final String COLUMN_LOCAL_FILENAME = "local_filename";
    /** represents downloaded file below api 11 **/
    public static final String COLUMN_LOCAL_URI = "local_uri";

    private DownloadManager downloadManager;
    private long lastClick;
    private Context mContext;

    public DownloadHelper(Context context) {
        downloadManager = (DownloadManager)context.getSystemService(DOWNLOAD_SERVICE);
        this.mContext = context;
    }

    /**
     * get download status
     *
     * @param downloadId
     * @return
     */
    public int getStatusById(long downloadId) {
        return getInt(downloadId, DownloadManager.COLUMN_STATUS);
    }

    /**
     * get downloaded byte, total byte
     *
     * @param downloadId
     * @return a int array with two elements
     *         <ul>
     *         <li>result[0] represents downloaded bytes, This will initially be
     *         -1.</li>
     *         <li>result[1] represents total bytes, This will initially be -1.</li>
     *         </ul>
     */
    public int[] getDownloadBytes(long downloadId) {
        int[] bytesAndStatus = getBytesAndStatus(downloadId);
        return new int[] { bytesAndStatus[0], bytesAndStatus[1] };
    }

    /**
     * get downloaded byte, total byte and download status
     *
     * @param downloadId
     * @return a int array with three elements
     *         <ul>
     *         <li>result[0] represents downloaded bytes, This will initially be
     *         -1.</li>
     *         <li>result[1] represents total bytes, This will initially be -1.</li>
     *         <li>result[2] represents download status, This will initially be
     *         0.</li>
     *         </ul>
     */
    @SuppressLint("Range")
    public int[] getBytesAndStatus(long downloadId) {
        DebugLog.w(TAG, "===============getBytesAndStatus:" + downloadId);
        int[] bytesAndStatus = new int[] { -1, -1, 0 };
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor c = null;
        try {
            c = downloadManager.query(query);
            if (c != null && c.moveToFirst()) {
                bytesAndStatus[0] = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                bytesAndStatus[1] = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                bytesAndStatus[2] = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return bytesAndStatus;
    }


    public int getErrorCode(long downloadId) {
        return getInt(downloadId, DownloadManager.COLUMN_REASON);
    }

    /**
     * get string column
     *
     * @param downloadId
     * @param columnName
     * @return
     */
    @SuppressLint("Range")
    private String getString(long downloadId, String columnName) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        String result = null;
        Cursor c = null;
        try {
            c = downloadManager.query(query);
            if (c != null && c.moveToFirst()) {
                result = c.getString(c.getColumnIndex(columnName));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }

    /**
     * get int column
     *
     * @param downloadId
     * @param columnName
     * @return
     */
    @SuppressLint("Range")
    private int getInt(long downloadId, String columnName) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        int result = -1;
        Cursor c = null;
        try {
            c = downloadManager.query(query);
            if (c != null && c.moveToFirst()) {
                result = c.getInt(c.getColumnIndex(columnName));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }


    public static boolean isDownloading(int downloadManagerStatus) {
        return downloadManagerStatus == DownloadManager.STATUS_RUNNING
                || downloadManagerStatus == DownloadManager.STATUS_PAUSED
                || downloadManagerStatus == DownloadManager.STATUS_PENDING;
    }


    public void download(ThemesBean themeInfo) {
        if(!NetworkStateUtil.isNetworkConnected(mContext)) {
            Toast.makeText(mContext,mContext.getString(R.string.themeclub_try_again),Toast.LENGTH_LONG).show();
            return;
        }
        long current = System.currentTimeMillis();
        if(current - lastClick < 1500){
            return ;
        }
        if(themeInfo==null){
            return;
        }
        lastClick = current;
        if(AppUtils.checkInstalled(mContext,themeInfo.getPackageName())) {
            ToastUtil.show(ThemeClubApplication.getContext(),mContext.getString(R.string.themeclub_hasinstalled));
            return;
        }
        ToastUtil.show(ThemeClubApplication.getContext(), R.string.themeclub_start_download);
        File folder = new File(AppUtils.getSDPath() + "/themes/");
        if (!folder.exists() || !folder.isDirectory()) {
            folder.mkdirs();
        }
        long downloadId;
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(themeInfo.getDownloadUrl()));
            request.setDestinationInExternalPublicDir("themes", themeInfo.getName() + themeInfo.getId() + ".apk"); //error
            request.setTitle("");
            request.setDescription("");
            request.setVisibleInDownloadsUi(false);
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            downloadId = downloadManager.enqueue(request);
            PreferencesUtils.putLong(mContext, ((Integer) themeInfo.getId()).toString(), downloadId);
        } catch (Exception e) {
            Log.e(TAG, "==================download Theme error:" + e);
        }
    }

    public void downloadLock(LockScreenBean lockScreenBean, String filePath) {
        if(!NetworkStateUtil.isNetworkConnected(mContext)) {
            Toast.makeText(mContext,mContext.getString(R.string.themeclub_try_again),Toast.LENGTH_LONG).show();
            return;
        }
        long current = System.currentTimeMillis();
        if(current - lastClick < 1500){
            return ;
        }
        if(lockScreenBean==null){
            return;
        }
        lastClick = current;
        if(AppUtils.checkInstalled(mContext,lockScreenBean.getPackageName())) {
            ToastUtil.show(ThemeClubApplication.getContext(),mContext.getString(R.string.themeclub_hasinstalled));
            return;
        }
        ToastUtil.show(ThemeClubApplication.getContext(), R.string.themeclub_start_download);
        File folder = new File(AppUtils.getSDPath() + "/lock/");
        if (!folder.exists() || !folder.isDirectory()) {
            folder.mkdirs();
        }
        if (filePath != null) {
            final File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        }
        long downloadId;
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(lockScreenBean.getDownloadUrl()));
            request.setDestinationInExternalPublicDir("lock", lockScreenBean.getPackageName() + lockScreenBean.getId() + ".apk"); //error
            request.setTitle("");
            request.setDescription("");
            request.setVisibleInDownloadsUi(false);
            request.setMimeType("application/vnd.android.package-archive");
            downloadId = downloadManager.enqueue(request);
            PreferencesUtils.putLong(mContext, ((Integer) lockScreenBean.getId()).toString(), downloadId);
        } catch (Exception e) {
            Log.e(TAG, "==================download lockScreen error:" + e);
        }
    }
}
