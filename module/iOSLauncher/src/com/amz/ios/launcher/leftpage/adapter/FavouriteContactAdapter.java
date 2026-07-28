package com.amz.ios.launcher.leftpage.adapter;

import android.content.Context;
import android.graphics.drawable.shapes.OvalShape;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amz.ios.launcher.CircleImageView;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.TextViewCustomFont;
import com.amz.ios.launcher.leftpage.drawables.FavouriteContactDrawable;
import com.amz.ios.launcher.leftpage.model.FavouriteContactInfo;

import java.util.List;

public class FavouriteContactAdapter extends RecyclerView.Adapter {

    LayoutInflater mLayoutInflater;
    List<FavouriteContactInfo> mContacts;

    public FavouriteContactAdapter(Context context) {
        super();
        mLayoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.recycler_starred_contact_item,parent,false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (mContacts == null) return;
        if (position < 0 || position >= mContacts.size()) return;
        FavouriteContactInfo info = mContacts.get(position);
        StringBuilder b;
        String str;
        if (holder instanceof ItemViewHolder) {
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            itemViewHolder.mContactNameTV.setText(info.b);
            itemViewHolder.mContactNameTV.setTextColor(-16777216);
            if (info.mBmp != null) {
                itemViewHolder.mContactAvatarIV.setImageBitmap(info.mBmp);
            } else if (info.b != null) {
                FavouriteContactDrawable.DrawableData data = new FavouriteContactDrawable.DrawableData();
                String[] split = info.b.split(" ");
                String str2 = "";
                if (split.length > 1) {
                    if (!TextUtils.isEmpty(split[0])) {
                        str2 = split[0].substring(0, 1);
                    }
                    if (!TextUtils.isEmpty(split[1])) {
                        b = new StringBuilder();
                        b.append(str2);
                        str = split[1];
                        b.append(str.substring(0, 1));
                        str2 = b.toString();
                    }
                } else {
                    if (!TextUtils.isEmpty(split[0])) {
                        b = new StringBuilder();
                        str = split[0];
                        b.append(str.substring(0, 1));
                        str2 = b.toString();
                    }
                }
                data.mShape = new OvalShape();
                data.mTextColor = -7829368;
                data.mText = str2;
                itemViewHolder.mContactAvatarIV.setImageDrawable(new FavouriteContactDrawable(data));
            } else {
                itemViewHolder.mContactAvatarIV.setImageResource(R.drawable.ic_contact);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mContacts == null) return 0;
        return mContacts.size();
    }

    public void setContacts(List<FavouriteContactInfo> contacts){
        mContacts = contacts;
        notifyDataSetChanged();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        CircleImageView mContactAvatarIV;
        TextViewCustomFont mContactNameTV;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mContactAvatarIV = itemView.findViewById(R.id.contact_avatar);
            mContactNameTV = itemView.findViewById(R.id.contact_name);
        }
    }
}
