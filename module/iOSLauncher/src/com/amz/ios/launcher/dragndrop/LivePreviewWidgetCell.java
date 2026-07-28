package com.amz.ios.launcher.dragndrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;
import com.amz.ios.launcher.model.WidgetItem;
import com.amz.ios.launcher.widget.WidgetCell;

/**
 * Extension of {@link WidgetCell} which supports generating previews from {@link RemoteViews}
 */
public class LivePreviewWidgetCell extends WidgetCell {

    private RemoteViews mPreview;

    public LivePreviewWidgetCell(Context context) {
        this(context, null);
    }

    public LivePreviewWidgetCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LivePreviewWidgetCell(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setPreview(RemoteViews view) {
        mPreview = view;
    }

    @Override
    public void ensurePreview() {
        super.ensurePreview();
    }

    /**
     * Generates a bitmap by inflating {@param views}.
     *
     * TODO: Consider moving this to the background thread.
     */
    public static Bitmap generateFromRemoteViews(Context context, RemoteViews views,
            LauncherAppWidgetProviderInfo info, int previewSize, int[] preScaledWidthOut) {

        DeviceProfile dp = LauncherAppState.getIDP(context).portraitProfile;
        int viewWidth = dp.cellWidthPx * info.spanX;
        int viewHeight = dp.cellHeightPx * info.spanY;

        final View v;
        try {
            v = views.apply(context, new FrameLayout(context));
            v.measure(MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(viewHeight, MeasureSpec.EXACTLY));

            viewWidth = v.getMeasuredWidth();
            viewHeight = v.getMeasuredHeight();
            v.layout(0, 0, viewWidth, viewHeight);
        } catch (Exception e) {
            return null;
        }

        preScaledWidthOut[0] = viewWidth;
        final int bitmapWidth, bitmapHeight;
        final float scale;
        if (viewWidth > previewSize) {
            scale = ((float) previewSize) / viewWidth;
            bitmapWidth = previewSize;
            bitmapHeight = (int) (viewHeight * scale);
        } else {
            scale = 1;
            bitmapWidth = viewWidth;
            bitmapHeight = viewHeight;
        }

        Bitmap preview = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(preview);
        c.scale(scale, scale);
        v.draw(c);
        c.setBitmap(null);
        return preview;
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        DeviceProfile profile = LauncherAppState.getIDP(getContext()).portraitProfile;
        int cellSize = (int) (profile.cellWidthPx * WIDTH_SCALE);
        params.width = params.height = cellSize;
        super.setLayoutParams(params);
    }
}
