package com.amz.ios.launcher.leftpage.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.RoundedImageView;
import com.amz.ios.launcher.TextViewCustomFont;
import com.amz.ios.launcher.leftpage.model.CustomWidgetDetailInfo;
import com.amz.ios.launcher.leftpage.model.CustomWidgetSummaryInfo;
import com.amz.ios.launcher.leftpage.views.CustomContentView;
import com.amz.ios.launcher.leftpage.views.SlidingUpWidgetsAppStyle;
import com.amz.ios.launcher.leftpage.views.SlidingUpWidgetsList;
import com.amz.ios.launcher.slideup.SlidingUpPanelLayout;

import java.util.ArrayList;

public class SlidingUpWidgetListAdapter extends RecyclerView.Adapter {

    public static final int SAMPLE_WEATHER_WIDGET_SQUARE = 10;
    public static final int SAMPLE_WEATHER_WIDGET = 11;
    public static final int SAMPLE_BATTERY_WIDGET = 60;
    public static final int SAMPLE_PHOTO_WIDGET = 30;
    public static final int SAMPLE_APP_SUGGESTIONS = 71;
    public static final int SAMPLE_CONTACT_FAVOURITE = 51;
    public static final int SAMPLE_CALENDAR_WIDGET = 40;

    LayoutInflater mLayoutInflater;
    Launcher mLauncher;

    public ArrayList<CustomWidgetSummaryInfo> summaries;

    public SlidingUpWidgetListAdapter(Launcher launcher, ArrayList<CustomWidgetSummaryInfo> summaryInfos) {
        mLauncher = launcher;
        mLayoutInflater = LayoutInflater.from(launcher);
        summaries = summaryInfos;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(
                viewType == 0 ? R.layout.left_page_sliding_up_widget_item_square : R.layout.left_page_sliding_up_widget_item_full , parent, false
        );
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

        if (position < 0 || position >= summaries.size()) return;

        CustomWidgetSummaryInfo info = summaries.get(position);


        if (holder instanceof ItemViewHolder) {
            final ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            itemViewHolder.mWidgetPreviewIV.setImageDrawable(
                    info.mDrawable
            );

            if (this.summaries.get(position).isFullItem) {
                StaggeredGridLayoutManager.LayoutParams params = ((StaggeredGridLayoutManager.LayoutParams) itemViewHolder.itemView.getLayoutParams());
                params.setFullSpan(true);
            }

            itemViewHolder.mWidgetLabelTV.setText(
                    info.mName
            );
            itemViewHolder.itemView.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        CustomContentView customContentView = mLauncher.mCustomContentView;
                        if (customContentView == null) return;
                        SlidingUpWidgetsList widgetList = customContentView.mSlidingUpWidgetsList;
                        final SlidingUpWidgetsAppStyle appStyleList = customContentView.mSlidingUpWidgetsAppStyle;
                        ArrayList<CustomWidgetDetailInfo> infos = widgetList.mWidgetSummaries.get(itemViewHolder.getAdapterPosition()).mDetails;
                        customContentView.mSlidingUpWidgetsAppStyle.setWidgetsAppStyleList(
                            infos
                        );
                        widgetList.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        appStyleList.requestFocus();
                                        appStyleList.setVisibility(View.VISIBLE);
                                        appStyleList.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                                    }
                                }
                        );
                    }
                }
            );
        }
    }

    @Override
    public int getItemCount() {
        if (summaries == null) return 0;
        return summaries.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (summaries == null) return 0;
        CustomWidgetSummaryInfo summaryInfo = summaries.get(position);
        if (summaryInfo == null) return 0;
        return summaryInfo.isFullItem ? 1 : 0;
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        RoundedImageView mWidgetPreviewIV;
        TextViewCustomFont mWidgetLabelTV;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mWidgetLabelTV = itemView.findViewById(
                    R.id.left_page_sliding_up_widget_item_text
            );
            mWidgetPreviewIV = itemView.findViewById(
                    R.id.left_page_sliding_up_widget_item_image
            );
        }
    }
}
