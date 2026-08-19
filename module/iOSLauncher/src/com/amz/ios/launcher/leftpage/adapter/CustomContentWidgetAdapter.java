package com.amz.ios.launcher.leftpage.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.amz.ios.launcher.LauncherAnimUtils;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.bounce.BouncyRecyclerView;
import com.amz.ios.launcher.leftpage.views.CustomContentView;
import com.amz.ios.launcher.leftpage.custom.CustomZoomImageView;
import com.amz.ios.launcher.leftpage.database.WidgetInfo;

import java.util.ArrayList;

public class CustomContentWidgetAdapter extends BouncyRecyclerView.BouncyAdapter<RecyclerView.ViewHolder> {

    // Trạng thái edit khi kéo-thả: phóng to nhẹ + nâng bóng cho item đang kéo, giống
    // cảm giác "nhấc" icon lên ở màn app.
    private static final float DRAG_SCALE = 1.08f;
    private static final float DRAG_ELEVATION = 24.0f;

    private final CustomContentView mCustomContentView;
    private final ArrayList<WidgetInfo> mWidgetInfoArrayList;

    public CustomContentWidgetAdapter(CustomContentView customContentView, ArrayList<WidgetInfo> arrayList) {
        this.mCustomContentView = customContentView;
        this.mWidgetInfoArrayList = arrayList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (i == 100){
            return new WidgetItemViewHolder(
                    inflater.inflate(R.layout.edit_widget_button,parent,false)
            );
        }

        int resId = -1;

        if (i == 20) {
            resId = R.layout.widget_clock_2x2;
        }
        else if (i == 21) {
            resId = R.layout.widget_clock_2x4;
        }
        else if (i == 30) {
            resId = R.layout.widget_photo_2x2;
        }
        else if (i == 40) {
            resId = R.layout.widget_calendar_2x2;
        }
        else if (i == 42) {
            // Lịch lưới tháng (đủ ngày) 2x2 — mặc định mới, đồng bộ thẻ nổi bật ở khay.
            resId = R.layout.widget_calendar_month_2x2;
        }
        else if (i == 41) {
            resId = R.layout.widget_calendar_2x4;
        }
        else if (i == 61) {
            resId = R.layout.widget_battery_2x2;
        }
        else if (i == 60) {
            resId = R.layout.widget_battery_2x4;
        }
        else if (i == 70 || i == 71) {
            resId = R.layout.widget_suggestion_2x4;
        }
        else if (i == 51){
            resId = R.layout.widget_layout_favourite_contact_2x4;
        }
        else {
            resId = R.layout.widget_clock_2x4;
//            return new LauncherWidgetListViewHolder(null);
        }
        return new LauncherWidgetListViewHolder(
                inflater.inflate(resId,parent,false)
        );

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof WidgetItemViewHolder) {
            WidgetItemViewHolder viewHolder = (WidgetItemViewHolder) holder;
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    mCustomContentView.startShaking();
                    mCustomContentView.enableButtons();
                }
            });
            ((StaggeredGridLayoutManager.LayoutParams) ((viewHolder).itemView.getLayoutParams())).setFullSpan(true);
        }
        else if (holder instanceof LauncherWidgetListViewHolder) {
            LauncherWidgetListViewHolder viewHolder = (LauncherWidgetListViewHolder) holder;
            CustomZoomImageView customZoomImageView = viewHolder.mDeleteBtn;
            final int pos = position;
            if (customZoomImageView != null) {
                customZoomImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        if (pos >= mWidgetInfoArrayList.size())
                            return;
                        if (mCustomContentView.mListWidgetRV == null || mCustomContentView.mWidgetListAdapter == null)
                            return;
                        final WidgetInfo info = mWidgetInfoArrayList.get(pos);
                        mWidgetInfoArrayList.remove(pos);
                        notifyItemRemoved(pos);
                        notifyItemRangeChanged(pos, mWidgetInfoArrayList.size(),null);
                        new Thread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        mCustomContentView.mWidgetInfoList.remove(info);
                                        info.delete();
                                    }
                                }
                        ).start();
                    }
                });
            }

            StaggeredGridLayoutManager.LayoutParams lp =
                    (StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
            lp.setFullSpan(isFullSpanType(this.mWidgetInfoArrayList.get(pos).type));

            CustomContentView customContentView = this.mCustomContentView;
            if (customContentView != null) {
                if (customContentView.t) {
                    viewHolder.startShaking();
                } else {
                    viewHolder.hide();
                }
            }
            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public final boolean onLongClick(View view) {
                    if (CustomContentWidgetAdapter.this.mCustomContentView == null) return true;
                    CustomContentWidgetAdapter.this.mCustomContentView.enableButtons();
                    CustomContentWidgetAdapter.this.mCustomContentView.startShaking();
                    return true;
                }
            });
        }
    }

    // Widget nào chiếm nguyên hàng (layout 2x4) thì full span; widget 2x2 thì nửa hàng.
    // Ánh xạ khớp với resId trong onCreateViewHolder (tránh dựa vào type % 10 vốn sai với
    // battery: 60=2x4 nhưng 60%10==0, 61=2x2 nhưng 61%10==1 -> bị đảo kích cỡ).
    private static boolean isFullSpanType(int type) {
        switch (type) {
            case 21:  // clock 2x4
            case 41:  // calendar 2x4
            case 60:  // battery 2x4
            case 51:  // favourite contact 2x4
            case 70:  // app suggestion
            case 71:  // app suggestion
                return true;
            default:  // 20/30/40/61 -> 2x2 (nửa hàng)
                return false;
        }
    }

    @Override
    public int getItemCount() {
        // Bỏ nút "Chỉnh sửa" (item type 100 ở cuối). Vào chế độ edit bằng cách nhấn giữ
        // widget (long-press) -> rung + kéo thả sắp xếp, giống màn app. Nên chỉ đếm số widget.
        ArrayList<WidgetInfo> arrayList = this.mWidgetInfoArrayList;
        if (arrayList != null) {
            return arrayList.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (this.mWidgetInfoArrayList != null) {
            return this.mWidgetInfoArrayList.get(position).type;
        }
        return 0;
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        if (this.mWidgetInfoArrayList.size() > fromPosition) {
            this.mWidgetInfoArrayList.add(toPosition, this.mWidgetInfoArrayList.remove(fromPosition));
            notifyItemMoved(fromPosition, toPosition);
            final CustomContentView customContentView = this.mCustomContentView;


            new Thread(new Runnable() {
                @Override
                public void run() {
                    customContentView.getClass();
                    for (int i = 0; i < customContentView.mWidgetInfoList.size(); i++) {
                        try {
                            WidgetInfo info = customContentView.mWidgetInfoList.get(i);
                            int indexOf = customContentView.mWidgetInfoList.indexOf(info);
                            info.order = indexOf;
                            // TODO: 2023.11.17 DataBase save
                            info.save();
                        } catch (Throwable th) {
                            th.getMessage();
                            return;
                        }
                    }
                }
            }).start();

        }
    }

    @Override
    public void onItemSwipedToStart(RecyclerView.ViewHolder viewHolder, int positionOfItem) {

    }

    @Override
    public void onItemSwipedToEnd(RecyclerView.ViewHolder viewHolder, int positionOfItem) {

    }

    @Override
    public void onItemSelected(@NonNull RecyclerView.ViewHolder viewHolder) {
        // Bắt đầu kéo: phóng to nhẹ + nâng bóng đổ giống khi kéo icon ở màn app
        // (item được "nhấc lên" khỏi lưới). Tắt rung của chính item đang kéo cho gọn.
        if (viewHolder == null) return;
        if (viewHolder instanceof LauncherWidgetListViewHolder) {
            ((LauncherWidgetListViewHolder) viewHolder).stopShaking();
        }
        View v = viewHolder.itemView;
        v.animate()
                .scaleX(DRAG_SCALE).scaleY(DRAG_SCALE)
                .setDuration(150L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        v.setElevation(DRAG_ELEVATION);
    }

    @Override
    public void onItemReleased(RecyclerView.ViewHolder viewHolder) {
        // Thả ra: item trở về kích cỡ thường + hạ bóng, và rung lại nếu vẫn đang ở
        // chế độ edit (giống các item khác) để người dùng biết vẫn có thể tiếp tục sắp xếp.
        if (viewHolder == null) return;
        View v = viewHolder.itemView;
        v.animate()
                .scaleX(1.0f).scaleY(1.0f)
                .setDuration(150L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        v.setElevation(0.0f);
        if (mCustomContentView != null && mCustomContentView.t
                && viewHolder instanceof LauncherWidgetListViewHolder) {
            ((LauncherWidgetListViewHolder) viewHolder).startShaking();
        }
    }

    public class WidgetItemViewHolder extends RecyclerView.ViewHolder{

        public WidgetItemViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class LauncherWidgetListViewHolder extends RecyclerView.ViewHolder {

        public CustomZoomImageView mDeleteBtn;
        public ObjectAnimator mObjectAnimator;

        public LauncherWidgetListViewHolder(View view) {
            super(view);
            float width = view.getWidth();
            float height = view.getHeight();
            this.mDeleteBtn = (CustomZoomImageView) view.findViewById(R.id.icon_delete_widget);
            this.mObjectAnimator = LauncherAnimUtils.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat("pivotX", ((((float) Math.random()) * 0.38f) + 0.2f) * width, ((((float) Math.random()) * 0.38f) + 0.2f) * width), PropertyValuesHolder.ofFloat("pivotY", ((((float) Math.random()) * 0.38f) + 0.2f) * height, ((((float) Math.random()) * 0.38f) + 0.2f) * height), PropertyValuesHolder.ofFloat("rotation", ((float) (Math.random() * 0.10000000149011612d)) - 0.3926991f, ((float) (Math.random() * 0.10000000149011612d)) + 0.3926991f));
            mObjectAnimator.setDuration((long) ((Math.random() * 36.0d) + 113.0d));
            mObjectAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mObjectAnimator.setRepeatMode(ValueAnimator.REVERSE);
            mObjectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            mObjectAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animation.removeAllListeners();
                }
            });
        }

        public final void hide() {
            if (this.mDeleteBtn != null) {
                this.mDeleteBtn.setVisibility(View.INVISIBLE);
            }
            if (this.mObjectAnimator != null) {
                this.mObjectAnimator.cancel();
                this.itemView.setRotation(0.0f);
            }
        }

        public final void startShaking() {
            if (this.mDeleteBtn != null) {
                this.mDeleteBtn.setVisibility(View.VISIBLE);
            }
        if (this.mObjectAnimator != null) {
                this.mObjectAnimator.start();
            }
        }

        public final void stopShaking(){
            CustomZoomImageView customZoomImageView = this.mDeleteBtn;
            if (customZoomImageView != null) {
                customZoomImageView.setVisibility(View.INVISIBLE);
            }
            ObjectAnimator objectAnimator = this.mObjectAnimator;
            if (objectAnimator != null) {
                objectAnimator.cancel();
                this.itemView.setRotation(0.0f);
            }
        }

    }
}
