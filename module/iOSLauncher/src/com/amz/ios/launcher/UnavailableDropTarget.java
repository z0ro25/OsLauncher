package com.amz.ios.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;

import com.amz.ios.ioslite.common.Partner;
import com.amz.ios.launcher.folder.FolderLayout;
import com.amz.ios.launcher.views.CustomTextView;


public class UnavailableDropTarget extends ButtonDropTarget {
    private CustomTextView mPromptText;

    public UnavailableDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UnavailableDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPromptText = (CustomTextView) findViewById(R.id.unavaliable_prompt);
    }

    public boolean acceptDrop(DragObject dragObject) {
        return false;
    }

    @Override
    protected boolean supportsDrop(DragSource source, Object info) {
        if (!Partner.getBoolean(mLauncher, Partner.FEATURE_DROP_UNAVAILABLE_ENABLE)) {
            return false;
        }

        if (info instanceof FolderInfo) {
            mPromptText.setText(mLauncher.getString(R.string.unable_remove_remove_label));
            return true;
        } else {
            mPromptText.setText(mLauncher.getString(R.string.unable_install_app_label));
            Pair<ComponentName, Integer> componentInfo = getAppInfoFlags(info);
            return (componentInfo != null)
                    && (componentInfo.second & AppInfo.DOWNLOADED_FLAG) == 0
                    && !(source instanceof FolderLayout);
        }
    }


    @Override
    public void onDragEnter(DragObject d) {

    }

    @Override
    public void onDragExit(DragObject d) {
    }


    @Override
    public void onDrop(final DragObject d) {

    }


    @Override
    void completeDrop(final DragObject d) {

    }

}
