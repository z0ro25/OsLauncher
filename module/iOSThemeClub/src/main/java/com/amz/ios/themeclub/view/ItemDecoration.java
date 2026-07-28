package com.amz.ios.themeclub.view;

import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

/**
 * Created by ZhangMingZhe on 11/15/16.
 */

public class ItemDecoration extends RecyclerView.ItemDecoration{
    //单位为px
    private int mBottom;
    private int mLeft;
    private int mRight;
    private int mTop;
    private int mType;
    public static final int ITEMDECORATION_TYPE_Z = 0;

    public ItemDecoration(int mLeft,int mTop,int mRight,int mBottom) {
        this.mLeft = mLeft;
        this.mTop = mTop;
        this.mRight = mRight;
        this.mBottom = mBottom;
        mType = -1;
    }

    public ItemDecoration(int mLeft,int mTop,int mRight,int mBottom,int mType) {
        this.mLeft = mLeft;
        this.mTop = mTop;
        this.mRight = mRight;
        this.mBottom = mBottom;
        this.mType = mType;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

        if(mType == ITEMDECORATION_TYPE_Z){

            int position = parent.getChildAdapterPosition(view);

            if(position%3==1){
                outRect.left = mLeft;
                outRect.right = mRight;
            }
            if(position<6){
                outRect.bottom = mBottom;
            }

        }else{
            outRect.left = mLeft;
            outRect.top = mTop;
            outRect.right = mRight;
            outRect.bottom = mBottom;
        }

    }
}
