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
    /** Layout cho 1 item: {@code R.layout.search_item} = ô lưới (BubbleTextView); {@code R.layout.search_item_row} = dòng dọc icon-trái/tên-phải. */
    int mItemLayoutRes = R.layout.search_item;

    /**
     * Giới hạn số app hiển thị ở CHẾ ĐỘ GỢI Ý (khi text rỗng). Mặc định = {@link #MAX_SEARCH_ITEM_SIZE}
     * để KHÔNG đổi hành vi các instance cũ (kết quả/lịch sử). Instance gợi ý set = 4, bấm "Xem thêm"
     * set = 8. KHÔNG sửa hằng số dùng chung MAX_SEARCH_ITEM_SIZE.
     */
    int mSuggestionLimit = MAX_SEARCH_ITEM_SIZE;

    public SearchViewAdapter(Context context, ArrayList<AppInfo> apps){
        this(context, apps, R.layout.search_item);
    }

    /** Thêm itemLayoutRes để dùng chung adapter cho cả lưới lẫn dòng dọc. */
    public SearchViewAdapter(Context context, ArrayList<AppInfo> apps, int itemLayoutRes){
        mLauncher = (Launcher) context;
        mItemLayoutRes = itemLayoutRes;
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
        View view = inflater.inflate(mItemLayoutRes,parent,false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < 0 || position >= mSearchedInfoList.size()) return;
        AppInfo appInfo = mSearchedInfoList.get(position);
        if (!(holder instanceof ListViewHolder)) return;
        View itemView = holder.itemView;
        if (mItemLayoutRes == R.layout.search_item_row) {
            // Dòng DỌC: icon trái (FullBubbleTextView) + tên phải (TextView).
            BubbleTextView icon = itemView.findViewById(R.id.icon);
            android.widget.TextView name = itemView.findViewById(R.id.text);
            // BubbleTextView.reapplyItemInfo chỉ áp khi getTag()==info -> phải setTag trước.
            if (icon != null) {
                icon.setTag(appInfo);
                icon.reapplyItemInfo(appInfo);
            }
            if (name != null && appInfo.title != null) name.setText(appInfo.title);
            itemView.setTag(appInfo);
            itemView.setOnClickListener(openAppClickListener);
            return;
        }
        // Ô LƯỚI (BubbleTextView icon trên + tên dưới) — hành vi cũ.
        BubbleTextView bubbleTextView = (BubbleTextView) itemView;
        bubbleTextView.setTag(appInfo);
        bubbleTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        bubbleTextView.reapplyItemInfo(appInfo);
        bubbleTextView.setOnClickListener(openAppClickListener);
    }

    /**
     * Bọc listener: GHI lịch sử app mở-từ-search TRƯỚC, rồi vẫn gọi luồng mở app dùng chung của Launcher.
     * Bất biến: phải gọi mLauncher.onClick(v) để giữ nguyên hành vi mở app cũ (chỉ thêm ghi lịch sử, KHÔNG
     * thay thế). Áp cho mọi instance.
     */
    private final View.OnClickListener openAppClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Object tag = v.getTag();
            if (tag instanceof AppInfo && ((AppInfo) tag).componentName != null) {
//                SearchHistoryStore.push(mLauncher, ((AppInfo) tag).componentName);
            }
            mLauncher.onClick(v);
        }
    };

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
        // Dùng mSuggestionLimit (mặc định = MAX_SEARCH_ITEM_SIZE) thay hằng số để nút "Xem thêm"
        // đổi 4<->8 chỉ trên instance gợi ý, không ảnh hưởng instance khác.
        int size = Math.min(mSuggestionLimit, mApplicationInfoList.size());
        for (int i = 0 ; i < size ; i++)
            applicationInfoArrayList.add(mApplicationInfoList.get(i));
        return applicationInfoArrayList;
    }

    /**
     * Đổi giới hạn số app ở chế độ gợi ý (4 hoặc 8) rồi nạp lại danh sách hiển thị. Chỉ có ý nghĩa
     * khi text rỗng (chế độ gợi ý); khi đang gõ, kết quả filter không bị cắt theo limit.
     */
    public void setSuggestionLimit(int limit) {
        mSuggestionLimit = limit;
        mSearchedInfoList.clear();
        mSearchedInfoList.addAll(getSearchedInfoList());
        notifyDataSetChanged();
    }

    public static class ListViewHolder extends RecyclerView.ViewHolder {

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }

}
