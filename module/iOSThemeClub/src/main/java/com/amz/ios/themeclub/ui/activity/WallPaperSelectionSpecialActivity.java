package com.amz.ios.themeclub.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.SimpleClickListener;
import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.adapter.SpecialWallPaperSelectionAdapter;
import com.amz.ios.themeclub.bean.WallPaperNewestPieceBean;
import com.amz.ios.themeclub.bean.WallPaperSelectionPieceBean;
import com.amz.ios.themeclub.bean.WallPapersBean;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.WallpaperUtil;
import com.amz.ios.themeclub.view.ItemDecoration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZhangMingZhe on 11/22/16.
 */

public class WallPaperSelectionSpecialActivity extends CommonAppCompatActivity {

    private final String TAG = WallPaperSelectionSpecialActivity.class.getSimpleName();
    RecyclerView mRecycleView;
    Toolbar mToolBar;
    CustomTextView mToolTitle;
    private SpecialWallPaperSelectionAdapter mAdapter;
    private WallPaperSelectionPieceBean.IssuesBean bean;
    ArrayList<WallPaperNewestPieceBean> mPieceList;
    private final int TYPE_Y_SIZE = 6;
    private Window mWindow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.changeStatusBarStyle(this);
        setContentView(R.layout.wallpaper_selection_activity);
        findViewByid();
        setData();
        initView();
    }

    private void findViewByid() {
        mRecycleView = (RecyclerView) findViewById(R.id.special_selection);
        mToolBar = (Toolbar) findViewById(R.id.tool_bar);
        mToolTitle = (CustomTextView) findViewById(R.id.tool_title);
    }

    private void setData() {
        bean = (WallPaperSelectionPieceBean.IssuesBean) getIntent().getExtras().getSerializable("data");
        Log.e(TAG,"size="+bean.getWallPapers().size());
        Log.e(TAG,"size="+bean.getWallPapers().toString());
        if(bean == null){
            Log.e(TAG,"bean is null");
            return;
        }
        mPieceList = new ArrayList<>();
        int total = bean.getWallPapers().size();
        if(total == 0){
            return;
        }
        int count = total/6;
        int remainder =total%6;
        int tempIndex = 0;
        if(count!=0){
        //添加块
            for (int i = 0; i < count;i++,tempIndex+=TYPE_Y_SIZE) {
                WallPaperNewestPieceBean temp = new WallPaperNewestPieceBean();
                temp.setmData(bean.getWallPapers().subList(tempIndex,tempIndex+TYPE_Y_SIZE));
                temp.setItemType(i%2==0?WallPaperNewestPieceBean.WALLPAPER_TYPE_Q:WallPaperNewestPieceBean.WALLPAPER_TYPE_Y);
                mPieceList.add(temp);
            }
        }
        if(remainder!=0){
            //如果数据有多的，添加空数据的块
          int addCount = TYPE_Y_SIZE - remainder;
            Log.e("test_zmz","addCount="+addCount+",allsize="+bean.getWallPapers().size());

          WallPaperNewestPieceBean temp1 = new WallPaperNewestPieceBean();
          if(mPieceList.size() == 0){
              temp1.setItemType(WallPaperNewestPieceBean.WALLPAPER_TYPE_Y);
          }else if(mPieceList.get(mPieceList.size()-1).getItemType()==WallPaperNewestPieceBean.WALLPAPER_TYPE_Q){
              temp1.setItemType(WallPaperNewestPieceBean.WALLPAPER_TYPE_Y);
          }else{
              temp1.setItemType(WallPaperNewestPieceBean.WALLPAPER_TYPE_Q);
          }
            List<WallPapersBean> list = new ArrayList<>();
           list.addAll(bean.getWallPapers().subList(bean.getWallPapers().size()-remainder,bean.getWallPapers().size()));

            Log.e(TAG,"remainder="+remainder+",list="+list.toString());

            for (int i = 0; i < addCount; i++) {
                WallPapersBean bean = new WallPapersBean();
                WallPapersBean.SmallImageBean small = new WallPapersBean.SmallImageBean();
                small.setDownloadUrl(null);
                bean.setSmallImage(small);
                list.add(bean);
            }
        temp1.setmData(list);
        mPieceList.add(temp1);
    }
        //第一个item类型是和原本的item类型相同的
//        WallPaperNewestPieceBean theFirst = new WallPaperNewestPieceBean();
//        theFirst.setmData(bean.getWallPapers().subList(0,bean.getDisplayNum()));
//        int type = -1;
//        if(bean.getDisplayNum()==3){
//            type = WallPaperNewestPieceBean.WALLPAPER_TYPE_X;
//        }else if(bean.getDisplayNum()==6){
//            type = WallPaperNewestPieceBean.WALLPAPER_TYPE_Y;
//        }else {
//            //=9
//            type = WallPaperNewestPieceBean.WALLPAPER_TYPE_Z;
//        }
//        theFirst.setItemType(type);
//        mPieceList.add(theFirst);
//        int totalSize = bean.getWallPapers().size();
//        //遍历剩下的view,从第一个item的数量开始
//        int lastCount = bean.getWallPapers().size() - bean.getDisplayNum();
//        //便利的次数
//        int times = lastCount/6;
//        int remainder = lastCount%6;
//        //添加块
//        if(times!=0){
//            for (int i = 0,j=bean.getDisplayNum(); i < times; i++,j+=TYPE_Y_SIZE) {
//                WallPaperNewestPieceBean temp = new WallPaperNewestPieceBean();
//                temp.setmData(bean.getWallPapers().subList(j,j+TYPE_Y_SIZE));
//                temp.setItemType(i%2==0?WallPaperNewestPieceBean.WALLPAPER_TYPE_Q:WallPaperNewestPieceBean.WALLPAPER_TYPE_Y);
//                mPieceList.add(temp);
//            }
//        }
//        //除不进的数据继续便利
//        if(remainder!=0){
//            int size = remainder/3;
//            if(size!=0){
//                for (int i = 0,j=(totalSize-remainder); i < size; i++,j+=TYPE_X_SIZE) {
//                    WallPaperNewestPieceBean temp = new WallPaperNewestPieceBean();
//                    temp.setmData(bean.getWallPapers().subList(j,j+TYPE_X_SIZE));
//                    temp.setItemType(WallPaperNewestPieceBean.WALLPAPER_TYPE_X);
//                    mPieceList.add(temp);
//                }
//            }
//        }
        Log.e(TAG,"size="+mPieceList.size()+","+mPieceList.toString());

        mAdapter = new SpecialWallPaperSelectionAdapter(this,mPieceList);
    }

    private void initView() {
        if(bean == null){
            Log.e(TAG,"bean is null");
            return;
        }
        Log.e(TAG,"data="+bean.toString());
        mToolBar.setNavigationIcon(R.drawable.source_detail_back);
        mToolBar.setTitle("");
        mToolTitle.setText(bean.getTitle());
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //设置系统状态栏
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        //此处应该有适配, 6.0以下
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
        }
        mRecycleView.setAdapter(mAdapter);
        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        mRecycleView.addItemDecoration(new ItemDecoration(0, 0, 0, 4));

        mRecycleView.addOnItemTouchListener(new SimpleClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {

            }

            @Override
            public void onItemLongClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {

            }

            @Override
            public void onItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
                int position = getCurrenrPosition(view.getId());
                WallPapersBean bean = mAdapter.getData().get(i).getmData().get(position);
                if(TextUtils.isEmpty(bean.getSmallImage().getDownloadUrl())){
                    return;
                }
                if(position != -1){
                    List<WallPapersBean> wallpapers = new ArrayList<WallPapersBean>();
                    wallpapers.addAll(mAdapter.getData().get(i).getmData());
                    Intent mIntent = new Intent(WallPaperSelectionSpecialActivity.this,OnlineWallpapersDetailActivity.class);
                    mIntent.putExtra(WallpaperUtil.ONLINEWALLPAPERLIST,(Serializable)wallpapers);
                    mIntent.putExtra(WallpaperUtil.ONLINEWALLPAPER_POSITION,position);
                    startActivity(mIntent);
                }
            }

            @Override
            public void onItemChildLongClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {

            }
        });

        final View view = findViewById(R.id.temp_view);
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppUtils.getStatusBarHeight(this)));
    }

    private int getCurrenrPosition(int id) {
        int position = -1;
        if(id == R.id.img_one||id ==R.id.img_1||id==R.id.y_img_1){
            position = 0;
        }else if(id == R.id.img_two||id == R.id.img_2||id == R.id.y_img_2){
            position = 1;
        }else if(id == R.id.img_three || id == R.id.img_3 || id==R.id.y_img_3){
            position = 2;
        }else if(id == R.id.img_4 || id == R.id.y_img_4){
            position = 3;
        }else if(id == R.id.img_5 || id == R.id.y_img_5){
            position = 4;
        }else if(id == R.id.img_6 || id == R.id.y_img_6){
            position = 5;
        }
        return position;
    }
}
