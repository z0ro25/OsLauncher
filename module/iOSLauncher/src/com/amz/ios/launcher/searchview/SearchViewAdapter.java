package com.amz.ios.launcher.searchview;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.ContentObserver;
import android.icu.number.UnlocalizedNumberFormatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.AppInfo;
import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.IOSAppWidget;
import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;

import java.util.ArrayList;
import java.util.Iterator;

public class SearchViewAdapter extends RecyclerView.Adapter implements Filterable {

    public static final int MAX_SEARCH_ITEM_SIZE = 8;

    Launcher mLauncher;
    ArrayList<AppInfo> mApplicationInfoList = new ArrayList<>();
    ArrayList<AppInfo> mSearchedInfoList = new ArrayList<>();
    Filter mFilter;

    public SearchViewAdapter(Context context, ArrayList<AppInfo> apps){
        mLauncher = (Launcher) context;
        mApplicationInfoList.clear();
        mApplicationInfoList.addAll(apps);
        mFilter = new SearchFilter();
        mSearchedInfoList = getSearchedInfoList();
    }

    public class SearchFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<AppInfo> applicationInfoArrayList = new ArrayList<>();
            FilterResults filterResults = new FilterResults();

            if (constraint != null && constraint.length() != 0){
                String trim = constraint.toString().toLowerCase().trim();
                for (AppInfo next : mApplicationInfoList) {
                    if (next != null && next.title != null && next.title.toString().toLowerCase().contains(trim)) {
                        applicationInfoArrayList.add(next);
                    }
                }
            }
            else {
                applicationInfoArrayList.addAll(getSearchedInfoList());
            }
            filterResults.values = applicationInfoArrayList;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mSearchedInfoList.clear();
            mSearchedInfoList.addAll((ArrayList<AppInfo>) results.values);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.search_item,parent,false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < 0 || position >= mSearchedInfoList.size()) return;
        AppInfo appInfo = mSearchedInfoList.get(position);
        if (holder instanceof ListViewHolder) {
            ListViewHolder listViewHolder = (ListViewHolder) holder;
            BubbleTextView bubbleTextView = (BubbleTextView) listViewHolder.itemView;
            bubbleTextView.setTag(appInfo);
            bubbleTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            bubbleTextView.reapplyItemInfo(appInfo);
            bubbleTextView.setOnClickListener(
                mLauncher
            );
        }
    }

    @Override
    public int getItemCount() {
        if (mSearchedInfoList == null) return 0;
        return mSearchedInfoList.size();
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    public final ArrayList<AppInfo> getSearchedInfoList(){
        ArrayList<AppInfo> applicationInfoArrayList = new ArrayList<>();
        int size = Math.min(MAX_SEARCH_ITEM_SIZE, mApplicationInfoList.size());
        for (int i = 0 ; i < size ; i++)
            applicationInfoArrayList.add(mApplicationInfoList.get(i));
        return applicationInfoArrayList;
    }

    public static class ListViewHolder extends RecyclerView.ViewHolder {

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }

}
