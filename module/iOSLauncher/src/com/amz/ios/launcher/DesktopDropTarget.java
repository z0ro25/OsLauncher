package com.amz.ios.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.amz.ios.launcher.folder.FolderLayout;


public class DesktopDropTarget extends ButtonDropTarget {
    public DesktopDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DesktopDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTargetView = findViewById(R.id.desktop_target_view);
    }

    @Override
    protected View getTargetView() {
        return mTargetView;
    }

    @Override
    public void onDragEnter(DragObject d) {
        super.onDragEnter(d);
        if (mLauncher.isFolderOpen()) {
            mLauncher.closeFolder();
        }
    }

    @Override
    public void onDragExit(DragObject d) {
        super.onDragExit(d);
    }

    @Override
    protected boolean supportsDrop(DragSource source, Object info) {
        return source instanceof FolderLayout;
    }


    @Override
    void completeDrop(final DragObject d) {
        ItemInfo item = (ItemInfo) d.dragInfo;
        if (d.dragSource instanceof FolderLayout) {
            FolderLayout folderLayout = (FolderLayout)d.dragSource;
            if (folderLayout.getItemCount() == 1) {
                FolderInfo folderInfo = folderLayout.getFolderInfo();
                item.screenId = folderInfo.screenId;
                item.container = folderInfo.container;
                item.cellX = folderInfo.cellX;
                item.cellY = folderInfo.cellY;
            }
            LauncherAppState app = LauncherAppState.getInstance();
            app.getModel().addWorkspaceItemFromFolder(mLauncher, item,mLauncher.getWorkspace().getCurrentScreenId());
        }
    }

}

