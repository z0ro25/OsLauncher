package com.amz.ios.gpuimage;

import android.content.Context;
import android.graphics.Bitmap;

import com.amz.ios.gpuimage.filter.GPUImageGaussianBlurFilter;

public class GausianBlur {

    private static GausianBlur instance = null;
    private GPUImage gpuimage;

    public static GausianBlur getInstance() {
        if (instance == null) {
            instance = new GausianBlur();
        }

        return instance;
    }

    public void setup(Context context) {
        gpuimage = new GPUImage(context);
        String vertextShaderString = GPUImageGaussianBlurFilter.vertexShaderForStandardBlurOfRadius(20, 10);
        String fragmentShaderString = GPUImageGaussianBlurFilter.fragmentShaderForStandardBlurOfRadius(20, 10);
        GPUImageGaussianBlurFilter blurFilter = new GPUImageGaussianBlurFilter(fragmentShaderString, vertextShaderString, 1);
        gpuimage.setFilter(blurFilter);
    }

    public Bitmap getBlurBitmap(Bitmap bitmap) {
        gpuimage.setImage(bitmap);
        return gpuimage.getBitmapWithFilterApplied();
    }
}

