package com.amz.ios.launcher.widget;

import android.content.Context;
import android.graphics.Bitmap;

public interface IWidgetPreview {
    Context getContext();
    void applyPreview(Bitmap preview);
}
