package com.amz.ios.launcher.leftpage.views;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.WidgetPreviewLoader;
import com.amz.ios.launcher.leftpage.adapter.SlidingUpWidgetListAdapter;
import com.amz.ios.launcher.leftpage.database.WidgetInfo;
import com.amz.ios.launcher.leftpage.model.CustomWidgetDetailInfo;
import com.amz.ios.launcher.leftpage.model.CustomWidgetSummaryInfo;
import com.amz.ios.launcher.slideup.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class SlidingUpWidgetsList extends SlidingUpPanelLayout {

    Launcher mLauncher;
    RecyclerView mWidgetListRV;
    SlidingUpWidgetListAdapter mSlidingUpWidgetListAdapter;
    int mMargin;
    public ArrayList<CustomWidgetSummaryInfo> mWidgetSummaries = new ArrayList<>();

    public SlidingUpWidgetsList(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = (Launcher) context;
        mMargin = mLauncher.getDeviceProfile().edgeMarginPx;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        config();
        setUpView();
        setAdapter();
        setListeners();
    }

    void setUpView(){
        mWidgetListRV = findViewById(R.id.left_page_widgets_lists);

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mWidgetListRV.getLayoutParams();
        layoutParams.leftMargin = mMargin;
        layoutParams.rightMargin = mMargin;

    }

    void setAdapter(){
        StaggeredGridLayoutManager gridLayoutManager = new StaggeredGridLayoutManager(2, RecyclerView.VERTICAL);
        mWidgetListRV.setLayoutManager(gridLayoutManager);

        mSlidingUpWidgetListAdapter = new SlidingUpWidgetListAdapter(mLauncher, mWidgetSummaries);
        mWidgetListRV.setAdapter(mSlidingUpWidgetListAdapter);

        mWidgetListRV.setItemAnimator(new DefaultItemAnimator());
        mWidgetListRV.addItemDecoration(
                new RecyclerView.ItemDecoration() {
                    @Override
                    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                        super.getItemOffsets(outRect, view, parent, state);
                        int margin = mMargin / 2;
                        outRect.top = margin;
                        outRect.left = margin;
                        outRect.bottom = margin;
                        outRect.right = margin;
                    }
                }
        );

    }

    void setListeners(){

    }

    void config(){
        ArrayList<CustomWidgetDetailInfo> battery = new ArrayList<>();
        battery.add(
                new CustomWidgetDetailInfo(
                    getDrawable(R.drawable.sample_battery_widget),
                    SlidingUpWidgetListAdapter.SAMPLE_BATTERY_WIDGET
                )
        );

        ArrayList<CustomWidgetDetailInfo> photo = new ArrayList<>();
        photo.add(
                new CustomWidgetDetailInfo(
                    getDrawable(R.drawable.sample_photo_widget),
                    SlidingUpWidgetListAdapter.SAMPLE_PHOTO_WIDGET
                )
        );

        ArrayList<CustomWidgetDetailInfo> appSuggestion = new ArrayList<>();
        appSuggestion.add(
                new CustomWidgetDetailInfo(
                        getDrawable(R.drawable.sample_app_suggestions),
                        SlidingUpWidgetListAdapter.SAMPLE_APP_SUGGESTIONS
                )
        );

        ArrayList<CustomWidgetDetailInfo> contact = new ArrayList<>();
        contact.add(
                new CustomWidgetDetailInfo(
                        getDrawable(R.drawable.sample_contact_favorite),
                        SlidingUpWidgetListAdapter.SAMPLE_CONTACT_FAVOURITE
                )
        );

        ArrayList<CustomWidgetDetailInfo> calendar = new ArrayList<>();
        calendar.add(
                new CustomWidgetDetailInfo(
                        getDrawable(R.drawable.sample_calendar_widget),
                        SlidingUpWidgetListAdapter.SAMPLE_CALENDAR_WIDGET
                )
        );


        mWidgetSummaries.add(
                new CustomWidgetSummaryInfo(
                        getDrawable(R.drawable.sample_battery_widget),
                        getResources().getString(R.string.battery),
                        false,
                        battery
                )
        );

        mWidgetSummaries.add(
                new CustomWidgetSummaryInfo(
                        getDrawable(R.drawable.sample_photo_widget),
                        getResources().getString(R.string.picture),
                        false,
                        photo
                )
        );

        mWidgetSummaries.add(
                new CustomWidgetSummaryInfo(
                        getDrawable(R.drawable.sample_app_suggestions),
                        getResources().getString(R.string.suggestions),
                        true,
                        appSuggestion
                )
        );

        mWidgetSummaries.add(
                new CustomWidgetSummaryInfo(
                        getDrawable(R.drawable.sample_contact_favorite),
                        getResources().getString(R.string.favorites),
                        true,
                        contact
                )
        );

        mWidgetSummaries.add(
                new CustomWidgetSummaryInfo(
                        getDrawable(R.drawable.sample_calendar_widget),
                        getResources().getString(R.string.calendar),
                        false,
                        calendar
                )
        );
    }

    public Drawable getDrawable(int resId){
        return ContextCompat.getDrawable(
                mLauncher,
                resId
        );
    }
}
