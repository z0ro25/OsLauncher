package com.amz.ios.launcher.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.amz.ios.launcher.R;

import java.util.Arrays;
import java.util.List;


public class SpinnerPopupWindow {
    PopupWindow mPopupWindow;
    View mSpinnerView;
    ListView mListView;
    SpinnerAdapter mAdapter;
    Context mContext;

    int selectColor;
    int normalColor;

    public SpinnerPopupWindow(Context context, int entriesResId, int position, AdapterView.OnItemClickListener onItemClickListener) {
        mContext = context;
        mSpinnerView = LayoutInflater.from(context).inflate(R.layout.pref_spinner, null);
        mListView = (ListView) mSpinnerView.findViewById(R.id.listview);
        mAdapter = new SpinnerAdapter(context, entriesResId);
        mAdapter.selectedPosition = position;
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(onItemClickListener);

        selectColor = context.getResources().getColor(com.amz.ios.ioslite.common.R.color.pref_active);
        normalColor = context.getResources().getColor(com.amz.ios.ioslite.common.R.color.pref_dark);
    }

    public void setSelectedPosition(int position) {
        mAdapter.selectedPosition = position;
        mAdapter.notifyDataSetChanged();
    }


    public void show(View anchor) {
        mPopupWindow.showAsDropDown(anchor);
    }


    public int getSelectedPosition() {
        return mAdapter.selectedPosition;
    }

    public void dismiss() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
    }


    final class SpinnerAdapter extends BaseAdapter {
        private int selectedPosition;
        private LayoutInflater mInflater;
        private List<CharSequence> mEntries;

        public SpinnerAdapter(Context context, int entriesResId) {
            mEntries = Arrays.asList(context.getResources().getTextArray(entriesResId));
            mInflater = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        }

        @Override
        public int getCount() {
            return mEntries.size();
        }

        @Override
        public Object getItem(int position) {
            return mEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.pref_spinner_item, parent, false);
            }

            ((CustomTextView) convertView).setText(mEntries.get(position));

            if (position == selectedPosition) {
                ((CustomTextView) convertView).setTextColor(selectColor);
            } else {
                ((CustomTextView) convertView).setTextColor(normalColor);
            }
            return convertView;
        }
    }

}
