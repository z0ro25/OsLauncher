package com.amz.ios.launcher.bounce;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class DragDropCallBack extends ItemTouchHelper.Callback {

    private RecyclerView.Adapter adapter;
    private boolean longPressDragEnabled;
    private boolean itemSwipeEnabled;

    public DragDropCallBack(RecyclerView.Adapter adapter, boolean longPressDragEnabled, boolean itemSwipeEnabled) {
        this.adapter = adapter;
        this.longPressDragEnabled = longPressDragEnabled;
        this.itemSwipeEnabled = itemSwipeEnabled;
    }

    public boolean isLongPressDragEnabled(){
        return longPressDragEnabled;
    }

    public void setDragEnabled(Boolean enabled)
    {
        longPressDragEnabled = enabled;
    }

    public boolean isItemViewSwipeEnabled(){
        return itemSwipeEnabled;
    }

    public void setSwipeEnabled(boolean enabled)
    {
        itemSwipeEnabled = enabled;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        if (isLongPressDragEnabled() && adapter instanceof DragDropAdapter){
            ((DragDropAdapter) adapter).onItemMoved(viewHolder.getAdapterPosition(),target.getAdapterPosition());
            return true;
        }
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        if (isItemViewSwipeEnabled() && adapter instanceof DragDropAdapter)
        {
            if (direction == ItemTouchHelper.START)
                ((DragDropAdapter)adapter).onItemSwipedToStart(viewHolder, viewHolder.getAdapterPosition());
            else if (direction == ItemTouchHelper.END)
                ((DragDropAdapter)adapter).onItemSwipedToEnd(viewHolder, viewHolder.getAdapterPosition());
        }
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE && adapter instanceof DragDropAdapter)
        {
            ((DragDropAdapter)adapter).onItemSelected(viewHolder);
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (adapter instanceof DragDropAdapter)
            ((DragDropAdapter)adapter).onItemReleased(viewHolder);
    }
}
