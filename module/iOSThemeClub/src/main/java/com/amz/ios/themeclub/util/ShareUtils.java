package com.amz.ios.themeclub.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;

import com.amz.ios.ioslite.common.util.BuildUtil;
import com.amz.ios.themeclub.R;

import java.io.File;

/**
 * Created by lideqian on 16-11-18.
 */
public class ShareUtils {
    private static final String FILE_PATH_AUTHOITY = "com.amz.ios.themeclub.file.provider";

    public static void ShareText(Context context ,String s) {

        Intent intent=new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.themeclub_share_extra_subject));
        intent.putExtra(Intent.EXTRA_TEXT, s);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, s));
    }


    public static void SharePic(Context context,String activityTitle,
                         String imgPath) {
        Intent intent = new Intent(Intent.ACTION_SEND);
            File f = new File(imgPath);
        if (f.exists() && f.isFile()) {
                intent.setType("image/jpg");
            Uri uri;
            if (BuildUtil.ATLEAST_NOUGAT) {
                uri = FileProvider.getUriForFile(context, FILE_PATH_AUTHOITY, f);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(f);
            }
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent,activityTitle));
    }

}
