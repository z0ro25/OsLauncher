package com.amz.ios.launcher.bounce;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public interface DragDropAdapter<T extends ViewHolder> {
    void onItemMoved(int fromPosition,int toPosition);
    void onItemSwipedToStart(ViewHolder viewHolder, int positionOfItem);
    void onItemSwipedToEnd(ViewHolder viewHolder, int positionOfItem);
    void onItemSelected(@NonNull ViewHolder viewHolder);
    void onItemReleased(ViewHolder viewHolder);
}
