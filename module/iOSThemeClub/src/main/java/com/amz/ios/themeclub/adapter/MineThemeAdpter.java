package com.amz.ios.themeclub.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestManager;
import com.amz.ios.ioslite.common.config.ThemeConfig;
import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.themeclub.R;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.bean.ThemeInfo;
import com.amz.ios.themeclub.util.ImageUtils;

/**
 * Created by GL on 16-12-6.
 */

public class MineThemeAdpter extends CursorAdapter{
    private static final String TAG = MineThemeAdpter.class.getSimpleName();
    private Context mContext;
    private LayoutInflater mInflater;
    private ViewHolder holder = null;

    private RequestManager mGlide;

    private final int maxMemory = (int) Runtime.getRuntime().maxMemory();
    private final int cacheSize = maxMemory / 2;
    private LruCache<String, Bitmap> mLruCache = new LruCache<String, Bitmap>(
            cacheSize) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
        }
    };

    public MineThemeAdpter(Context context, Cursor cursor, Boolean autoRequery){
        super(context, cursor, autoRequery);
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mGlide = Glide.with(mContext);
    }

    @SuppressLint("Range")
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String packageName = "";
        packageName = cursor.getString(cursor.getColumnIndex(ThemeInfo.Impl.THEME_PACKAGE_NAME));
        holder = (ViewHolder) view.getTag();
        if (mContext.getPackageName().equals(packageName) && ThemeConfig.getDefaultThemePkg().equals(packageName)) {
            holder.themeName.setText(R.string.themeclub_default_thmem);
        } else {
            holder.themeName.setText(cursor.getString(cursor.getColumnIndex(ThemeInfo.Impl.THEME_TITLE)));
        }

        holder.themeName.setBackgroundColor(mContext.getResources().getColor(R.color.themeclub_theme_item_text_background));
        holder.select.setVisibility(View.GONE);
        if (IOSSettings.getString(mContext.getContentResolver(),
                IOSSettings.Launcher.LAUNCHER_THEME_PACKAGE, mContext.getPackageName()).equals(packageName)) {
                holder.select.setVisibility(View.VISIBLE);
            holder.themeName.setBackgroundColor(mContext.getResources().getColor(R.color.themeclub_theme_item_text_background_selected));
        }

        synchronized (mLruCache) {
            if (mLruCache.get(packageName) == null) {
                Bitmap bitmap = ImageUtils.Bytes2Bitmap(cursor.getBlob(cursor.getColumnIndex(ThemeInfo.Impl.THEME_THUMB)));
                mLruCache.put(packageName, bitmap);
                holder.preview.setImageBitmap(bitmap);
//                mGlide.load(cursor.getBlob(cursor.getColumnIndex(ThemeInfo.Impl.THEME_THUMB))).asBitmap().into(holder.preview);
            } else {
                holder.preview.setImageBitmap(mLruCache.get(packageName));
            }
        }

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View rootView = null;
        rootView = mInflater.inflate(R.layout.themeclub_mine_theme_item, null);
        holder = new ViewHolder();
        holder.preview = (ImageView) rootView.findViewById(R.id.preview);
        holder.select = (RelativeLayout) rootView.findViewById(R.id.selected);
        holder.themeName = (CustomTextView) rootView.findViewById(R.id.theme_title);
        rootView.setTag(holder);
        return rootView;
    }

    @Override
    public Object getItem(int position) {
        return super.getItem(position);
    }

    class ViewHolder {
        ImageView preview;
        RelativeLayout select;
        CustomTextView themeName;
    }
}
