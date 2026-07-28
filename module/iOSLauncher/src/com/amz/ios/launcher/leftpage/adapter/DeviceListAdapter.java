package com.amz.ios.launcher.leftpage.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;
import com.amz.ios.launcher.leftpage.model.Battery;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter {

    List<Battery> mDevices;
    public LayoutInflater mLayoutInflater;

    public DeviceListAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.item_battery,parent,false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position){
        if (mDevices == null) return;
        if (position < 0 || position >= mDevices.size()) return;
        Battery battery = mDevices.get(position);
        if (holder instanceof ListViewHolder) {
            ListViewHolder listViewHolder = (ListViewHolder) holder;
            listViewHolder.mBatteryPercent.setText(
                battery.b
            );
            listViewHolder.mDeviceName.setText(
                    battery.c
            );
            listViewHolder.mBatteryPercent.setTextColor(
                    Color.BLACK
            );
            listViewHolder.mDeviceName.setTextColor(
                    Color.BLACK
            );
        }
    }

    @Override
    public int getItemCount() {
        if (mDevices == null) return 0;
        return mDevices.size();
    }

    public void setDevices(List<Battery> batteries){
        this.mDevices = batteries;
        notifyDataSetChanged();
    }

    public static class ListViewHolder extends RecyclerView.ViewHolder {

        TextViewCustomFont mDeviceName;
        TextViewCustomFont mBatteryPercent;

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            setUpView();
        }

        void setUpView(){
            mDeviceName = itemView.findViewById(R.id.battery_device_name);
            mBatteryPercent = itemView.findViewById(R.id.battery_percent);
        }
    }

}
