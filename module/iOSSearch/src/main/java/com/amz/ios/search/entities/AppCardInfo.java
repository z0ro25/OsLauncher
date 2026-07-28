package com.amz.ios.search.entities;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.ioslite.common.util.DisplayUtil;
import com.amz.ios.http.Internal.DroiRequestQueue;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.search.config.MSCConfiguration;
import com.amz.ios.search.provider.AdapterItemPresenter;
import com.amz.ios.search.utils.IntentUtils;
import com.amz.ios.search.widget.GridViewExtend;
import com.amz.ioslauncher.iossearch.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-15 上午11:35
 */
public class AppCardInfo extends IGroupItemInfoImplAdapter<AppCardInfo.HotAppViewHolder> {

    private static final String TAG = AppCardInfo.class.getSimpleName();

    private static final boolean DEBUG = false;//MSCConfiguration.DEBUG;

    public static final int SHOW_LEVEL = 0;

    // for online info
    public String apkId;
    public long downloadNum;
    public String fileSize;
    public String apkName;
    public String downloadUrl;
    public String iconUrl = "";
    public String packageName;
    public View adView;

    public String target;

    // for local info
    public String name;
    public ComponentName componentName;
    public String action;
    public ActivityInfo mIcon;
    public LauncherAppInfo lancherApp;

    private int mItemViewMaxLimit;
    private int mItemViewMinLimit;

    public static final int FLAG_ONLINE_APP = 0x1;
    public static final int FLAG_LOCAL_APP = 0x2;
    private int mDataType = FLAG_ONLINE_APP;

    private String mHeadText;

    public AppCardInfo() {
        this(FLAG_ONLINE_APP);
    }

    public AppCardInfo(int dataType) {
        super(MSCConfiguration.MutiItemType.APP_TYPE, R.layout.fmsearch_layout_item_hotapp);
        mDataType = dataType;
        mItemViewMaxLimit = MSCConfiguration.DEFAULT_WEIGHT_MAX_ITEM_NUMBER;
        mItemViewMinLimit = MSCConfiguration.DEFAULT_WEIGHT_MIN_ITEM_NUMBER;
        setShowLevel(SHOW_LEVEL);
    }

    @Override
    public void realBindViewHolder(HotAppViewHolder viewHolder) {
        super.realBindViewHolder(viewHolder);
        final BaseAdapter adapter = (BaseAdapter) viewHolder.flowLayout.getAdapter();
        if (TextUtils.isEmpty(mHeadText)) {
            mHeadText = viewHolder.flowLayout.getContext()
                    .getString(mDataType == FLAG_ONLINE_APP ? R.string.fmsearch_txt_head_online_app : R.string
                            .fmsearch_txt_head_local_app);
        }
        viewHolder.mGroup.clear();
        viewHolder.mGroup.addAll(mGroup);
        viewHolder.mHeadTxt.setText(mHeadText);
        viewHolder.mHeadTxt.setTextColor(Color.WHITE);

//        adapter.notifyDataSetChanged();
        viewHolder.flowLayout.setAdapter(adapter);
    }

    @Override
    protected HotAppViewHolder generateViewHolderInternal(View itemView) {
        return new HotAppViewHolder(itemView, mGroup);
    }

    public static class HotAppViewHolder extends AdapterItemPresenter.BaseViewHolder {

        private GridViewExtend flowLayout;
        private CustomTextView mHeadTxt;

        private List<BaseCardItemInfo> mGroup;

        private int mScreenWidth;
        private float mItemWith;
        private float mItemHeight;
        private float mOuterSideMargin;

        public HotAppViewHolder(final View itemView, List<BaseCardItemInfo> group) {
            super(itemView);
            mGroup = new ArrayList<>();
            if (group != null) {
                mGroup.addAll(group);
            }

            final Resources res = mContext.getResources();
            itemView.setBackgroundColor(res.getColor(com.amz.ios.ioslite.common.R.color.white10percent));
            final LayoutInflater laoutinflater = LayoutInflater.from(mContext);
            flowLayout = (GridViewExtend) itemView.findViewById(R.id.hotapp_content);
            final DroiRequestQueue quen = DroiRequestQueue.getInstance(mContext);
            //final DroiLRUImageLoader imageLoader = DroiLRUImageLoader.getInstance(mContext);
            mHeadTxt = (CustomTextView) itemView.findViewById(R.id.hotapp_head);
            final PackageManager pm = mContext.getPackageManager();

            mItemWith = res.getDimension(R.dimen.fmsearch_dp_item_hotapp_width);
            mItemHeight = res.getDimension(R.dimen.fmsearch_dp_item_hotapp_height);
            mOuterSideMargin = res.getDimension(R.dimen.fmsearch_dp_item_inner_common_padding);
            mScreenWidth = DisplayUtil.getScreenWidthInPx(mContext);

            //for screen change
            final int num = (int) ((mScreenWidth - mOuterSideMargin * 2) / mItemWith);
            flowLayout.setNumColumns(num);

            flowLayout.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    //ui has two line content
                    return Math.min(mGroup.size(), num * 2);
                }

                @Override
                public Object getItem(int position) {
                    return mGroup.get(position);
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }


                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    final ViewHolder holder;
                    final ViewAdHolder adHolder;
                    final AppCardInfo itemInfo = (AppCardInfo) getItem(position);
                    if (itemInfo.mDataType == FLAG_ONLINE_APP) {
                        convertView = itemInfo.adView;
                    } else {
                        if (convertView == null) {
                            holder = new ViewHolder();
                            convertView = laoutinflater.inflate(R.layout.fmsearch_layout_app_content, null);
                            holder.appView = (CustomTextView) convertView.findViewById(R.id.txt_app);
                            holder.image = (ImageView) convertView.findViewById(R.id.image);
                            convertView.setTag(holder);
                        } else {
                            holder = (ViewHolder) convertView.getTag();
                        }
                        if (parent.getChildCount() == position) {
                            if (itemInfo.lancherApp != null) {
                                holder.image.setImageBitmap(itemInfo.lancherApp.getIconBitmap());
                                holder.appView.setText(itemInfo.lancherApp.getLabel());
                            } else {
//                                holder.image.setImageDrawable(itemInfo.mIcon.loadIcon(pm));
                                LauncherAppState app = LauncherAppState.getInstance();
                                Bitmap bitmap = app.getThemeManager().getmIconCache().getIconDrawable(itemInfo.componentName);
                                holder.image.setImageBitmap(bitmap);

                                holder.appView.setText(itemInfo.name);
                            }
                        }
                        flowLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                final AppCardInfo itemInfo = (AppCardInfo) parent.getItemAtPosition(position);
                                Log.d(TAG, ">>>>>>HotAppViewHolder#onItemClick :" + itemInfo);
                                if (itemInfo != null) {
                                    AnalyticsDelegate.onSearchEvent(mContext,UMEventConstants.APPS_ITEM_CLICK);

                                    if (!TextUtils.isEmpty(itemInfo.target)) {
                                        IntentUtils.openUrl(mContext, itemInfo.target);
                                        return;
                                    }

                                    if (IntentUtils.toApp(mContext, itemInfo.packageName, ""))
                                        return;

                                    if (TextUtils.isEmpty(itemInfo.apkId)) {
                                        IntentUtils.downloadApp(mContext, itemInfo.downloadUrl);
                                    } else {
                                        IntentUtils.toDroiAppDetail(mContext, itemInfo.apkId, itemInfo.packageName);
                                    }
                                } else {
                                    Log.e(TAG, ">>>>>>HotAppViewHolder#onItemClick : item info is null!");
                                }
                            }
                        });

                    }
                    return convertView;
                }

                class ViewHolder {
                    CustomTextView appView;
                    ImageView image;
                }

                class ViewAdHolder {
                    LinearLayout adLayout;
                }
            });

        }
    }


    @Override
    public String toString() {
        return "AppCardInfo{" +
                "adView='" + adView + '\'' +
                /*", lancherApp=" + lancherApp +
                ", apkName='" + apkName + '\'' +*/
                '}';
    }
}
