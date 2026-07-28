package com.amz.ios.search.entities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amz.ios.ioslite.common.analytics.AnalyticsDelegate;
import com.amz.ios.ioslite.common.analytics.UMEventConstants;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.search.config.MSCConfiguration;
import com.amz.ios.search.filesearcher.searchengine.FileItem;
import com.amz.ios.search.provider.AdapterItemPresenter;
import com.amz.ios.search.utils.IntentUtils;
import com.amz.ioslauncher.iossearch.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileItemInfo extends com.amz.ios.search.entities.IGroupItemInfoImplAdapter<FileItemInfo.FileViewHolder> {

    private static final String TAG = FileItemInfo.class.getSimpleName();

    private static final boolean DEBUG = MSCConfiguration.DEBUG;
    public static final int SHOW_LEVEL = 0x5;

    public String name;
    public String filePath;
    public File file;

    /**
     * Each view should has a viewType to register int recycleView;
     */
    public FileItemInfo() {
        super(MSCConfiguration.MutiItemType.FILE_TYPE, R.layout.fmsearch_layout_item_file);
        setShowLevel(SHOW_LEVEL);
    }

    public FileItemInfo(FileItem item) {
        super(MSCConfiguration.MutiItemType.FILE_TYPE, R.layout.fmsearch_layout_item_file);
        setShowLevel(SHOW_LEVEL);
        this.name = item.getName();
        this.filePath = item.getPath();
        this.file = item.getFile();
    }

    @Override
    public void realBindViewHolder(FileViewHolder viewHolder) {
        super.realBindViewHolder(viewHolder);
        final BaseAdapter adapter = (BaseAdapter) viewHolder.mContainer.getAdapter();
        if (mGroup == null) return;
        viewHolder.mGroup.clear();
        viewHolder.mGroup.addAll(mGroup);
        viewHolder.mContainer.setAdapter(adapter);
    }

    @Override
    protected FileViewHolder generateViewHolderInternal(View itemView) {
        return new FileViewHolder(itemView, mGroup);
    }

    public class FileViewHolder extends AdapterItemPresenter.BaseViewHolder {

        private RelativeLayout mFileHead;

        private ListView mContainer;

        private int showlimit;

        private List<BaseCardItemInfo> mGroup;

        public FileViewHolder(View itemView, List<BaseCardItemInfo> group) {
            super(itemView);
            mGroup = new ArrayList<>();
            if (group != null) {
                mGroup.addAll(group);
            }
            itemView.setBackgroundColor(mContext.getResources().getColor(com.amz.ios.ioslite.common.R.color.white10percent));

            showlimit = MSCConfiguration.FILE_SHOW_NUMBER;
            final LayoutInflater layoutInflater = LayoutInflater.from(itemView.getContext());

            if (DEBUG) Log.d(TAG, "FileViewHolder :" + mGroup);

            mFileHead = (RelativeLayout) itemView.findViewById(R.id.file_head_container);

            mContainer = (ListView) itemView.findViewById(R.id.file_content);
            mContainer.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return Math.min(mGroup.size(), showlimit);
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
                    final FileItemInfo itemInfo = (FileItemInfo) getItem(position);
                    if (convertView == null) {
                        holder = new ViewHolder();
                        convertView = layoutInflater.inflate(R.layout.fmsearch_layout_file_content, null);
                        holder.content = (CustomTextView) convertView.findViewById(R.id.content);
                        holder.content.setTextColor(Color.WHITE);
                        convertView.setTag(holder);
                    } else {
                        holder = (ViewHolder) convertView.getTag();
                    }
                    holder.content.setText(itemInfo.name);
                    return convertView;
                }

                class ViewHolder {
                    CustomTextView content;
                }
            });

            mContainer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final FileItemInfo itemInfo = (FileItemInfo) parent.getItemAtPosition(position);
                    if (itemInfo != null){
                        IntentUtils.openFile(mContext, itemInfo);
                    }
                }
            });
        }
    }

    @Override
    public String toString() {
        return "FileItemInfo{" +
                "name='" + name + '\'' +
                "path='" + filePath +
                '}';
    }
}
