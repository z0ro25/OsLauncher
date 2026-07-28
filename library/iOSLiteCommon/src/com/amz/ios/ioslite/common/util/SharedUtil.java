package com.amz.ios.ioslite.common.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.ioslite.common.R;

/**
 * Created by greg on 16-11-15.
 */

public class SharedUtil {

    public static void accessDroiFacebookMainPage(Context context) {
        String url = Partner.getString(context, Partner.PRODUCT_FACEBOOK_WEBSITE);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        CommonUtilities.startActivitySafely(context, intent);
    }

    public static void accessWebsite(Context context) {
        String url = Partner.getString(context, Partner.PRODUCT_OFFICAL_WEBSITE);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        CommonUtilities.startActivitySafely(context, intent);
    }


    public static void shareSoftware(Context context, String title, String content) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, content);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("text/plain");
        CommonUtilities.startActivitySafely(context, Intent.createChooser(intent, title));
    }


    public static void sendEmailToUs(Context context) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        String uri = "mailto:" + Partner.getString(context, Partner.PRODUCT_EMAIL);
        intent.setData(Uri.parse(uri));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, R.string.error_email_not_login, Toast.LENGTH_SHORT).show();
        }
    }

}
