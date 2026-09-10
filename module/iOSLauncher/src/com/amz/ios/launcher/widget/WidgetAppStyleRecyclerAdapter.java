package com.amz.ios.launcher.widget;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * Adapter cho carousel chọn cỡ widget (sheet level-2) sau khi BỎ ViewPager.
 *
 * VÌ SAO BỎ ViewPager: ViewPager chỉ dựng trang (populate) từ bên trong onMeasure. Ở màn này
 * setAdapter chạy khi sheet còn chưa hiện, và trên Android 9 không có vòng traversal nào chạm
 * được pager sau đó -> pager không bao giờ đo -> không có trang -> ô preview TRỐNG.
 * RecyclerView bind view theo layout/scroll bình thường, không phụ thuộc mẹo đo như vậy.
 *
 * Mỗi item là MỘT {@link WidgetAppStyleCell} đã dựng sẵn từ trước (giữ nguyên cách cũ để không
 * đụng luồng tạo cell trong SlidingUpWidgetsCellAppStyle.setData). Cell được bọc trong một
 * FrameLayout rộng bằng RecyclerView để mỗi item chiếm trọn 1 "trang" và cell nằm CHÍNH GIỮA.
 */
public class WidgetAppStyleRecyclerAdapter
        extends RecyclerView.Adapter<WidgetAppStyleRecyclerAdapter.CellHolder> {

    private final ArrayList<WidgetAppStyleCell> mCells;
    /** Bề rộng mỗi trang = bề rộng RecyclerView, đặt từ ngoài khi đã biết kích thước thật. */
    private int mPageWidth;
    /** Chiều cao khung carousel, dùng để dựng preview đúng cỡ ngay khi bind (không chờ đo). */
    private int mPageHeight;

    public WidgetAppStyleRecyclerAdapter(ArrayList<WidgetAppStyleCell> cells) {
        mCells = cells != null ? cells : new ArrayList<WidgetAppStyleCell>();
        setHasStableIds(true);
    }

    /** Cập nhật bề rộng trang; gọi lại khi RecyclerView có kích thước thật. */
    public void setPageWidth(int pageWidth) {
        if (pageWidth > 0 && pageWidth != mPageWidth) {
            mPageWidth = pageWidth;
            notifyDataSetChanged();
        }
    }

    /** Cập nhật chiều cao khung carousel (để dựng preview đúng cỡ ngay khi bind). */
    public void setPageHeight(int pageHeight) {
        if (pageHeight > 0) {
            mPageHeight = pageHeight;
        }
    }

    public WidgetAppStyleCell getCell(int position) {
        if (position < 0 || position >= mCells.size()) return null;
        return mCells.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public CellHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Khung trang: rộng bằng RecyclerView, cao khớp cha, cell căn giữa cả 2 chiều.
        FrameLayout page = new FrameLayout(parent.getContext());
        page.setClipChildren(false);
        page.setClipToPadding(false);
        int w = mPageWidth > 0 ? mPageWidth : parent.getWidth();
        page.setLayoutParams(new RecyclerView.LayoutParams(
                w > 0 ? w : ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return new CellHolder(page);
    }

    @Override
    public void onBindViewHolder(@NonNull CellHolder holder, int position) {
        FrameLayout page = (FrameLayout) holder.itemView;

        // Trang luôn rộng đúng bằng RecyclerView để snap đứng giữa. Ép lại mỗi lần bind vì holder
        // có thể được tái dùng từ lần mở sheet trước (kích thước chốt lúc onCreateViewHolder có
        // thể đã cũ).
        ViewGroup.LayoutParams pageLp = page.getLayoutParams();
        int w = mPageWidth > 0 ? mPageWidth : pageLp.width;
        if (w > 0 && pageLp.width != w) {
            pageLp.width = w;
            page.setLayoutParams(pageLp);
        }

        WidgetAppStyleCell cell = mCells.get(position);
        // Cell có thể còn dính trang cũ (holder tái dùng) -> gỡ khỏi cha trước khi gắn lại.
        if (cell.getParent() instanceof ViewGroup) {
            ((ViewGroup) cell.getParent()).removeView(cell);
        }
        page.removeAllViews();

        // Cell LẤP ĐẦY trang và căn giữa. Khung chứa preview sống (mLiveHost) dùng MATCH_PARENT nên
        // bám theo kích thước cell -> cell khớp trang thì LiveWidgetPreviewHelper mới tính scale
        // theo đúng chiều cao khung thật (nếu để cell giữ kích thước vuông cố định mSize ~949px
        // trong khi trang thấp hơn nhiều thì widget cỡ lớn bị tràn ra ngoài).
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = android.view.Gravity.CENTER;
        page.addView(cell, lp);

        // Cell là LinearLayout DỌC, trong đó ảnh tĩnh (mWidgetPreview) khai báo height=match_parent.
        // Nếu ảnh tĩnh còn hiển thị khi dùng preview sống, nó chiếm TRỌN chiều cao cell và đẩy
        // khung preview sống xuống với chiều cao 0 -> ô trắng trơn.
        //
        // Phải xử lý CẢ HAI chiều: holder được tái dùng nên cell của widget APP NGOÀI có thể còn
        // dính trạng thái "ảnh tĩnh đang ẩn" từ item widget iOS trước đó -> ô trống dù ảnh đã nạp.
        boolean live = LiveWidgetPreviewHelper.isLivePreviewSupported(cell.mParcelable);
        if (cell.mWidgetPreview != null && cell.mLiveHost != null) {
            cell.mWidgetPreview.setVisibility(live ? View.GONE : View.VISIBLE);
            cell.mLiveHost.setVisibility(live ? View.VISIBLE : View.GONE);
        }

        cell.setVisibility(View.VISIBLE);

        // Dựng preview NGAY bằng kích thước khung ĐÃ BIẾT (bề rộng trang x chiều cao carousel),
        // không chờ cell được đo. Trước đây phải đợi vòng layout nên lần mở đầu ô trống, vuốt qua
        // lại mới hiện. Kích thước do sheet tính sẵn và truyền vào (xem computePreviewBoxHeight).
        int boxW = mPageWidth > 0 ? mPageWidth : page.getWidth();
        int boxH = mPageHeight;
        if (boxW > 0 && boxH > 0) {
            cell.buildLivePreviewNow(boxW, boxH);
        } else {
            cell.ensurePreview();
        }
    }

    @Override
    public int getItemCount() {
        return mCells.size();
    }

    static class CellHolder extends RecyclerView.ViewHolder {
        CellHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
