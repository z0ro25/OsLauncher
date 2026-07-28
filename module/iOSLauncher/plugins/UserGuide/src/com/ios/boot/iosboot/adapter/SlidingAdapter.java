package com.ios.boot.iosboot.adapter;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import androidx.viewpager.widget.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.amz.ios.launcher.R;

/**
 * Created by YiYang on 16-11-30.
 */

public class SlidingAdapter extends PagerAdapter {

    private Context mContext;

    public SlidingAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = View.inflate(container.getContext(), R.layout.viewpager_item, null);
        final ImageView imageView = (ImageView) view.findViewById(R.id.viewpager_item_iv);
        if (position == 0) {
            new AsyncTask<Void, Void, Drawable>() {
                @Override
                protected Drawable doInBackground(Void... params) {
                    try {
                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext.getApplicationContext());
                        return wallpaperManager.getDrawable();
                    } catch (Exception e) {
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Drawable bitmap) {
                    if (bitmap != null) {
                        imageView.setBackgroundDrawable(bitmap);
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            imageView.setBackgroundResource(com.ios.theme.def.R.drawable.default_wallpaper);
        }
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        container.removeView((View) object);
    }
}
