package com.truongnt.ios.launcher.bounce;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public interface DragDropAdapter<T extends ViewHolder> {
    void onItemMoved(int fromPosition,int toPosition);
    void onItemSwipedToStart(ViewHolder viewHolder, int positionOfItem);
    void onItemSwipedToEnd(ViewHolder viewHolder, int positionOfItem);
    void onItemSelected(@NonNull ViewHolder viewHolder);
    void onItemReleased(ViewHolder viewHolder);

    /**
     * Item ở {@code position} có được kéo-thả không.
     *
     * <p>Mặc định {@code true} — mọi adapter đang chạy giữ nguyên hành vi cũ. Adapter nào cần
     * ghim một item cố định (ví dụ item quảng cáo trên màn trái) thì override trả {@code false};
     * {@link DragDropCallBack#getMovementFlags} sẽ trả 0 cờ kéo cho item đó nên ItemTouchHelper
     * không nhấc nó lên được.
     */
    default boolean canDragItem(int position) {
        return true;
    }
}
