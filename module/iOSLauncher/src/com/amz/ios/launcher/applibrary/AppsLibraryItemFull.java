package com.amz.ios.launcher.applibrary;

import static com.amz.ios.launcher.Launcher.viewItem4GroupAppLibrary;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAnimUtils;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;

import java.util.ArrayList;

public class AppsLibraryItemFull extends ConstraintLayout implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    AppsLibraryBubbleTextView mItem1;
    AppsLibraryBubbleTextView mItem2;
    AppsLibraryBubbleTextView mItem3;
    AppsLibraryBubbleTextView mItem4;
    TextViewCustomFont mLabelTV;
    ConstraintLayout mView;
    Launcher mLauncher = (Launcher) getContext();
    String mLabel = "";
    boolean canOpen = false;

    public AppsLibraryItemFull(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsLibraryItemFull(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUp();
        config();
        setListeners();
    }

    void setUp() {
        LayoutInflater.from(getContext()).inflate(R.layout.apps_library_item_full, (ViewGroup) this, true);
        mView = findViewById(R.id.app_background);
        mLabelTV = findViewById(R.id.app_category_title);

        mItem1 = findViewById(R.id.app_1);
        mItem2 = findViewById(R.id.app_2);
        mItem3 = findViewById(R.id.app_3);
        mItem4 = findViewById(R.id.app_4);

        mItem1.setTextVisibility(false);
        mItem2.setTextVisibility(false);
        mItem3.setTextVisibility(false);
        mItem4.setTextVisibility(false);
    }

    void config() {
        DeviceProfile deviceProfile = mLauncher.getDeviceProfile();
        int margin = deviceProfile.edgeMarginPx / 3;

        int height = mItem1.getLineHeight();
        height = margin;

        height = (getWidth() - deviceProfile.cellWidthPx * 2) / 3;

        /*((MarginLayoutParams)mItem1.getLayoutParams()).topMargin = height;
        ((MarginLayoutParams)mItem1.getLayoutParams()).setMarginStart(height);

        ((MarginLayoutParams)mItem2.getLayoutParams()).topMargin = height;
        ((MarginLayoutParams)mItem2.getLayoutParams()).setMarginEnd(height);

        ((MarginLayoutParams)mItem3.getLayoutParams()).setMarginStart(height);
        ((MarginLayoutParams)mItem3.getLayoutParams()).bottomMargin = margin;

        ((MarginLayoutParams)mItem4.getLayoutParams()).setMarginEnd(height);
        ((MarginLayoutParams)mItem4.getLayoutParams()).bottomMargin = margin;*/
    }

    void setListeners() {
        mItem1.setOnClickListener(this);
        mItem2.setOnClickListener(this);
        mItem3.setOnClickListener(this);
        mItem4.setOnClickListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    @Override
    public void onClick(View v) {
        if (v == mItem4 && canOpen) {
            OpenedLibraryView view = mItem1.mOpenView;
            if (view.getParent() == null) {
                mLauncher.hideAppsLibrary();
                mLauncher.getLauncherView().addView(view);
                viewItem4GroupAppLibrary = view;
                view.setScaleX(0.3f);
                view.setScaleY(0.3f);
                view.setAlpha(0.0f);
                ObjectAnimator animator = LauncherAnimUtils.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat("alpha", 1.0f), PropertyValuesHolder.ofFloat("scaleX", 1.0f), PropertyValuesHolder.ofFloat("scaleY", 1.0f));
                animator.setDuration(399L);
                animator.setInterpolator(Launcher.initInterpolator(0.02f, 0.11f, 0.13f, 1.0f));
                animator.start();
            }
        }

    }

    public void setTitle(String title) {
        this.mLabel = title;
        this.mLabelTV.setText(title);
        mItem1.mOpenView.mAdapter.mLabel = title;
    }

    public void setApps(ArrayList<AppInfo> infos) {

        mItem1.mOpenView.mAdapter.mApps = infos;
        mItem1.mOpenView.mAdapter.notifyDataSetChanged();
        mItem1.mOpenView.setOnClickListener(this);

        mItem1.setText(null);
        mItem1.setTag(null);
        mItem1.clearBackground();
        mItem2.setTag(null);
        mItem2.clearBackground();
        mItem3.setTag(null);
        mItem3.clearBackground();
        mItem4.setTag(null);
        mItem4.clearBackground();

        if (infos == null) return;

        int size = infos.size();
        if (size == 0) return;

        mItem1.setTag(infos.get(0));
        mItem1.reapplyItemInfo(infos.get(0));

        if (size > 1) {
            mItem2.setTag(infos.get(1));
            mItem2.reapplyItemInfo(infos.get(1));
        }
        if (size > 2) {
            mItem3.setTag(infos.get(2));
            mItem3.reapplyItemInfo(infos.get(2));
        }

        mItem1.setText("");
        mItem2.setText("");
        mItem3.setText("");

        if (size < 4) return;


        if (size < 5) {
            mItem4.setTag(infos.get(3));
            mItem4.reapplyItemInfo(infos.get(3));
            mItem4.setText("");
            return;
        }

        Paint paint = new Paint(1);
        paint.setAntiAlias(true);

        int bmpSize = mLauncher.getDeviceProfile().iconSizePx;

        int smallIconSize = (int) (bmpSize / 2.6d);
        int padding = (int) (bmpSize * 0.6d / 7.800000000000001d);

        Bitmap createBitmap = Bitmap.createBitmap(bmpSize, bmpSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);

        int restItemSize = Math.min(infos.size() - 3, 4);

        int i = 0;
        while (i < restItemSize) {
            int row = i % 2;
            int cell = i / 2;
            int left = ((row + 1) * padding) + (row * smallIconSize);
            int top = ((cell + 1) * padding) + (cell * smallIconSize);
            canvas.drawBitmap(
                    infos.get(i + 3).getIcon(),
                    (Rect) null,
                    new Rect(left, top, left + smallIconSize, top + smallIconSize), paint);
            i++;
        }

        mItem4.setIcon(
                mLauncher.createIconDrawableWithNoBg(createBitmap),
                createBitmap.getWidth()
        );

        OpenedLibraryView openedLibraryView = mItem4.mOpenView;
        OpenLibraryItemAdapter openLibraryItemAdapter = openedLibraryView.mAdapter;
        openLibraryItemAdapter.mApps.clear();
        openLibraryItemAdapter.mApps.addAll(infos);
        openLibraryItemAdapter.mLabel = this.mLabel;
        openLibraryItemAdapter.notifyDataSetChanged();
        this.canOpen = true;
    }
}
