package com.amz.ios.themeclub.ui.activity;


import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.intertfaces.IProgressView;
import com.amz.ios.themeclub.presenter.WallpaperPresenter;
import com.amz.ios.themeclub.tools.WallpaperSetTask;
import com.amz.ios.themeclub.util.WallpaperUtil;
import com.amz.ios.themeclub.view.ZoomInOutImage;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.ArrayList;

/**
 * Created by server on 16-11-19.
 */

public class ShowDownloadedWallpaperActivity extends CommonAppCompatActivity implements View.OnClickListener{

    private final String TAG = "setWallpaper";
    public static final int REQUEST_CODE_CROP_SET_WALLPAPER_DONE = 1100;

    private String wallpaperPath;
    private ArrayList<String> mBothWallpaperPaths;
    private ViewPager mImage;
    private Button mSetWallpaperButton;
    private int mCurrentPosition;
    RelativeLayout mProgress;
    LocalWallpaperAdapter mLocalWallpaperAdapter;
    private  WallpaperSetTask mWallpaperSetTask;
    private WallpaperPresenter mWallpaperPresenter;
    private IProgressView mIProgressView = new IProgressView() {
        @Override
        public void showProgress() {
            if(mProgress!=null){
                mProgress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void closeProgress() {
            if(mProgress!=null) {
                mProgress.setVisibility(View.GONE);
            }
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.themeclub_activity_setwallpaper);
//        AppUtils.immersive(this);
        mProgress = (RelativeLayout)findViewById(R.id.progress_set);
        mImage = (ViewPager)findViewById(R.id.image_viewpager);
        mSetWallpaperButton = (Button)findViewById(R.id.set_as_wallpaper);
        mSetWallpaperButton.setOnClickListener(this);
        initConfig();
        Log.d(TAG, "onCreate: gaol wallpaperPath="+wallpaperPath);
        initToolbar();
    }

    private void initConfig() {
        mBothWallpaperPaths = new ArrayList<String>();
        if(getIntent() != null) {
            Intent intent = getIntent();
            mBothWallpaperPaths = intent.getStringArrayListExtra(WallpaperUtil.LOCALWALLPAPER_PATHS);
            mCurrentPosition = intent.getIntExtra(WallpaperUtil.LOCALWALLPAPER_POSITION,0);
        }
        mWallpaperPresenter = new WallpaperPresenter(this,null, AppConfig.isWallPaperScroolEnable(this));
        wallpaperPath = mBothWallpaperPaths.get(mCurrentPosition);
        mLocalWallpaperAdapter = new LocalWallpaperAdapter();
        mImage.setAdapter(mLocalWallpaperAdapter);
        mImage.setOffscreenPageLimit(2);
        mImage.setCurrentItem(mCurrentPosition);
        mImage.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCurrentPosition = position;
                wallpaperPath = mBothWallpaperPaths.get(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void initToolbar(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.set_wallpaper_toolbar);
        if( getResources().getConfiguration().locale.getLanguage().equals("ar") ){
            toolbar.setNavigationIcon(R.drawable.themeclub_advance);
        }else {
            toolbar.setNavigationIcon(R.drawable.themeclub_back);
        }
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_setwallpaper, menu);
        return true;
    }

    private boolean isSystemWallpaper(String wallpaperPath){
        return wallpaperPath.startsWith("/system")||wallpaperPath.startsWith("system/");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: gaol");
        wallpaperPath = mBothWallpaperPaths.get(mCurrentPosition);
        int id = item.getItemId();
//        if(id == R.id.delete){
//            if(isSystemWallpaper(wallpaperPath)) {
//                Log.e(TAG,mBothWallpaperPaths.get(currentPosition));
//                ToastUtil.show(this,R.string.themeclub_private_picture);
//            } else {
//                FileUtils.deleteFile(new File(mBothWallpaperPaths.get(currentPosition)));
//                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + mBothWallpaperPaths.get(currentPosition))));
//                Intent intet = new Intent(WallpaperUtil.WALLPAPER_NEED_UPDATE);
//                sendBroadcast(intet);
//                Toast.makeText(this, R.string.themeclub_delete_sucess, Toast.LENGTH_LONG).show();
//                finish();
//            }
//
//        }else if(id == R.id.share){
//            if (isSystemWallpaper(wallpaperPath)){
//                ToastUtil.show(this,R.string.themeclub_private_picture_share);
//            }else {
//                ShareUtils.SharePic(this,getString(R.string.themeclub_share_wallpaper),mBothWallpaperPaths.get(currentPosition));
//            }
//        }else
            if(id == R.id.crop){
            Intent intent = new Intent(this, CropImageActivity.class);
            intent.putExtra("path", wallpaperPath);
            intent.putExtra("selectedId", mCurrentPosition);
            intent.putExtra("isUriOrPath",false);
            startActivityForResult(intent, REQUEST_CODE_CROP_SET_WALLPAPER_DONE);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.set_as_wallpaper){
            mWallpaperPresenter.applyWallpaper(this,
                    mIProgressView,
                    null,
                    mBothWallpaperPaths.get(mCurrentPosition),
                    LayoutInflater.from(this).inflate(R.layout.themeclub_activity_setwallpaper,null),
                    mCurrentPosition
            );
//            mWallpaperSetTask = new WallpaperSetTask(this,mIProgressView, mBothWallpaperPaths.get(currentPosition), setAsWallpaper,true);
//            mWallpaperSetTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mWallpaperSetTask != null && mWallpaperSetTask.getStatus() == AsyncTask.Status.RUNNING) {
            mWallpaperSetTask.cancel(true);
            mWallpaperSetTask = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }else if(requestCode == REQUEST_CODE_CROP_SET_WALLPAPER_DONE){
            if (data != null) {
                boolean setWallpaperDone = data.getBooleanExtra("set_wallpaper", false);
                if(setWallpaperDone){
                    finish();
                }
            }
        }
    }


    public class LocalWallpaperAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mBothWallpaperPaths.size();
        }

        @Override
        public boolean isViewFromObject(View v, Object object) {
            return v == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            View view = getLayoutInflater().inflate(R.layout.themeclub_localwallpaper_viewpager_item, container, false);
            ZoomInOutImage iv = (ZoomInOutImage)view.findViewById(R.id.viewpager_localwallpaper_item);
            iv.setImageBitmap(BitmapFactory.decodeFile(mBothWallpaperPaths.get(position)));
            container.addView(view,0);
            return view;
        }
    }
}