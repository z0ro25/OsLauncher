package com.amz.ios.themeclub.ui.activity;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.bean.ThemeInfo;
import com.amz.ios.themeclub.util.ThemeUtils;
import com.amz.ios.themeclub.view.ThemePagedView;

import java.util.ArrayList;
import java.util.List;

import static com.amz.ios.themeclub.effect.ThemeScrollEffect.SCROLL_EFFECT_STACK;


/**
 * Created by TUDOL on 10/25/2019.
 */

public class FullThemeActivity extends AppCompatActivity implements ThemePagedView.PageSwitchListener {
    private List<Bitmap> mListviews = new ArrayList<>();
    private int mCurIdx;
    private ThemePagedView mPagedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_theme);

        ThemeInfo themeInfo = (ThemeInfo) getIntent().getSerializableExtra("theme_info");
        ArrayList<String> mThemePreviews = ThemeUtils.getPicList(this, themeInfo.packageName);
        BitmapDrawable mDrawable;
        for (int i = 0; i < mThemePreviews.size(); i++) {
            mDrawable = ThemeUtils.getThemePreview(this, themeInfo.packageName, themeInfo.themePath, mThemePreviews.get(i));
            if (mDrawable != null) {
                mListviews.add(mDrawable.getBitmap());
            }
        }

        mCurIdx = getIntent().getIntExtra("current_index", 0);

        mPagedView = (ThemePagedView) findViewById(R.id.theme_preview);
        mPagedView.setViews(mListviews, 1);
        mPagedView.setScrollEffectFromString(SCROLL_EFFECT_STACK);
        mPagedView.showScrollEffectAnimation();
    }

    @Override
    public void onPageSwitch(View newPage, int newPageIndex) {

    }

    @Override
    public void onPageBeginMoving() {

    }

    @Override
    public void onPageEndMoving() {

    }
}
