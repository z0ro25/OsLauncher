package com.ios.cleanwidget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.amz.ios.launcher.R;
import com.amz.ios.launcher.views.CustomTextView;


public class CleanResultDialog extends Dialog {
    private Context mContext;
    private static boolean isResultDialogShowing = false;

    public CleanResultDialog(final Context context, Long value) {
        super(context, R.style.CleanResultDialog);
        mContext = context;

        getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        getWindow().setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.y = context.getResources().getDimensionPixelOffset(R.dimen.dialog_y_offset);


        setContentView(R.layout.clean_ad_layout);
        setCanceledOnTouchOutside(true);

        CustomTextView title = (CustomTextView) findViewById(R.id.title);
        String content;
        if (value <= 0) {
            content = context.getString(R.string.dialog_clean_memory_fullly);
        } else {
            content = context.getString(R.string.dialog_clean_memory, value);
        }
        title.setText(content);

        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isResultDialogShowing = false;
            }
        });
    }

    public static boolean isResultShowing() {
        return isResultDialogShowing;
    }

    @Override
    public void show() {
        isResultDialogShowing = true;
        super.show();
    }

}
