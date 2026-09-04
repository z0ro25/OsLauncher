package com.amz.ios.themeclub;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import com.google.android.material.tabs.TabLayout;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.themeclub.R;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.adapter.BottomTabPagerAdapter;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.util.PermissionUtils;

public class MainActivity extends CommonAppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private CustomTextView mThemeTitle;

    private Window mWindow;

    private BottomTabPagerAdapter mBottomTabPagerAdapter;
    private final String MINIMALIST_THEME = "minimalist_theme";

    private String toPointPage;
    private final String PAGR_TYPE_KEY = "themeclubtype";

    public static int PAGE_TYPE;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPermission();
//        AppUtils.changeStatusBarStyle(this);
        setContentView(R.layout.themeclub_activity_main);
        findView();
        mBottomTabPagerAdapter = new BottomTabPagerAdapter(getSupportFragmentManager(), this);
        mViewPager.setAdapter(mBottomTabPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        mViewPager.setOffscreenPageLimit(mTabLayout.getTabCount());
        mTabLayout.setSelectedTabIndicatorColor(Color.TRANSPARENT);
        mThemeTitle = (CustomTextView) findViewById(R.id.theme_title);


        Intent mIntent = getIntent();
        if (mIntent != null) {
            PAGE_TYPE = mIntent.getIntExtra(PAGR_TYPE_KEY, 0);
            toPointPage = mIntent.getStringExtra(MINIMALIST_THEME);
            Log.e(TAG, "toPointPage=" + toPointPage + ",type=" + PAGE_TYPE);
//            mViewPager.setCurrentItem(PAGE_TYPE);
        }
        setTabIcon();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        PAGE_TYPE = intent.getIntExtra(PAGR_TYPE_KEY, 0);
//        mViewPager.setCurrentItem(type);
    }

    private void findView() {
        mViewPager = (ViewPager) findViewById(R.id.container);
        mTabLayout = (TabLayout) findViewById(R.id.tab);
        mTabLayout.setVisibility(View.GONE);

    }

    private void setTabIcon() {
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            switch (i) {
                case AppConfig.TAB_WALLPAPER:
                    mTabLayout.getTabAt(AppConfig.TAB_WALLPAPER).setCustomView
                            (mBottomTabPagerAdapter.getView(this, AppConfig.TAB_WALLPAPER));
                    break;
                case AppConfig.TAB_THEME:
                    mTabLayout.getTabAt(AppConfig.TAB_THEME).setCustomView(mBottomTabPagerAdapter
                            .getView(this, AppConfig.TAB_THEME));
                    break;
                case AppConfig.TAB_LOCK:
                    mTabLayout.getTabAt(AppConfig.TAB_LOCK).setCustomView(mBottomTabPagerAdapter
                            .getView(this, AppConfig.TAB_LOCK));
                    break;
                case AppConfig.TAB_MINE:
                    mTabLayout.getTabAt(AppConfig.TAB_MINE).setCustomView(mBottomTabPagerAdapter
                            .getView(this, AppConfig.TAB_MINE));
                    break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        int currentItem = mViewPager.getCurrentItem();
        if (currentItem == 0) {
            AnalyticsDelegate.onWallpaperEvent(MainActivity.this, UMEventConstants.WALLPAPER_EXIT);
        } else if (currentItem == 1) {
            AnalyticsDelegate.onThemeEvent(MainActivity.this, UMEventConstants.THEME_EXIT);
        }
        Glide.get(this).clearMemory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    //请求权限
    public void getPermission() {
        PermissionUtils.getSdPermission(MainActivity.this);
        Log.e(TAG, "GET PERMISSON " + "isGetted");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case PermissionUtils.WRITE_EXTERNAL_STORAGE_REQUEST_CODE:
                boolean hasAllGranted = true;
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                        hasAllGranted = false;
                        new AlertDialog.Builder(this)
                                .setMessage( getString(R.string.themeclub_persion_content))
                                .setPositiveButton(getString(R.string.themeclub_persion_setting), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(),
                                                null);

                                        intent.setData(uri);
                                        startActivity(intent);
                                        finish();
                                    }
                                })
                                .setNegativeButton(getString(R.string.themeclub_persion_cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Toast.makeText(MainActivity.this, getString(R.string.themeclub_permissions_not_granted),
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                            }
                        }).show();
                    } else {
                        Toast.makeText(this, getString(R.string.themeclub_permissions_not_granted),
                                Toast.LENGTH_SHORT).show();
                        this.finish();
                    }
                }
                if (hasAllGranted) {
                    Toast.makeText(this, getString(R.string
                                    .themeclub_permision_available_read_external_storage_rationale),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case PermissionUtils.READ_PHONE_STATE_CODE:
                if (grantResults.length > 0 && !(grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, getString(R.string.themeclub_permissions_not_granted),
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
            default:
                break;

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mThemeTitle.updateFont();
    }
}
