package com.amz.ios.search.utils;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Contacts;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.amz.ios.ioslite.common.util.PackageUtil;
import com.amz.ios.ioslite.common.util.ToastUtil;
import com.amz.ios.search.SearchActivity;
import com.amz.ios.search.entities.FileItemInfo;
import com.amz.ioslauncher.iossearch.R;

import java.util.List;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-21 下午8:07
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class IntentUtils {

    private static final String TAG = IntentUtils.class.getSimpleName();

    public static final String ACTION_DROI_APP = "com.zhuoyi.appDetailInfo";

    public static void newCall(Context context, String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            Uri data = Uri.parse("tel:".concat(phoneNumber));
            intent.setData(data);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, ">>>>>>IntentUtils#newCall : " + e.getMessage());
        }
    }

    public static void toContactDetail(Context context, String lookupKey, long contactId) {
        StringBuilder sb = new StringBuilder();
        sb.append("content://com.android.contacts/contacts/lookup/")
                .append(lookupKey)
                .append("/")
                .append(contactId);
        Uri personUri = Uri.parse(sb.toString());
        Intent contactIntent = new Intent();
        contactIntent.setData(personUri);
        contactIntent.setAction(Intent.ACTION_VIEW);
        context.startActivity(contactIntent);
    }

    public static void toContact(Context context) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Contacts.People.CONTENT_URI);
        context.startActivity(intent);
    }

    public static boolean toApp(Context context, ComponentName componentName) {
        try {
            Intent intent = new Intent();
            intent.setComponent(componentName);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, ">>>>>>IntentUtils#toApp : " + e.getMessage());
        }
        return false;
    }

    public static boolean toApp(Context context, String packageName, String className) {
        try {
            if (TextUtils.isEmpty(packageName)) {
                return false;
            }
            Intent intent;
            if (TextUtils.isEmpty(className)) {
                PackageManager manager = context.getPackageManager();
                intent = manager.getLaunchIntentForPackage(packageName);
            } else {
                intent = Intent.makeMainActivity(new ComponentName(packageName, className));
            }
            if (intent == null) {
                return false;
            }
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, ">>>>>>IntentUtils#toApp : " + e.getMessage());
        }
        return false;
    }

    public static void toDroiAppDetail(Context context, String apkid, String packageName) {
        try {
            int id = Integer.valueOf(apkid);
            Intent localIntent = new Intent(ACTION_DROI_APP);
            localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            localIntent.putExtra("refId", id); // appID
            localIntent.putExtra("packageName", packageName);
            localIntent.putExtra("from_path", "TydLauncher"); // "TydLauncher"
            context.startActivity(localIntent);
        } catch (ActivityNotFoundException e) {
            handDroiMarketNotFound(context);
        } catch (NumberFormatException e) {
            ToastUtil.show(context, R.string.fmsearch_apkid_error);
        }
    }

    /**
     * 调用的是系统的音乐播放器
     */
    public static void openMusic(Context context, long id, String path, String minetype) {
        try {
            Intent musicIntent = new Intent(Intent.ACTION_VIEW);
            if (!TextUtils.isEmpty(minetype)) {
                minetype = "audio/*";
                //add this will jump to playlist
                //"vnd.android.cursor.dir/playlist";
            }
//            musicIntent.setDataAndType(ContentUris.withAppendedId(MediaStore.Audio.Media.INTERNAL_CONTENT_URI,id), minetype);
            musicIntent.setDataAndType(Uri.parse("file://" + path), minetype);
            musicIntent.putExtra("withtabs", true); // 显示tab选项卡
            context.startActivity(musicIntent);
        } catch (Exception e) {
            Log.e(TAG, ">>>>>>IntentUtils#openMusic : " + e.getMessage());
        }
    }

    public static void toMusicPlay(Context context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 15) {
                Intent intent = new Intent("android.intent.action.MUSIC_PLAYER");//Min SDK 8
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                context.startActivity(intent);
            }
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, ">>>>>>IntentUtils#toMusicPlay : " + e.getMessage());
        }
    }

    public static void openUrl(Context context, String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//Min SDK 15
        context.startActivity(intent);
    }

    public static void openFile(Context context, FileItemInfo fileItemInfo) {
        try {
            Uri uri = Uri.parse(fileItemInfo.filePath);
            String strExtension = fileItemInfo.filePath.substring(fileItemInfo.filePath.lastIndexOf(".") + 1);
            String strMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(strExtension);
            Uri testUri = FileProvider.getUriForFile(context, "com.amz.ios.search.fileprovider", fileItemInfo.file);

            if (strMimeType != null && strMimeType != "" ) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(testUri, strMimeType);

                PackageManager packageManager = context.getPackageManager();
                List<ResolveInfo> list = packageManager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY);
                if (list.size() > 0) {
                    context.startActivity(intent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handDroiMarketNotFound(final Context context) {
        if (context == null) return;
        final String message = context.getResources().getString(PackageUtil.isAppInstalled(context, "com.zhuoyi.market")
                ? R.string.fmsearch_download_market_update_message : R.string.fmsearch_download_market_warning_message);
        final String title = context.getResources().getString(R.string.fmsearch_tips);
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadApp(context, "http://qrscan.zhuoyi.com/pro");
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    public static final void toIOSSearch(Context context, String key) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.putExtra(SearchActivity.SEARCH_INTENT_KEYWORD, key);
        context.startActivity(intent);
    }

    public static final void downloadApp(Context context, String url) {
        try {
            Intent webIntent = new Intent(Intent.ACTION_VIEW);
            webIntent.setData(Uri.parse(url));
            webIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(webIntent);
        } catch (ActivityNotFoundException e) {
            ToastUtil.show(context, R.string.fmsearch_activity_not_found);
        }
    }
}
