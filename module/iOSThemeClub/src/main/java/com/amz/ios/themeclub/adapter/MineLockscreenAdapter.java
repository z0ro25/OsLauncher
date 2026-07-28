package com.amz.ios.themeclub.adapter;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.bean.LockscreenInfo;
import com.amz.ios.themeclub.util.LockScreenUtils;

import java.util.List;


/**
 * Created by ubuntu on 20/06/17.
 */

public class MineLockscreenAdapter extends ArrayAdapter<LockscreenInfo> {


    private static final String TAG = MineThemeAdpter.class.getSimpleName();
    private Context mContext;
    private LayoutInflater mInflater;

    public MineLockscreenAdapter(Context context) {
        super(context, 0);
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
    }

    public void setData(List<LockscreenInfo> data) {
        clear();
        if (data != null) {
            addAll(data);
        }
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.themeclub_mine_theme_item, null);
            holder = new ViewHolder();
            holder.preview = (ImageView) convertView.findViewById(R.id.preview);
            holder.select = (RelativeLayout) convertView.findViewById(R.id.selected);
            holder.themeName = (CustomTextView) convertView.findViewById(R.id.theme_title);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        LockscreenInfo lockscreenInfo = (LockscreenInfo) getItem(position);
        holder.select.setVisibility(View.GONE);
        if(LockScreenUtils.getLockscreenPackage(mContext).equals(lockscreenInfo.getPackageName())){
            holder.select.setVisibility(View.VISIBLE);
        }
        holder.themeName.setText(lockscreenInfo.getTitle());
        Bitmap bitmap = null;
        BitmapDrawable bitmapDrawable = lockscreenInfo.getPreviewThumb();
        if(bitmapDrawable==null){
            if(lockscreenInfo.getPackageName().equals(AppConfig.THEMECLUB_PREVIEW_DEFAULT)) {
                bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.themeclub_default_theme);
                holder.preview.setImageBitmap(bitmap);
            }else{
                bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.theme_no_default2);
                holder.preview.setImageBitmap(bitmap);
            }
        }else{
            holder.preview.setImageBitmap(bitmapDrawable.getBitmap());
        }
        return convertView;
    }

    class ViewHolder {
        ImageView preview;
        RelativeLayout select;
        CustomTextView themeName;
    }
}
