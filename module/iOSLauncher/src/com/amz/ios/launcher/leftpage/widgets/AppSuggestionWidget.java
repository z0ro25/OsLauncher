package com.amz.ios.launcher.leftpage.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.leftpage.model.AppSuggestionInfo;
import com.amz.ios.launcher.leftpage.adapter.AppSuggestionAdapter;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class AppSuggestionWidget extends BlurConstraintLayoutWidget {

    RecyclerView mAppSuggestionRV;
    Launcher mLauncher;
    Handler mHandler;
    int mMargin;
    AppSuggestionAdapter mAppSuggestionAdapter;
    Runnable mAppSuggestionReloadRunnable;

    public AppSuggestionWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppSuggestionWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (context instanceof Launcher) {
            mLauncher = (Launcher) context;
            mMargin = mLauncher.getDeviceProfile().edgeMarginPx;
        }

        this.mHandler = new Handler();
        this.mAppSuggestionReloadRunnable = new Runnable() {
            @Override
            public void run() {
                reloadSuggestionApps(AppSuggestionWidget.this);
            }
        };

        LayoutInflater.from(context).inflate(R.layout.app_suggestions_widget,this,true);
    }

    public void getAppSuggestions(){
        try {
            mAppSuggestionAdapter.mAppSuggestionInfoArrayList = new AppSuggestionCallable().call();
            mAppSuggestionAdapter.notifyDataSetChanged();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void reloadSuggestionApps(AppSuggestionWidget widget){
        if (widget != null)
            widget.getAppSuggestions();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAppSuggestionRV = findViewById(R.id.app_suggestions_widget_content);
        mAppSuggestionRV.setItemAnimator(
                new DefaultItemAnimator()
        );
        mAppSuggestionAdapter = new AppSuggestionAdapter(mLauncher);
        mAppSuggestionRV.setAdapter(mAppSuggestionAdapter);
        mAppSuggestionRV.setLayoutManager(new GridLayoutManager(mLauncher, 4));
        ((ConstraintLayout.LayoutParams) mAppSuggestionRV.getLayoutParams()).setMargins(
                mMargin,
                mMargin,
                mMargin,
                mMargin
        );

        mAppSuggestionRV.addItemDecoration(new AppSuggestionDecoration(mMargin));
        this.mHandler.removeCallbacksAndMessages(null);
        this.mHandler.postDelayed(this.mAppSuggestionReloadRunnable, 1000L);
        setTextAndBackgroundColor(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    public class AppSuggestionCallable implements Callable<ArrayList<AppSuggestionInfo>> {
        @Override
        public ArrayList<AppSuggestionInfo> call() throws Exception {
            ArrayList<AppSuggestionInfo> arrayList = new ArrayList<>();

            String path = mLauncher.getDatabasePath("app_icons.db").getPath();
            Cursor query = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).query(true,"icons", null, null, null, null, null, "history DESC", "8");
            if (!path.isEmpty() && query != null) {
                int index = 0;
                while (query.moveToNext()) {
                    try {
                        String label = query.getString(query.getColumnIndexOrThrow("label"));
                        String componentName = query.getString(query.getColumnIndexOrThrow("componentName"));
                        @SuppressLint("Range") byte[] blob = query.getBlob(query.getColumnIndex("icon"));
                        AppSuggestionInfo info = new AppSuggestionInfo(index, label, componentName, BitmapFactory.decodeByteArray(blob, 0, blob.length));
                        index++;
                        arrayList.add(info);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                query.close();
            }
            return arrayList;
        }
    }

    @Override
    public void r() {
        super.r();
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mAppSuggestionRV.getLayoutParams();
        layoutParams.setMargins(
                mMargin,
                mMargin,
                mMargin,
                mMargin
        );
        mAppSuggestionRV.setLayoutParams(layoutParams);
    }

    @Override
    public void setAppSuggestionViewMargin() {
        super.setAppSuggestionViewMargin();
        ((ConstraintLayout.LayoutParams) mAppSuggestionRV.getLayoutParams()).setMargins(
                mMargin,
                mMargin,
                mMargin,
                mMargin
        );
        this.mHandler.removeCallbacksAndMessages(null);
        this.mHandler.postDelayed(this.mAppSuggestionReloadRunnable, 1000L);
    }

    public static class AppSuggestionDecoration extends RecyclerView.ItemDecoration {
        final int margin;

        public AppSuggestionDecoration(int m)    {
            this.margin = m / 2;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            parent.setPadding(margin, margin, margin, margin);
            parent.setClipToPadding(false);
            outRect.top = margin;
            outRect.bottom = margin;
            outRect.left = margin;
            outRect.right = margin;
        }
    }

}
