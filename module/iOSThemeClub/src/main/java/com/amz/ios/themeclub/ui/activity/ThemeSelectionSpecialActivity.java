package com.amz.ios.themeclub.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemChildClickListener;
import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.adapter.ThemeNewestAdapter;
import com.amz.ios.themeclub.bean.ThemesBean;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.view.ItemDecoration;

import java.util.ArrayList;

/**
 * Created by ZhangMingZhe on 11/25/16.
 */

public class ThemeSelectionSpecialActivity extends CommonAppCompatActivity {
    private Toolbar mToolbar;
    private CustomTextView mTitle;
    private String title = "";
    private RecyclerView mRecycleView;
    private ThemeNewestAdapter mAdapter;
    private ArrayList<ThemesBean> list;
    private Window mWindow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.changeStatusBarStyle(this);
        setContentView(R.layout.theme_specail_layout);
        getData(getIntent());
        initView();
    }

    private void initView() {
        mToolbar = (Toolbar) findViewById(R.id.theme_special_toolbar);
        mToolbar.setTitle("");
        mTitle = (CustomTextView) findViewById(R.id.theme_special_title);
        mTitle.setText(title);
        mToolbar.setNavigationIcon(R.drawable.source_detail_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mAdapter = new ThemeNewestAdapter(this,list);
        mRecycleView = (RecyclerView) findViewById(R.id.theme_special_recycle);
        mRecycleView.setAdapter(mAdapter);
        mRecycleView.addItemDecoration(new ItemDecoration(0, 0, 0, 20));
        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        mRecycleView.addOnItemTouchListener(new OnItemChildClickListener() {
//            @Override
//            public void SimpleOnItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
//                Intent yIntent = new Intent(ThemeSelectionSpecialActivity.this, OnlineThemeDetailActivity.class);
//                yIntent.putExtra("themebean",list.get(i));
//                startActivity(yIntent);
//            }

            @Override
            public void onSimpleItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
                Intent yIntent = new Intent(ThemeSelectionSpecialActivity.this, OnlineThemeDetailActivity.class);
                yIntent.putExtra("themebean",list.get(i));
                startActivity(yIntent);

            }
        });

        final View view = findViewById(R.id.temp_view);
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppUtils.getStatusBarHeight(this)));
   }

    private void getData(Intent intent) {
        title = intent.getStringExtra("title");
        list = (ArrayList<ThemesBean>) intent.getSerializableExtra("data");
        Log.e("test_zmz","data="+list.toString());
    }


}
