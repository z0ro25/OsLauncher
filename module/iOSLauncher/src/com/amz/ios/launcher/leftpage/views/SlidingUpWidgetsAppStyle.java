package com.amz.ios.launcher.leftpage.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.viewpager2.widget.ViewPager2;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.leftpage.adapter.SlidingUpWidgetsAppStyleAdapter;
import com.amz.ios.launcher.leftpage.model.CustomWidgetDetailInfo;
import com.amz.ios.launcher.slideup.SlidingUpPanelLayout;

import java.util.ArrayList;

public class SlidingUpWidgetsAppStyle extends SlidingUpPanelLayout {

    Launcher mLauncher;
    SlidingUpWidgetsAppStyleAdapter mAppStyleAdapter;
    AppCompatButton mAddBtn;
    ViewPager2 mWidgetPager;

    public SlidingUpWidgetsAppStyle(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = (Launcher) context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setUpView();
        setAdapter();
        setListeners();
    }

    void setUpView(){
        mAddBtn = findViewById(R.id.add_button_widgets_app_style);
        mWidgetPager = findViewById(R.id.view_pager_widgets_app_style);
        mWidgetPager.setOffscreenPageLimit(3);
    }

    void setListeners(){
        mAddBtn.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        int currentItem = mWidgetPager.getCurrentItem();
                        CustomWidgetDetailInfo detail = mAppStyleAdapter.mWidgetDetails.get(currentItem);
                        CustomContentView customContentView = mLauncher.mCustomContentView;
                        if (customContentView == null) return;
                        customContentView.startShaking();
                        customContentView.addWidget(detail.mId);
                        customContentView.collapseAppsStyle();
                        customContentView.collapseWidgetList();
                        customContentView.enableButtons();
                    }
                }
        );
    }

    void setAdapter(){
        mWidgetPager.setAdapter(mAppStyleAdapter);
    }

    public void setWidgetsAppStyleList(ArrayList<CustomWidgetDetailInfo> details){
        mAppStyleAdapter = new SlidingUpWidgetsAppStyleAdapter(mLauncher, details);
        setAdapter();
    }

}
