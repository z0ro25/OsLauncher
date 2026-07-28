package com.amz.ios.search.entities;

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
import com.amz.ios.search.provider.AdapterItemPresenter;
import com.amz.ios.search.utils.IntentUtils;
import com.amz.ioslauncher.iossearch.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-16 下午8:26
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class ContactItemInfo extends IGroupItemInfoImplAdapter<ContactItemInfo.ContactViewHolder> {

    private static final String TAG = ContactItemInfo.class.getSimpleName();

    private static final boolean DEBUG = MSCConfiguration.DEBUG;
    public static final int SHOW_LEVEL = 0x2;

    //contact info
    public String name;
    public String lookupkey;
    public long contactId;
    public long lastTimeConnected;
    public String phoneNumber;

    /**
     * Each view should has a viewType to register int recycleView;
     */
    public ContactItemInfo() {
        super(MSCConfiguration.MutiItemType.CONTACT_TYPE, R.layout.fmsearch_layout_item_contact);
        setShowLevel(SHOW_LEVEL);
    }

    @Override
    public void realBindViewHolder(ContactViewHolder viewHolder) {
        super.realBindViewHolder(viewHolder);
        final BaseAdapter adapter = (BaseAdapter) viewHolder.mContainer.getAdapter();
        if (mGroup == null) return;
        viewHolder.mGroup.clear();
        viewHolder.mGroup.addAll(mGroup);
        viewHolder.mContainer.setAdapter(adapter);
    }

    @Override
    protected ContactViewHolder generateViewHolderInternal(View itemView) {
        return new ContactViewHolder(itemView, mGroup);
    }

    public class ContactViewHolder extends AdapterItemPresenter.BaseViewHolder {

        private RelativeLayout mContactHead;

        private ListView mContainer;

        private int showlimit;

        private List<BaseCardItemInfo> mGroup;

        public ContactViewHolder(View itemView, List<BaseCardItemInfo> group) {
            super(itemView);
            mGroup = new ArrayList<>();
            if (group != null) {
                mGroup.addAll(group);
            }
            itemView.setBackgroundColor(mContext.getResources().getColor(com.amz.ios.ioslite.common.R.color.white10percent));

            showlimit = MSCConfiguration.CONTACT_SHOW_NUMBER;
            final LayoutInflater layoutInflater = LayoutInflater.from(itemView.getContext());

            if (DEBUG) Log.d(TAG, ">>>>>>ContactViewHolder#ContactViewHolder :" + mGroup);
            //set onclick event
            mContactHead = (RelativeLayout) itemView.findViewById(R.id.contact_head_container);
            mContactHead.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    IntentUtils.toContact(mContext);
                }
            });

            mContainer = (ListView) itemView.findViewById(R.id.contact_content);
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
                    final ContactItemInfo itemInfo = (ContactItemInfo) getItem(position);
                    if (convertView == null) {
                        holder = new ViewHolder();
                        convertView = layoutInflater.inflate(R.layout.fmsearch_layout_contact_content, null);
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
                    final ContactItemInfo itemInfo = (ContactItemInfo) parent.getItemAtPosition(position);
                    if (itemInfo != null){
                        AnalyticsDelegate.onSearchEvent(mContext,UMEventConstants.CONTACT_ITEM_CLICK);
                        IntentUtils.newCall(mContext, itemInfo.phoneNumber);
                    } else {
                        Log.e(TAG, ">>>>>>ContactViewHolder#onItemClick : item info is null!");
                    }
                }
            });
        }
    }

    @Override
    public String toString() {
        return "ContactItemInfo{" +
                "name='" + name + '\'' +
                ", lookupkey='" + lookupkey + '\'' +
                ", contactId=" + contactId +
                ", lastTime=" + lastTimeConnected +
                '}';
    }
}
