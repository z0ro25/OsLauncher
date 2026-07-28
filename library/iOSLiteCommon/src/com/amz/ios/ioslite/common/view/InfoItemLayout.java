package com.amz.ios.ioslite.common.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amz.ios.ioslite.common.R;

public class InfoItemLayout extends LinearLayout {
    private TextView mTitleText;
    private TextView mDesText;

    public InfoItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ItemInfoLayout);
        inflate(context, R.layout.pref_info_item_layout, this);

        mTitleText = (TextView) findViewById(R.id.info_title);
        mDesText = (TextView) findViewById(R.id.info_description);


        String title = array.getString(R.styleable.ItemInfoLayout_itemInfoTitle);
        if (!TextUtils.isEmpty(title)) {
            mTitleText.setText(title);
        }
    }

    public void setDescription(String value) {
        mDesText.setText(value);
    }

    public void setTitle(String value) {
        mTitleText.setText(value);
    }
}
