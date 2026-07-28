package com.amz.ios.themeclub.adapter;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

public class MyCoverFlowAdapter extends CoverFlowAdapter {

    private boolean dataChanged;
    private List<Bitmap> mBitmaps = new ArrayList<>();

    public MyCoverFlowAdapter(Context context, List<Bitmap> bitmaps) {
        for (int i = 0; i < bitmaps.size(); i++)
            mBitmaps.add(bitmaps.get(i));
//        image1 = BitmapFactory.decodeResource(context.getResources(),
//                R.drawable.footprint_header_bg1);
//
//        image2 = BitmapFactory.decodeResource(context.getResources(),
//                R.drawable.ic_launcher);
    }

    public void changeBitmap() {
        dataChanged = true;

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (mBitmaps != null)
            return mBitmaps.size();
        return 0;
    }

    @Override
    public Bitmap getImage(final int position) {
        if (position < mBitmaps.size())
            return mBitmaps.get(position);
        return null;
    }
}
