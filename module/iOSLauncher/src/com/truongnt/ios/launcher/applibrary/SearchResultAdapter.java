package com.truongnt.ios.launcher.applibrary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.truongnt.ios.launcher.BubbleTextView;
import com.truongnt.ios.launcher.R;
import com.truongnt.ios.launcher.TextViewCustomFont;

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
            // Kết quả đổi -> thanh chữ cái phải rút gọn/mở rộng theo, nếu không sẽ còn chữ bấm
            // vào chẳng nhảy đi đâu (nhóm đó đã bị lọc mất).
            if (mOnResultsChangedListener != null) {
                mOnResultsChangedListener.onResultsChanged();
            }
        }
    }

    /** Báo cho màn ngoài biết danh sách kết quả vừa đổi (để cập nhật thanh chữ cái). */
    public interface OnResultsChangedListener {
        void onResultsChanged();
    }

    private OnResultsChangedListener mOnResultsChangedListener;

    public void setOnResultsChangedListener(OnResultsChangedListener l) {
        mOnResultsChangedListener = l;
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
            // ĐỒNG BỘ CỠ ICON: ép mọi icon về đúng cỡ icon desktop (originalIconSizePx, scale 1f).
            //
            // reapplyItemInfo đặt bounds theo drawable của TỪNG app nên cỡ hiển thị chênh nhau —
            // đây là lý do danh sách trông so le. scaleIconSize() đặt lại bounds về MỘT cỡ duy
            // nhất cho mọi hàng, và vì view dùng wrap_content nên khung tự ôm trọn icon ở cỡ đó
            // (không cắt xén). KHÔNG ép khung cố định nhỏ hơn icon — sẽ cắt mất logo.
            viewHolder.mIconIV.scaleIconSize(1f);
            // reapplyItemInfo chặn relayout (mDisableRelayout) -> view tái sử dụng có thể giữ kích
            // thước đo cũ khiến icon lệch/không đều "thi thoảng". Ép đo lại để mọi item đồng cỡ.
            viewHolder.mIconIV.requestLayout();
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

    /**
     * Danh sách chữ cái nhóm ĐANG CÓ trong kết quả hiện tại (item type 0 = header chữ cái).
     * Dùng cho thanh A-Z: chỉ hiện chữ có thật, để bấm chữ nào cũng nhảy được tới nơi.
     */
    public ArrayList<String> getSectionLetters() {
        ArrayList<String> letters = new ArrayList<>();
        for (SearchResult r : mSearchedResult) {
            if (r != null && r.getType() == 0 && r.getName() != null) {
                String name = r.getName().trim();
                if (!name.isEmpty() && !letters.contains(name)) {
                    letters.add(name);
                }
            }
        }
        return letters;
    }

    /** Vị trí item header của chữ cái này trong list; -1 nếu không có. */
    public int findPositionForLetter(String letter) {
        if (letter == null) {
            return -1;
        }
        for (int i = 0; i < mSearchedResult.size(); i++) {
            SearchResult r = mSearchedResult.get(i);
            if (r != null && r.getType() == 0 && letter.equalsIgnoreCase(
                    r.getName() != null ? r.getName().trim() : null)) {
                return i;
            }
        }
        return -1;
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

    // Bỏ addItemDecoration(SearchResultDecoration): không dùng sticky header ghim đỉnh nữa.
    // Chữ cái đầu nhóm đã là item thường (type 0) trong list nên tự cuộn theo khi vuốt.

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
