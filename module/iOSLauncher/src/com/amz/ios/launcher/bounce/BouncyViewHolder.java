package com.amz.ios.launcher.bounce;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class BouncyViewHolder extends RecyclerView.ViewHolder {
    public BouncyViewHolder(@NonNull View itemView) {
        super(itemView);
    }
    abstract void onPulled(float delta);
    abstract void onRelease();
    abstract void onAbsorb(int velocity);
}
