package com.amz.ios.ioslite.common.util;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amz.ios.ioslite.common.R;

public class IOSToastUtil {

    private static Toast mToast;

    private static void resetToast(Context context) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = new Toast(context);
    }

    /**
     * Make a ios toast that just contains a text view.
     *
     * @param context  The context to use.  Usually your {@link android.app.Application}
     *                 or {@link android.app.Activity} object.
     * @param message  The text to show.  Can be formatted text.
     * @param duration How long to display the message.  Either {@link Toast.LENGTH_SHORT} or
     *                 {@link Toast.LENGTH_LONG}
     * @param top      the location on screen , top or bottom;
     */
    public static void showToast(Context context, String message, int iconResId, int duration, boolean top) {
        resetToast(context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.toast_layout, null);
        TextView title = (TextView) layout.findViewById(R.id.title);
        ImageView icon = (ImageView) layout.findViewById(R.id.icon);

        title.setText(message);
        if (iconResId > 0) {
            icon.setImageResource(iconResId);
            icon.setVisibility(View.VISIBLE);
        }

        if (top) {
            mToast.setGravity(Gravity.TOP, 0, 150);
        } else {
            mToast.setGravity(Gravity.BOTTOM, 0, 150);
        }

        mToast.setDuration(duration);
        mToast.setView(layout);
        mToast.show();
    }

    public static void showToast(Context context, String message, int resId, int duration) {
        showToast(context, message, resId, duration, false);
    }

    public static void showToast(Context context, String message) {
        showToast(context, message, 0, Toast.LENGTH_SHORT, false);
    }

}
