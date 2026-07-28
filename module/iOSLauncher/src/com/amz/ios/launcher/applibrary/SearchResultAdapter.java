package com.amz.ios.launcher.applibrary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.BubbleTextView;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;

import java.util.ArrayList;

public class SearchResultAdapter extends RecyclerView.Adapter implements Filterable {

    ArrayList<SearchResult> mAllResults;
    ArrayList<SearchResult> mSearchedResult;
    Filter mFilter;

    public SearchResultAdapter(ArrayList<SearchResult> results){
        this.mAllResults = new ArrayList<>();
        mSearchedResult = new ArrayList<>();
        mAllResults.addAll(results);
        mSearchedResult.addAll(results);
        mFilter = new SearchFilterable();
    }

    class SearchFilterable extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<SearchResult> arrayList = new ArrayList<>();
            if (charSequence != null && charSequence.length() != 0) {
                String trim = charSequence.toString().toLowerCase().trim();
                for (SearchResult next : mAllResults) {
                    if (next != null && next.getType() == 1 && next.getName() != null && next.getName().toLowerCase().contains(trim)) {
                        arrayList.add(next);
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = arrayList;
                return filterResults;
            }
            arrayList.addAll(mAllResults);
            FilterResults results = new FilterResults();
            results.values = arrayList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mSearchedResult.clear();
            mSearchedResult.addAll((ArrayList) results.values);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        int resId = viewType == 0 ? R.layout.item_header_apps_library_search_view : R.layout.item_apps_library_search_view;
        View view = inflater.inflate(resId,viewGroup,false);
        return viewType == 0 ? new HeaderAppsViewHolder(view) : new NormalAppsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderAppsViewHolder) {
            HeaderAppsViewHolder viewHolder = (HeaderAppsViewHolder) holder;
            ((TextViewCustomFont) viewHolder.itemView).setText(
                    mSearchedResult.get(position).getName()
            );
        }
        else if (holder instanceof NormalAppsViewHolder){
            final NormalAppsViewHolder viewHolder = (NormalAppsViewHolder) holder;
            SearchResult result = mSearchedResult.get(position);
            viewHolder.mLabelTV.setText(
                result.getName()
            );
            viewHolder.mIconIV.setTag(result.getAppInfo());
            viewHolder.mIconIV.reapplyItemInfo(result.getAppInfo());
            viewHolder.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            viewHolder.mIconIV.click();
                        }
                    }
            );
        }
    }

    public final boolean isNormalItem(int i) {
        return this.mSearchedResult.get(i).getType() == 0;
    }

    @Override
    public int getItemCount() {
        if (mSearchedResult == null) return 0;
        return mSearchedResult.size();
    }

    @Override
    public Filter getFilter() {
        return this.mFilter;
    }

    @Override
    public int getItemViewType(int position) {
        return mSearchedResult.get(position).getType();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        recyclerView.addItemDecoration(
                new SearchResultDecoration(this)
        );
    }

    public class HeaderAppsViewHolder extends RecyclerView.ViewHolder {

        public HeaderAppsViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class NormalAppsViewHolder extends RecyclerView.ViewHolder {

        TextViewCustomFont mLabelTV;
        BubbleTextView mIconIV;

        public NormalAppsViewHolder(@NonNull View itemView) {
            super(itemView);
            this.mLabelTV = itemView.findViewById(R.id.text);
            this.mIconIV = itemView.findViewById(R.id.icon);
            this.mIconIV.setTextVisibility(false);
        }
    }
}
