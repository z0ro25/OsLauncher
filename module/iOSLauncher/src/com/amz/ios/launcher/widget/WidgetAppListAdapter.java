package com.amz.ios.launcher.widget;

import android.content.ComponentName;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.DeviceProfile;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.LauncherAppState;
import com.amz.ios.launcher.LauncherAppWidgetProviderInfo;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;
import com.amz.ios.launcher.WidgetPreviewLoader;
import com.amz.ios.launcher.compat.AppWidgetManagerCompat;
import com.amz.ios.launcher.leftpage.widgets.WidgetBaseLayout;
import com.amz.ios.launcher.model.PackageItemInfo;
import com.amz.ios.launcher.model.WidgetsModel;
import com.google.gson.internal.$Gson$Preconditions;

import java.util.ArrayList;

public class WidgetAppListAdapter extends RecyclerView.Adapter {

    public Launcher mLauncher;
    LayoutInflater mLayoutInflater;
    WidgetsModel mWidgetsModel;
    WidgetPreviewLoader mWidgetPreviewLoader;
    View.OnClickListener onClickListener;
    View.OnLongClickListener onLongClickListener;
    IWidgetListAdapter mWidgetListDelegate;
    boolean shouldUpdate = false;
    AppWidgetManagerCompat mAppWidgetManagerCompat;
    DeviceProfile mGrid;
    int mMargin;

    public interface IWidgetListAdapter {

    }

    public WidgetAppListAdapter(Launcher launcher, View.OnClickListener clickListener, View.OnLongClickListener longClickListener, IWidgetListAdapter delegate) {
        this.mLauncher = launcher;
        this.mGrid = launcher.getDeviceProfile();
        this.mLayoutInflater = LayoutInflater.from(mLauncher);
        this.onClickListener = clickListener;
        this.onLongClickListener = longClickListener;
        this.mWidgetListDelegate = delegate;
        this.mAppWidgetManagerCompat = AppWidgetManagerCompat.getInstance(mLauncher);
        this.mWidgetPreviewLoader = LauncherAppState.getInstance().getWidgetCache();
        this.mMargin = mGrid.edgeMarginPx / 2;
    }

    public void setWidgetModel(WidgetsModel widgetModel){
        this.mWidgetsModel = widgetModel;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1){
            View view = mLayoutInflater.inflate(R.layout.widgets_full_row_view, parent, false);
            return new IOSWidgetViewHolder(view);
        }
        else {
            View view = mLayoutInflater.inflate(R.layout.widgets_list_row_view,parent,false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        PackageItemInfo info = mWidgetsModel.mFilteredPackageInfo.get(position);
        if (info == null) return;

        final ArrayList<Object> widgets = mWidgetsModel.mFilteredWidgetList.get(info);

        shouldUpdate = true;
        if (widgets == null) return;
        if (holder instanceof IOSWidgetViewHolder) {
            IOSWidgetViewHolder iosWidgetViewHolder = (IOSWidgetViewHolder) holder;
            ArrayList<Object> iosWidgets = mWidgetsModel.getIOSWidgets();
            for (Object obj : iosWidgets){
                LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo = (LauncherAppWidgetProviderInfo) obj ;
                PendingAddWidgetInfo pendingAddWidgetInfo = new PendingAddWidgetInfo(mLauncher,launcherAppWidgetProviderInfo,null);
                ComponentName provider = launcherAppWidgetProviderInfo.provider;
                if (provider == null) return;

                View view = null;
                TextViewCustomFont mLabelTV = null;

                if (provider.getClassName().contains("BatteryWidgetProvider")){
                    view = iosWidgetViewHolder.mWidget1;
                    mLabelTV = iosWidgetViewHolder.mWidgetName1;
                }
                if (provider.getClassName().contains("PictureAppWidgetProvider")){
                    view = iosWidgetViewHolder.mWidget2;
                    mLabelTV = iosWidgetViewHolder.mWidgetName2;
                }

                if (view != null){
                    view.setVisibility(View.VISIBLE);
                    view.setTag(pendingAddWidgetInfo);
                    view.setOnClickListener(onClickListener);
                    view.setOnLongClickListener(onLongClickListener);
                }

                if (mLabelTV != null){
                    mLabelTV.setText(mAppWidgetManagerCompat.loadLabel(launcherAppWidgetProviderInfo));
                }

                iosWidgetViewHolder.itemView.setPadding(
                        mMargin,
                        mMargin,
                        mMargin,
                        mMargin
                );
            }
        }
        else if (holder instanceof ItemViewHolder) {
            final ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            itemViewHolder.mWidgetSection.applyFromPackageItemInfo(info);
            itemViewHolder.mWidgetSection.setTextVisibility(false);
            itemViewHolder.mWidgetSection.setText("");
            itemViewHolder.mWidgetAppName.setText(info.title);
            itemViewHolder.mWidgetSection.setOnLongClickListener(onLongClickListener);
            itemViewHolder.mWidgetSection.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!shouldUpdate){
                                notifyItemChanged(position);
                            }
                            if (mWidgetListDelegate != null){
                                if (mWidgetListDelegate instanceof WidgetsContainerView) {
                                    final WidgetsContainerView containerView = (WidgetsContainerView) mWidgetListDelegate;
                                    mLauncher.mWidgetsAppStyle.setData(
                                            widgets,
                                            onClickListener,
                                            onLongClickListener
                                    );
                                    mLauncher.mWidgetsAppStyle.post(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    containerView.expandAppStyleView();
                                                }
                                            }
                                    );
                                }
                            }
                            shouldUpdate = false;
                        }
                    }
            );
            itemViewHolder.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            itemViewHolder.mWidgetSection.performClick();
                        }
                    }
            );
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {

        int prevCount = mWidgetsModel.mFilteredPackageInfo.size();
        int currentCount = mWidgetsModel.getPackageSize();
        if (prevCount == currentCount && position == 0) return 1;
        return 2;
    }

    @Override
    public int getItemCount() {
        if (this.mWidgetsModel == null) return 0;
        if (this.mWidgetsModel.mFilteredPackageInfo == null) return 0;
        return this.mWidgetsModel.mFilteredPackageInfo.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        BubbleTextView mWidgetSection;
        TextViewCustomFont mWidgetAppName;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mWidgetSection = itemView.findViewById(R.id.section);
            mWidgetAppName = itemView.findViewById(R.id.widget_app_name);
        }


    }

    public static class IOSWidgetViewHolder extends RecyclerView.ViewHolder {

        TextViewCustomFont mWidgetName1;
        TextViewCustomFont mWidgetName2;
        WidgetBaseLayout mWidget1;
        WidgetBaseLayout mWidget2;

        public IOSWidgetViewHolder(@NonNull View itemView) {
            super(itemView);
            Launcher launcher = (Launcher) itemView.getContext();
            int margin = launcher.getDeviceProfile().edgeMarginPx;
            mWidget1 = itemView.findViewById(R.id.widget_square_preview_item_1);
            mWidget2 = itemView.findViewById(R.id.widget_square_preview_item_2);
            mWidget1.setPadding(margin,margin,margin,margin);
            mWidget2.setPadding(margin,margin,margin,margin);
            mWidgetName1 = itemView.findViewById(R.id.widget_square_preview_text_1);
            mWidgetName2 = itemView.findViewById(R.id.widget_square_preview_text_2);
        }

        public void setListeners(View.OnClickListener onClickListener, View.OnLongClickListener onLongClickListener){
            mWidget1.setOnClickListener(onClickListener);
            mWidget1.setOnLongClickListener(onLongClickListener);
            mWidget2.setOnClickListener(onClickListener);
            mWidget2.setOnLongClickListener(onLongClickListener);
        }
    }

}
