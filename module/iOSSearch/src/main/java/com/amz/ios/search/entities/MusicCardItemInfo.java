package com.amz.ios.search.entities;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.search.config.MSCConfiguration;
import com.amz.ios.search.provider.AdapterItemPresenter;
import com.amz.ios.search.utils.IntentUtils;
import com.amz.ioslauncher.iossearch.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-19 上午11:40
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class MusicCardItemInfo extends IGroupItemInfoImplAdapter<MusicCardItemInfo.MusicCardViewHolder> {

    private static final String TAG = MusicCardItemInfo.class.getSimpleName();

    /**
     * internal storage
     */
    public static final int LOCAL_INTERNAL = 0x1;
    /**
     * external storage
     */
    public static final int LOCAL_EXTERNAL = 0x2;

    public String musicPath;
    public String musicAlbum;
    public String musicName;
    public String musicArtist;
    public String musicAlbumKey;
    public String musicAlbumArtPath;
    public String musicMineType;
    public long musicId;
    public int musicLocal;

    /**
     * Each view should has a viewType to register int recycleView;
     */
    public MusicCardItemInfo() {
        super(MSCConfiguration.MutiItemType.MUSIC_TYPE, R.layout.fmsearch_layout_item_music);
    }

    @Override
    public void realBindViewHolder(MusicCardViewHolder viewHolder) {
        super.realBindViewHolder(viewHolder);
        final BaseAdapter adapter = (BaseAdapter) viewHolder.mContent.getAdapter();
        viewHolder.mGroup.clear();
        viewHolder.mGroup.addAll(mGroup);
        viewHolder.mContent.setAdapter(adapter);
    }

    @Override
    protected MusicCardViewHolder generateViewHolderInternal(View itemView) {
        return new MusicCardViewHolder(itemView, mGroup);
    }

    public class MusicCardViewHolder extends AdapterItemPresenter.BaseViewHolder {

        private ListView mContent;

        private List<BaseCardItemInfo> mGroup;

        public MusicCardViewHolder(View itemView, List<BaseCardItemInfo> group) {
            super(itemView);
            mGroup = new ArrayList<>();
            if (group != null) {
                mGroup.addAll(group);
            }

            final Context context = itemView.getContext();
            final int showLimit = MSCConfiguration.MUSIC_SHOW_NUMBER;
            final LayoutInflater inflater = LayoutInflater.from(context);
            itemView.findViewById(R.id.head_container).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    IntentUtils.toMusicPlay(context);
                }
            });

            mContent = (ListView) itemView.findViewById(R.id.music_content);
            mContent.setAdapter(new BaseAdapter() {

                @Override
                public int getCount() {
                    return Math.min(showLimit, mGroup.size());
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
                    if (convertView == null) {
                        holder = new ViewHolder();
                        convertView = inflater.inflate(R.layout.fmsearch_layout_music_content, null);
                        holder.mAuthor = (CustomTextView) convertView.findViewById(R.id.author);
                        holder.mTitle = (CustomTextView) convertView.findViewById(R.id.title);
                        convertView.setTag(holder);
                    } else {
                        holder = (ViewHolder) convertView.getTag();
                    }
                    final MusicCardItemInfo itemInfo = (MusicCardItemInfo) getItem(position);
                    holder.mTitle.setText(itemInfo.musicName);
                    holder.mAuthor.setText(itemInfo.musicArtist);
                    return convertView;
                }
            });

            mContent.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final MusicCardItemInfo itemInfo = (MusicCardItemInfo) parent.getItemAtPosition(position);
                    if (itemInfo == null) {
                        Log.e(TAG, ">>>>>>MusicCardViewHolder#onItemClick : item is null!");
                        return;
                    }
                    AnalyticsDelegate.onSearchEvent(mContext,UMEventConstants.MUSIC_ITEM_CLICK);
                    IntentUtils.openMusic(context, itemInfo.musicId, itemInfo.musicPath, itemInfo.musicMineType);
                }
            });
        }

        private class ViewHolder {
            CustomTextView mTitle;
            CustomTextView mAuthor;
        }
    }
}
