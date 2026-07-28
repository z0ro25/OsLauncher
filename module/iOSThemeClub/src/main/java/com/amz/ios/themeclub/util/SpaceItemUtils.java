package com.amz.ios.themeclub.util;

import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

/**
 * Created by lideqian on 16-12-12.
 */
public class SpaceItemUtils extends RecyclerView.ItemDecoration{

    private int space;

    public SpaceItemUtils(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.right = space;
    }
}