package com.amz.ios.search.provider;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import com.amz.ios.ioslite.common.AsyncHandler;
import com.amz.ios.ioslite.common.ad.IOSAdConfig;
import com.amz.ios.ioslite.common.ad.IOSAdError;
import com.amz.ios.ioslite.common.ad.IOSAdManager;
import com.amz.ios.ioslite.common.ad.IOSAppAd;
import com.amz.ios.ioslite.common.ad.IOSAppAdListener;
import com.amz.ios.ioslite.common.ad.IOSNAdResponse;
import com.amz.ios.ioslite.common.ad.IOSNativeAd;
import com.amz.ios.ioslite.common.ad.IOSNativeAdListener;
import com.amz.ios.ioslite.common.util.CommonUtilities;
import com.amz.ios.http.Internal.Action;
import com.amz.ios.http.Internal.BaseProvider;
import com.amz.ios.http.Internal.CancelableCallBack;
import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.ItemInfo;
import com.amz.ios.launcher.LauncherModel;
import com.amz.ios.search.config.MSCConfiguration;
import com.amz.ios.search.entities.AdCardItemInfo;
import com.amz.ios.search.entities.AppCardInfo;
import com.amz.ios.search.entities.ContactItemInfo;
import com.amz.ios.search.entities.FileItemInfo;
import com.amz.ios.search.entities.LauncherAppInfo;
import com.amz.ios.search.entities.MusicCardItemInfo;
import com.amz.ios.search.filesearcher.FileSearcher;
import com.amz.ios.search.filesearcher.searchengine.FileItem;
import com.amz.ios.search.utils.PinyinUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-15 下午3:53
 */
public class DataFlowProvider extends BaseProvider {

    private static final String TAG = DataFlowProvider.class.getSimpleName();

    private static final boolean DEBUG = MSCConfiguration.DEBUG;

    private Map<String, Integer> mTaskRef = null;

    private PinyinUtils mPininUtils = null;

    private Comparator<ContactItemInfo> mContactItemInfoComparator;
    private SparseArray<String> mEventIdList;
    private final String SYSTEM_PATH = "system";
    private IOSAppAd mAppAd;
    private IOSNativeAd mNativiAd;
    private final int REQUEST_APP_COUNT = 8;
    private IOSAppAdListener mRecommendAppListener;
    private IOSNativeAdListener mNativiAdListener;

    public DataFlowProvider(Context context) {
        super(context);
        mTaskRef = new HashMap<>();
        mPininUtils = PinyinUtils.getInstance(context);
    }

    @Override
    protected void onDestroy() {
        mTaskRef.clear();
        mTaskRef = null;
        mAllApps.clear();
        if (mAppAd != null) {
            mAppAd.destory();
        }
        mRecommendAppListener = null;
        if (mNativiAd != null) {
            mNativiAd.destory();
        }
        mNativiAdListener = null;
    }

    //load recommod app
    public Action loadRecommondApp() {
        // 获取应用广告对象
        if (mAppAd == null) {
            mAppAd = IOSAdManager.getInstance(getContext()).getAppAd(IOSAdConfig.ID_APP_RECOMMEND_NEWSPAGE);
        }
        String name = "loadRecommondApp";
        final String taskName = checkAndgetRefName(name);
        Action recommonAppAction = new Action<List<AppCardInfo>>(this, taskName) {
            @Override
            public void work(final CancelableCallBack<List<AppCardInfo>> callBack) {
                if (mRecommendAppListener == null) {
                    mRecommendAppListener = new IOSAppAdListener() {
                        @Override
                        public void onError(IOSAdError error) {
                            callBack.onFalure("fail to fetch apps :" + error, 0);
                        }

                        @Override
                        public void onAdLoaded(List<? extends IOSNAdResponse> responses) {
                            if (responses != null && responses.size() != 0) {
                                if (responses.size() < 8) {
                                    mAppAd.load(REQUEST_APP_COUNT);
                                } else {
                                    List<View> adViewList = new ArrayList<View>();
                                    adViewList.clear();
                                    for (int i = 0; i < responses.size(); i++) {
                                        adViewList.add(responses.get(i).getAdContentView());
                                    }
                                    callBack.onRealSucess(adapteToAppCardInfo(adViewList));
                                }

                            }
                        }
                    };
                }

                if (mAppAd != null) {
                    mAppAd.setAppAdListener(mRecommendAppListener);
                    mAppAd.load(REQUEST_APP_COUNT);
                }
            }
        };
        return recommonAppAction;
    }

    private List<AppCardInfo> adapteToAppCardInfo(List<View> adViews) {
        List<AppCardInfo> appCardsInfo = new ArrayList<>();
        for (int i = 0; i < adViews.size(); i++) {
            AppCardInfo app = new AppCardInfo();
            app.adView = adViews.get(i);
            appCardsInfo.add(app);
        }
        return appCardsInfo;
    }

    private String checkAndgetRefName(String name) {
        Integer ref = mTaskRef.get(name);
        if (ref == null) {
            ref = 0;
        }
        mTaskRef.put(name, ref + 1);
        return name.concat("-").concat(String.valueOf(ref));
    }

    //load contact
    @SuppressLint("Range")
    public Action loadContact(final String keyWord) {

        if (DEBUG) Log.d(TAG, ">>>>>>DataFlowProvider#loadContact : start load!");
        String name = "loadContact";
        final String taskName = checkAndgetRefName(name);
        final int limit = MSCConfiguration.MUSIC_SHOW_NUMBER;
        Action<List<ContactItemInfo>> contactItemInfoAction = new Action<List<ContactItemInfo>>(this, taskName) {
            @Override
            protected void work(CancelableCallBack<List<ContactItemInfo>> callBack) {
                if (DEBUG) {
                    Log.d(TAG, ">>>>>>DataFlowProvider#work : get contact info with keyword " + keyWord);
                }
                if (keyWord.isEmpty()) {
                    if (callBack != null) callBack.onFalure("", 0);
                   return;
                }
                Cursor lookUpCursor = null;
                Cursor personCursor = null;
                Cursor contactCursor = null;
                ContentResolver cntr = null;
                long begin = System.currentTimeMillis();
                try {
                    StringBuilder selection = new StringBuilder();
                    String[] selectionArgs = null;
                    if (!TextUtils.isEmpty(keyWord)) {
                        final String allStr = "%";
                        //filter title
                        selection.append(ContactsContract.Contacts.DISPLAY_NAME).append(" like ?").append(" and ").append(" has_phone_number = ?");
                        selectionArgs = new String[]{allStr.concat(keyWord).concat(allStr), "1"};
                    }
                    String[] projection = {ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.LAST_TIME_CONTACTED};
                    List<ContactItemInfo> contactItemInfos = null;
                    cntr = getContext().getContentResolver();
                    lookUpCursor = cntr.query(ContactsContract.Contacts.CONTENT_URI, projection, selection.toString(), selectionArgs
                            , ContactsContract.Contacts.LAST_TIME_CONTACTED + " desc ");
                    if (lookUpCursor != null && lookUpCursor.moveToFirst()) {
                        contactItemInfos = new ArrayList<>();
                        ContactItemInfo item;
                        int i = 0;
                        do {
                            item = new ContactItemInfo();
                            item.contactId = lookUpCursor.getLong(lookUpCursor.getColumnIndex(ContactsContract.Contacts._ID));
                            item.name = lookUpCursor.getString(lookUpCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                            item.lookupkey = lookUpCursor.getString(lookUpCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                            item.lastTimeConnected = lookUpCursor.getLong(lookUpCursor.getColumnIndex(ContactsContract.Contacts.LAST_TIME_CONTACTED));
                            if (mQuit) {
                                Log.d(TAG, ">>>>>>DataFlowProvider : " + getName() + " is canceled");
                                if (callBack != null) callBack.onFalure("", 0);
                                return;
                            }
                            // Phone info are stored in the ContactsContract.Data table
                            selection.delete(0, selection.length());
                            selection.append(ContactsContract.Contacts.LOOKUP_KEY).append(" = ?");
                            projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
                            selectionArgs = new String[]{item.lookupkey};
                            personCursor = cntr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selection.toString(), selectionArgs, null);
                            if (personCursor != null && personCursor.moveToNext()) {
                                item.phoneNumber = personCursor.getString(0);
                            }
                            if (mQuit) {
                                Log.d(TAG, ">>>>>>DataFlowProvider : " + getName() + " is canceled");
                                if (callBack != null) callBack.onFalure("", 0);
                                return;
                            }
                            contactItemInfos.add(item);
                            i += 1;
                        } while (lookUpCursor.moveToNext() && i < limit);
                    }
                    Log.d(TAG, ">>>>>>DataFlowProvider#work : query key info takes : " + (System.currentTimeMillis() - begin) + "ms");
                    if (mQuit) {
                        Log.d(TAG, ">>>>>>DataFlowProvider : " + getName() + " is canceled");
                        if (callBack != null) callBack.onFalure("", 0);
                        return;
                    }
                    //sort app
                    if (mContactItemInfoComparator == null) {
                        mContactItemInfoComparator = new Comparator<ContactItemInfo>() {
                            @Override
                            public int compare(ContactItemInfo lhs, ContactItemInfo rhs) {
                                return lhs.lastTimeConnected >= rhs.lastTimeConnected ? -1 : 1;
                            }
                        };
                    }
                    Collections.sort(contactItemInfos, mContactItemInfoComparator);
                    if (callBack != null) callBack.onRealSucess(contactItemInfos);
                } catch (Exception e) {
                    Log.e(TAG, ">>>>>>DataFlowProvider#work :" + e.getMessage());
                    if (callBack != null) callBack.onFalure(e.getMessage(), 0);
                } finally {
                    CommonUtilities.close(lookUpCursor);
                    CommonUtilities.close(personCursor);
                    CommonUtilities.close(contactCursor);
                }
            }
        };
        return contactItemInfoAction;
    }

    //load ad
    public Action loadAd() {
        if (mNativiAd == null) {
            mNativiAd = IOSAdManager.getInstance(getContext()).getNativeAd(IOSAdConfig.ID_SEARCH);
        }
        String name = "loadAd";
        final String taskName = checkAndgetRefName(name);
        final Action<List<AdCardItemInfo>> adItemInfoAction = new Action<List<AdCardItemInfo>>(this, taskName) {
            @Override
            protected void work(final CancelableCallBack<List<AdCardItemInfo>> callBack) {
                if (callBack == null) return;
                if (mNativiAdListener == null) {
                    mNativiAdListener = new IOSNativeAdListener() {
                        @Override
                        public void onError(IOSAdError error) {

                        }

                        @Override
                        public void onAdLoaded(IOSNAdResponse response) {
                            if (response == null && response.getAdContentView() == null) {
                                callBack.onFalure("", 0);
                            }
                            AdCardItemInfo itemInfo = new AdCardItemInfo(response.getAdContentView());
                            List<AdCardItemInfo> adCardItemInfos = new ArrayList<AdCardItemInfo>();
                            adCardItemInfos.add(itemInfo);
                            callBack.onRealSucess(adCardItemInfos);
                        }

                        @Override
                        public void onClick() {

                        }
                    };
                }
                if (mNativiAd != null) {
                    mNativiAd.setNativeAdListener(mNativiAdListener);
                    AsyncHandler.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mNativiAd.load();
                        }
                    });
                }
            }
        };
        holdAction(adItemInfoAction);
        return adItemInfoAction;
    }

    //load music
    public Action loadMusic(final String keyWord) {
        if (DEBUG) Log.d(TAG, ">>>>>>DataFlowProvider#loadMusic : start load!");
        String name = "loadMusic";
        final String taskName = checkAndgetRefName(name);
        Action<List<MusicCardItemInfo>> contactItemInfoAction = new Action<List<MusicCardItemInfo>>(this, taskName) {
            @Override
            protected void work(CancelableCallBack<List<MusicCardItemInfo>> callBack) {
                if (DEBUG) {
                    Log.d(TAG, ">>>>>>DataFlowProvider#work : get contact info");
                }
                if (TextUtils.isEmpty(keyWord)) {
                    if (callBack != null) {
                        callBack.onFalure("", 0);
                    }
                    return;
                }
                String selection = null;
                String[] selectionArgs = null;
                String[] projection = new String[]{
                        MediaStore.Audio.AudioColumns._ID,
                        MediaStore.Audio.AudioColumns.DATA,
                        MediaStore.Audio.AudioColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ALBUM,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AudioColumns.MIME_TYPE,
                        MediaStore.Audio.AudioColumns.ALBUM_KEY
                };
                if (!TextUtils.isEmpty(keyWord)) {
                    final String allStr = "%";
                    //filter title
                    selection = MediaStore.Audio.AudioColumns.TITLE.concat(" like ? ");
                    selectionArgs = new String[]{allStr.concat(keyWord).concat(allStr)};
                }
                List<MusicCardItemInfo> musicCardItemInfos = null;
                Cursor internalMusicCursor = null;
                Cursor outerMusicCursor = null;
                try {
                    musicCardItemInfos = new ArrayList<>();
                    internalMusicCursor = getContext().getContentResolver().query(
                            MediaStore.Audio.Media.INTERNAL_CONTENT_URI, projection, selection, selectionArgs,
                            null);
                    fillMusicData(musicCardItemInfos, internalMusicCursor, true);
                    outerMusicCursor = getContext().getContentResolver().query(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs,
                            null
                    );
                    fillMusicData(musicCardItemInfos, outerMusicCursor, false);
                    if (callBack != null) {
                        callBack.onRealSucess(musicCardItemInfos);
                    }
                } catch (Exception e) {
                    Log.e(TAG, ">>>>>>SearchDataProvider#work : + " + e.getMessage());
                    callBack.onFalure("", 0);
                } finally {
                    CommonUtilities.close(internalMusicCursor);
                    CommonUtilities.close(outerMusicCursor);
                }
            }
        };
        return contactItemInfoAction;
    }

    @SuppressLint("Range")
    private void fillMusicData(List<MusicCardItemInfo> musicCardItemInfos, Cursor musicCursor, boolean internal) {
        MusicCardItemInfo itemInfo = null;
        for (musicCursor.moveToFirst(); !musicCursor.isAfterLast(); musicCursor
                .moveToNext()) {

            String path = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            if (path == null || path.contains(SYSTEM_PATH)) {
                continue;
            }
            itemInfo = new MusicCardItemInfo();

            // 取得音乐播放路径
            itemInfo.musicPath = path;

            itemInfo.musicLocal = internal ? MusicCardItemInfo.LOCAL_INTERNAL : MusicCardItemInfo.LOCAL_EXTERNAL;

            itemInfo.musicId = musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media._ID));


            // 取得音乐的名字
            itemInfo.musicName = musicCursor.getString(musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.TITLE));

            // 取得音乐的专辑名称
            itemInfo.musicAlbum = musicCursor.getString(musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.ALBUM));

            // 取得音乐的演唱者
            itemInfo.musicArtist = musicCursor.getString(musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.ARTIST));

            //music type
            itemInfo.musicMineType = musicCursor.getString(musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));

            // 取得歌曲对应的专辑对应的Key
            itemInfo.musicAlbumKey = musicCursor.getString(musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY));

            String[] argArr = {itemInfo.musicAlbumKey};
            ContentResolver albumResolver = getContext().getContentResolver();
            Cursor albumCursor = albumResolver.query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.AlbumColumns.ALBUM_ART},
                    MediaStore.Audio.AudioColumns.ALBUM_KEY + " = ?",
                    argArr, null);

            if (null != albumCursor && albumCursor.getCount() > 0) {
                albumCursor.moveToFirst();
                int albumArtIndex = albumCursor
                        .getColumnIndex(MediaStore.Audio.AlbumColumns.ALBUM_ART);
                itemInfo.musicAlbumArtPath = albumCursor.getString(albumArtIndex);
            }
            if (TextUtils.isEmpty(itemInfo.musicAlbumArtPath)) {
                // 没有专辑定义，给默认图片
            }
            musicCardItemInfos.add(itemInfo);
        }
    }

    //load local app
    public Action loadLocalApp(final String keyWord) {
        String name = "loadLocalApp";
        Log.d(TAG, ">>>>>>DataFlowProvider#loadLocalApp : start load!");
        final String taskName = checkAndgetRefName(name);
        Action<List<AppCardInfo>> appCardInfoAction = new Action<List<AppCardInfo>>(this, taskName) {
            @Override
            protected void work(CancelableCallBack<List<AppCardInfo>> callBack) {
                if (mAllApps.isEmpty()) {
                    getAllApps();
                }
                if (keyWord.isEmpty()) {
                    if (callBack != null) callBack.onFalure("", 0);
                    return;
                }
                if (mQuit) {
                    Log.d(TAG, ">>>>>>DataFlowProvider : " + getName() + " is canceled");
                    if (callBack != null) callBack.onFalure("", 0);
                    return;
                }
                List<AppCardInfo> containsApps = filterApp(keyWord);
                if (isCanceled()) {
                    return;
                }
                if (callBack != null) {
                    callBack.onRealSucess(containsApps);
                }
            }
        };
        return appCardInfoAction;
    }

    //load local app
    public Action loadLocalFile(final String keyWord) {
        String name = "loadLocalFile";
        Log.d(TAG, ">>>>>>DataFlowProvider#loadLocalFile : start load!");
        final String taskName = checkAndgetRefName(name);
        Action<List<FileItemInfo>> fileSearchAction = new Action<List<FileItemInfo>>(this, taskName) {
            @Override
            protected void work(final CancelableCallBack<List<FileItemInfo>> callBack) {
                if (keyWord.isEmpty()) {
                    if (callBack != null) callBack.onFalure("", 0);
                    return;
                }
                if (mQuit) {
                    Log.d(TAG, ">>>>>>DataFlowProvider : " + getName() + " is canceled");
                    if (callBack != null) callBack.onFalure("", 0);
                    return;
                }

                FileSearcher fileSearcher = new FileSearcher(getContext());
                fileSearcher
                        .withKeyword(keyWord)
                        .search(new FileSearcher.FileSearcherCallback() {
                            @Override
                            public void onSelect(List<FileItem> files) {
                                if (callBack != null) {
                                    final List<FileItemInfo> containsFiles = new ArrayList<>();
                                    for (FileItem file: files) {
                                        FileItemInfo fileInfo = new FileItemInfo(file);
                                        containsFiles.add(fileInfo);
                                    }
                                    callBack.onRealSucess(containsFiles);
                                }
                                Log.d(TAG, "complete search");
                            }
                        });

                if (isCanceled()) {
                    return;
                }
            }
        };
        return fileSearchAction;
    }


    private static final String OTA2_PACKAGE_NAME = "com.zhuoyi.security.service";
    private final List<AppCardInfo> mAllApps = new ArrayList<>();
    private List<LauncherAppInfo> mLauncherAppInfos = null;

    private List<AppCardInfo> filterApp(String keyWord) {
        final List<LauncherAppInfo> allApps = mLauncherAppInfos;
        final List<AppCardInfo> containsApps = new ArrayList<>();
        if (allApps == null || allApps.size() == 0) {
            if (mAllApps != null && !mAllApps.isEmpty()) {
                for (AppCardInfo app : mAllApps) {

                    if (isHiddenApp(app.packageName)) {
                        continue;
                    }
                    if (app.name.toLowerCase().contains(keyWord.toLowerCase())){ //changed by Hong
                        containsApps.add(app);
                    }
                }
            }
        } else {
            AppCardInfo cardInfo;
            for (LauncherAppInfo app : allApps) {
                if (app.getLabel().toString().toLowerCase().contains(keyWord.toLowerCase())){ //changed by Hong
                    cardInfo = new AppCardInfo(AppCardInfo.FLAG_LOCAL_APP);
                    cardInfo.lancherApp = app;
                    containsApps.add(cardInfo);
                }

//                if (mPininUtils.contains(app.getLabel().toString(), keyWord)) {
//                    cardInfo = new AppCardInfo(AppCardInfo.FLAG_LOCAL_APP);
//                    cardInfo.lancherApp = app;
//                    containsApps.add(cardInfo);
//                }
            }
        }
        if (DEBUG) Log.d(TAG, ">>>>>>DataFlowProvider#filterApp : " + containsApps);
        return containsApps;
    }

    private void getAllApps() {
        //if no mem load
        PackageManager pm = getContext().getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(pm));
        AppCardInfo item;
        ResolveInfo info;
        final int size = resolveInfos.size();
        for (int i = 0; i < size; i++) {
            info = resolveInfos.get(i);
            if (OTA2_PACKAGE_NAME.equals(info.activityInfo.packageName)) {
                continue;
            }

            String packageName = getContext().getPackageName();//changed by Hong
            if (packageName.equals(info.activityInfo.packageName)){
                continue;
            }

            item = new AppCardInfo(AppCardInfo.FLAG_LOCAL_APP);
            item.packageName = info.activityInfo.packageName;
            item.name = info.loadLabel(pm).toString();
            item.componentName = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            item.mIcon = info.activityInfo;
            mAllApps.add(item);
        }
        if (DEBUG) Log.d(TAG, ">>>>>>DataFlowProvider#getAllApps : " + mAllApps);
    }

    private boolean isHiddenApp(String packageName) {
        Iterator<ItemInfo> it = LauncherModel.sHdItemsIdMap.values().iterator();
        while (it.hasNext()) {
            ItemInfo next = it.next();
            if (next instanceof AppInfo) {
                AppInfo applicationInfo = (AppInfo)next;
                if (packageName.equals(applicationInfo.getPackageName())) {
                    return true;
                }
            }
        }

        return false;
    }
}
