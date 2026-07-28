package com.amz.ios.launcher.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.media.midi.MidiManager;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.ItemInfo;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.StylusEventHelper;
import com.amz.ios.launcher.WidgetPreviewLoader;

public class WidgetAppStyleCell extends LinearLayout implements View.OnLayoutChangeListener, IWidgetPreview {

    public static final int[] WIDGET_CELL = {R.attr.hideWidgetText};

    WidgetImageView mWidgetPreview;
    TextView mWidgetName;
    TextView mWidgetDims;
    LinearLayout mWidgetText;
    StylusEventHelper mStylusEventHelper;
    String mWidgetDimenStrFormat;
    Launcher mLauncher;
    DeviceProfile mGrid;
    WidgetPreviewLoader mPreviewLoader;
    WidgetPreviewLoader.PreviewLoadRequest mActiveRequest;
    Parcelable mParcelable;
    int mSize;
    int mWidth;
    int mHeight;

    public WidgetAppStyleCell(Context context) {
        super(context);

        mLauncher = (Launcher) context;
        mGrid = mLauncher.getDeviceProfile();
        mPreviewLoader = LauncherAppState.getInstance().getWidgetCache();

        TypedArray a = context.obtainStyledAttributes(null,WIDGET_CELL,0,0);
        boolean z = a.getBoolean(0, false);
        a.recycle();

        LayoutInflater.from(context).inflate(R.layout.widget_app_style_cell,this,true);

        mWidgetPreview = findViewById(R.id.widget_preview);
        mWidgetName = findViewById(R.id.widget_name);
        mWidgetDims = findViewById(R.id.widget_dims);
        mWidgetText = findViewById(R.id.widget_text);

        if (z){
            mWidgetText.setVisibility(View.GONE);
        }

        mStylusEventHelper = new StylusEventHelper(this);

        Resources resources = context.getResources();

        mWidgetDimenStrFormat = resources.getString(R.string.widget_dims_format);
        mSize = (int) (mGrid.cellWidthPx * 3.6f);

        mHeight = (int) (mSize * 0.8f);
//        else {
            mWidth = mHeight;
//        }

        setWillNotDraw(false);
        setClipToPadding(false);
        setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());

        LinearLayout.LayoutParams layoutParams;

//        if (getId() == R.id.widget_full_preview_item){
//            layoutParams = new LinearLayout.LayoutParams(-1,-1);
//        }
//        else {
            layoutParams = new LinearLayout.LayoutParams(mSize,mSize);
//        }

        int margin = z ? 0 : mGrid.edgeMarginPx;

        layoutParams.rightMargin = margin;
        layoutParams.leftMargin = margin;
        layoutParams.gravity = 17;

        setLayoutParams(layoutParams);
        setOrientation(LinearLayout.VERTICAL);
        setFocusable(true);
    }

    public void ensurePreview(){
        if (mActiveRequest != null) {
            return;
        }
        int[] previewSize = getPreviewSize();

        int width = previewSize[0];
        int height = previewSize[1];

        mActiveRequest = mPreviewLoader.getPreview(
                this.mParcelable,
                width,
                height,
                this
        );
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        removeOnLayoutChangeListener(this);
        ensurePreview();
    }

    public int getActualItemWidth() {
        return Math.min(
                getPreviewSize()[0],
                ((ItemInfo) getTag()).spanX * mGrid.cellWidthPx);
    }

    public int[] getPreviewSize() {
        return new int[]{mWidth, mHeight};
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean touchEvent = super.onTouchEvent(event);
        if (mStylusEventHelper.checkAndPerformStylusEvent(event)) return true;
        return touchEvent;
    }

    @Override
    public void applyPreview(Bitmap preview) {
        if (preview != null){
            mWidgetPreview.setBitmap(preview);
            mWidgetPreview.setAlpha(0.0f);
            mWidgetPreview.animate().alpha(1.0f).setDuration(90L);
        }
    }

    public void setDims(int width, int height){
        String str = String.format(mWidgetDimenStrFormat,width, height);
        if (mWidgetDims != null){
            mWidgetDims.setText(str);
        }
    }
}
