package com.amz.ios.launcher.editpage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.CellLayout;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.Workspace;
import com.amz.ios.launcher.bounce.BouncyRecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Adapter cho màn Edit Pages: mỗi item = 1 page (thumbnail render thật + dấu tích ẩn/hiện).
 * Hỗ trợ kéo-thả đổi thứ tự (BouncyRecyclerView + ItemTouchHelper) và toggle ẩn/hiện.
 *
 * Danh sách {@link PageItem} chứa TẤT CẢ page thật (kể cả page ẩn) theo đúng thứ tự hiện tại
 * trong Workspace. Thay đổi chỉ áp vào Workspace khi bấm Done ({@link #getOrder()} /
 * {@link #getHiddenSet()}).
 */
public class EditPagesAdapter extends BouncyRecyclerView.BouncyAdapter<EditPagesAdapter.PageViewHolder> {

    public static class PageItem {
        public final long screenId;
        public Bitmap thumb;
        public boolean visible;

        PageItem(long screenId, Bitmap thumb, boolean visible) {
            this.screenId = screenId;
            this.thumb = thumb;
            this.visible = visible;
        }
    }

    private final List<PageItem> mItems = new ArrayList<>();
    private final int mThumbWidth;
    private final int mThumbHeight;

    /**
     * @param workspace   Workspace hiện tại (nguồn page + render thumbnail).
     * @param hiddenIds   tập screenId đang ẩn (từ prefs).
     * @param thumbWidth  bề rộng thumbnail (px).
     * @param thumbHeight chiều cao thumbnail (px).
     */
    public EditPagesAdapter(Workspace workspace, Set<Long> hiddenIds, int thumbWidth, int thumbHeight) {
        this.mThumbWidth = thumbWidth;
        this.mThumbHeight = thumbHeight;
        setHasStableIds(true);
        buildItems(workspace, hiddenIds);
    }

    private void buildItems(Workspace workspace, Set<Long> hiddenIds) {
        ArrayList<Long> order = workspace.getScreenOrder();
        for (int i = 0; i < order.size(); i++) {
            long screenId = order.get(i);
            // Bỏ qua các screen đặc biệt (custom content -301, extra empty -202): id âm.
            if (screenId < 0) {
                continue;
            }
            CellLayout cl = workspace.getScreenWithId(screenId);
            Bitmap thumb = (cl != null) ? viewToBitmap(cl, mThumbWidth, mThumbHeight) : null;
            boolean visible = hiddenIds == null || !hiddenIds.contains(screenId);
            mItems.add(new PageItem(screenId, thumb, visible));
        }
    }

    /**
     * Render một CellLayout thành bitmap kích thước cho trước (scale giữ nguyên tỉ lệ vẽ đầy khung).
     * Page ẩn (đã detach khỏi Workspace) vẫn còn width/height cũ nên vẫn vẽ được.
     */
    private static Bitmap viewToBitmap(CellLayout view, int outWidth, int outHeight) {
        int srcW = view.getWidth();
        int srcH = view.getHeight();
        if (srcW <= 0 || srcH <= 0) {
            // Page chưa từng được layout: đo & layout thủ công theo kích thước output.
            view.measure(
                    View.MeasureSpec.makeMeasureSpec(outWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(outHeight, View.MeasureSpec.EXACTLY));
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            srcW = view.getWidth();
            srcH = view.getHeight();
        }
        if (srcW <= 0 || srcH <= 0) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float scale = Math.min((float) outWidth / srcW, (float) outHeight / srcH);
        // Căn giữa nội dung đã scale.
        float dx = (outWidth - srcW * scale) / 2f;
        float dy = (outHeight - srcH * scale) / 2f;
        canvas.translate(dx, dy);
        canvas.scale(scale, scale);
        view.draw(canvas);
        return bitmap;
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).screenId;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_edit_page, parent, false);
        return new PageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        final PageItem item = mItems.get(position);

        ViewGroup.LayoutParams lp = holder.card.getLayoutParams();
        lp.width = mThumbWidth;
        lp.height = mThumbHeight;
        holder.card.setLayoutParams(lp);

        holder.thumb.setImageBitmap(item.thumb);
        applyVisibleState(holder, item.visible);

        holder.check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) {
                    return;
                }
                PageItem it = mItems.get(pos);
                // Không cho ẩn page cuối cùng còn hiển thị (tối thiểu 1 page hiện).
                if (it.visible && countVisible() <= 1) {
                    return;
                }
                it.visible = !it.visible;
                applyVisibleState(holder, it.visible);
            }
        });
    }

    private void applyVisibleState(PageViewHolder holder, boolean visible) {
        holder.check.setImageResource(
                visible ? R.drawable.ic_page_check_on : R.drawable.ic_page_check_off);
        holder.card.setAlpha(visible ? 1f : 0.4f);
    }

    private int countVisible() {
        int n = 0;
        for (PageItem it : mItems) {
            if (it.visible) {
                n++;
            }
        }
        return n;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    /** Thứ tự screenId hiện tại (sau kéo-thả). */
    public ArrayList<Long> getOrder() {
        ArrayList<Long> order = new ArrayList<>();
        for (PageItem it : mItems) {
            order.add(it.screenId);
        }
        return order;
    }

    /** Tập screenId đang bị ẩn (sau các thao tác toggle). */
    public java.util.HashSet<Long> getHiddenSet() {
        java.util.HashSet<Long> hidden = new java.util.HashSet<>();
        for (PageItem it : mItems) {
            if (!it.visible) {
                hidden.add(it.screenId);
            }
        }
        return hidden;
    }

    // ---- Drag & drop ----

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        if (fromPosition < 0 || toPosition < 0
                || fromPosition >= mItems.size() || toPosition >= mItems.size()) {
            return;
        }
        mItems.add(toPosition, mItems.remove(fromPosition));
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemSwipedToStart(RecyclerView.ViewHolder viewHolder, int positionOfItem) {
    }

    @Override
    public void onItemSwipedToEnd(RecyclerView.ViewHolder viewHolder, int positionOfItem) {
    }

    @Override
    public void onItemSelected(@NonNull RecyclerView.ViewHolder viewHolder) {
        viewHolder.itemView.setScaleX(1.06f);
        viewHolder.itemView.setScaleY(1.06f);
    }

    @Override
    public void onItemReleased(RecyclerView.ViewHolder viewHolder) {
        viewHolder.itemView.setScaleX(1f);
        viewHolder.itemView.setScaleY(1f);
    }

    public static class PageViewHolder extends RecyclerView.ViewHolder {
        final CardView card;
        final ImageView thumb;
        final ImageView check;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_thumb);
            thumb = itemView.findViewById(R.id.iv_thumb);
            check = itemView.findViewById(R.id.iv_check);
        }
    }
}
