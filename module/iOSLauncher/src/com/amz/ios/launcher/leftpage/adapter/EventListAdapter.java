package com.amz.ios.launcher.leftpage.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.Launcher;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.leftpage.model.CalendarEventInfo;
import java.util.List;

public class EventListAdapter extends RecyclerView.Adapter {

    Launcher mLauncher;
    List<CalendarEventInfo> mEvents;
    LayoutInflater mLayoutInflater;
    int mMargin;

    public EventListAdapter(Context context) {
        super();
        mLauncher = (Launcher) context;
        mLayoutInflater = LayoutInflater.from(context);
        mMargin = mLauncher.getDeviceProfile().edgeMarginPx;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(
                R.layout.event_item,
                parent,
                false
        );
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (mEvents == null) return;
        if (position < 0 || position >= mEvents.size()) return;
        if (holder instanceof ItemViewHolder) {
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            CalendarEventInfo eventInfo = mEvents.get(position);
            itemViewHolder.mEventColor.setBackgroundColor(eventInfo.mEventColor);
            itemViewHolder.mEventName.setText(eventInfo.mEventName);
            itemViewHolder.mEventTime.setText(eventInfo.mEventTime);
            itemViewHolder.mEventName.setTextColor(Color.BLACK);
            itemViewHolder.mEventTime.setTextColor(Color.BLACK);
        }
    }

    @Override
    public int getItemCount() {
        if (mEvents == null) return 0;
        return mEvents.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {

        View mEventColor;
        TextView mEventName;
        TextView mEventTime;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mEventColor = itemView.findViewById(R.id.event_color);
            mEventName = itemView.findViewById(R.id.event_name);
            mEventTime = itemView.findViewById(R.id.event_time);
            itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mEvents == null) return;
                            try{
                                int i = mEvents.get(getAdapterPosition()).mEventId;
                                Intent intent = new Intent("android.intent.action.VIEW");
                                Uri.Builder buildUpon = CalendarContract.Events.CONTENT_URI.buildUpon();
                                buildUpon.appendPath(Long.toString(i));
                                intent.setData(buildUpon.build());
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                mLauncher.startActivity(intent);
                            }
                            catch (Throwable thr){
                                Toast.makeText(mLauncher, mLauncher.getString(R.string.application_not_found), 1).show();
                            }
                        }
                    }
            );
        }
    }

    public void setEvents(List<CalendarEventInfo> events){
        mEvents = events;
        notifyDataSetChanged();
    }
}
