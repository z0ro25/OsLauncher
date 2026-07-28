package com.amz.ios.themeclub.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.bean.LockScreenBean;
import com.amz.ios.themeclub.ui.activity.LockScreenDetailActivity;
import com.amz.ios.themeclub.util.AppUtils;

import java.util.ArrayList;
import java.util.List;

public class LockScreenItemView extends LinearLayout {
    private final String TAG = getClass().getSimpleName();
    private List<LockScreenBean> mData = new ArrayList<>();
    private int mWidth;
    private int mLeftMargin;
    private int mTopMargin;
    private int mChildRightMargin;
    private int mHeight;
    private int mDataSize;
    private RequestManager mGlide;

    public LockScreenItemView(Context context) {
        this(context, null);
    }

    public LockScreenItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LockScreenItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mGlide = Glide.with(getContext());
        int screenWidth = AppUtils.getScreenWidth(getContext());
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.themeclubLockscreenItemView);
        mHeight = (int) ta.getDimension(R.styleable.themeclubLockscreenItemView_themeclubChildHeight, 180);
        mDataSize = ta.getInteger(R.styleable.themeclubLockscreenItemView_themeclubDataSize, 3);
        mChildRightMargin = (int) ta.getDimension(R.styleable.themeclubLockscreenItemView_themeclubChildRightMargin, 0);
        mLeftMargin = (int) ta.getDimension(R.styleable.themeclubLockscreenItemView_themeclubLeftMargin, 0);
        mTopMargin = (int) ta.getDimension(R.styleable.themeclubLockscreenItemView_themeclubTopMargin, 0);
        final float parentViewWidth = ta.getDimension(R.styleable.themeclubLockscreenItemView_themeclubParentViewMargin, 0);
        mWidth = (int) (screenWidth - parentViewWidth);
        ta.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        DebugLog.i(TAG, "==================onLayout:" + mData.size());
        final int size = mDataSize;
        final int childWidth = (mWidth - mLeftMargin - mChildRightMargin * size) / size;
        final int childHeight = mHeight - mTopMargin;
        DebugLog.w(TAG, "=====================child:" + childHeight + "/" + childWidth);
        final LayoutParams layoutParams = new LayoutParams(childWidth, LayoutParams.MATCH_PARENT);
        for (int i = 0; i < mData.size(); i++) {
            if (mData.get(i).getPreview() == null) {
                return;
            }
            final ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(layoutParams);
            final int position = i;
            imageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                        startDetail(mData, position);
                    }
            });
            imageView.layout(mLeftMargin + i * (childWidth + mChildRightMargin), mTopMargin, mLeftMargin + (i + 1) * childWidth + mChildRightMargin * i, mTopMargin + childHeight);
            mGlide.load(mData.get(i).getPreview().getDownloadUrl()).placeholder(R.drawable.theme_vertical).centerCrop().into(imageView);
            addView(imageView);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Glide.get(getContext()).clearMemory();
    }

    public void setData(List<LockScreenBean> data) {
        mData.clear();
        mData.addAll(data);
    }

    public void startDetail(List<LockScreenBean> lockScreenBeans, int position) {
        try {
            Intent intent = new Intent(getContext(), LockScreenDetailActivity.class);
            intent.putExtra("lockbean", lockScreenBeans.get(position));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        } catch (Exception e) {
            DebugLog.e(TAG, "==============Exception:" + e.toString());
        }
    }
}
