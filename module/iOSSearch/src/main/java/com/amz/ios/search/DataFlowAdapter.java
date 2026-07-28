package com.amz.ios.search;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;

import com.amz.ios.http.Internal.MerlinTreeList;
import com.amz.ios.search.entities.BaseCardItemInfo;
import com.amz.ios.search.provider.AdapterItemPresenter;

import java.util.Comparator;
import java.util.List;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-14 下午8:54
 */
public class DataFlowAdapter extends RecyclerView.Adapter<AdapterItemPresenter.BaseViewHolder> {

    private static final String TAG = DataFlowAdapter.class.getSimpleName();

    private final List<BaseCardItemInfo> mDatas = new MerlinTreeList<>(new Comparator<BaseCardItemInfo>() {
        @Override
        public int compare(BaseCardItemInfo lhs, BaseCardItemInfo rhs) {
            return rhs.getShowLevel() - lhs.getShowLevel();
        }
    });

    public DataFlowAdapter(Context context) {
        mAdapterItemPresenter = new AdapterItemPresenter(context, mDatas);
    }

    private AdapterItemPresenter mAdapterItemPresenter;

    @Override
    public AdapterItemPresenter.BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return mAdapterItemPresenter.getViewHolderByType(viewType);
    }

    @Override
    public void onBindViewHolder(AdapterItemPresenter.BaseViewHolder holder, int position) {
        mAdapterItemPresenter.bindViewHolder(holder, mDatas.get(position));
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mAdapterItemPresenter.getItemViewType(position);
    }

    public DataFlowAdapter notifyChanged() {
        notifyDataSetChanged();
        Log.d(TAG, ">>>>>>DataFlowAdapter#notifyChanged : " + mDatas.toString());
        return this;
    }

    public void destory() {
        if (mDatas.size() <= 0) return;
        for (BaseCardItemInfo itemInfo : mDatas) {
            itemInfo.onDestroy();
        }
    }

    public static class Tes {

        public Tes(int a) {
            this.a = a;
        }

        int a = 1;

        @Override
        public String toString() {
            return "Tes{" +
                    "a=" + a +
                    '}';
        }
    }


    //+++++++++++++++++++++++++++++++++++++++++++
    //  collection delegate start
    //+++++++++++++++++++++++++++++++++++++++++++

    public BaseCardItemInfo get(int location) {
        return mDatas.get(location);
    }

    public boolean isEmpty() {
        return mDatas.isEmpty();
    }

    public DataFlowAdapter clear() {
        mDatas.clear();
        return this;
    }

    public int size() {
        return mDatas.size();
    }

    public boolean contains(Object object) {
        return mDatas.contains(object);
    }

    public DataFlowAdapter add(BaseCardItemInfo data) {
        if (data != null) {
            mDatas.add(data);
        }
        return this;
    }

    public DataFlowAdapter add(BaseCardItemInfo data, int pos) {
        if (data != null) {
            mDatas.add(pos, data);
        }
        return this;
    }

    public DataFlowAdapter addAll(List<BaseCardItemInfo> datas) {
        this.mDatas.addAll(datas);
        return this;
    }

    public DataFlowAdapter update(int position, BaseCardItemInfo data) {
        if (data != null) {
            mDatas.set(position, data);
        }
        return this;
    }

    public DataFlowAdapter remove(int position) {
        if (mDatas.size() > position && position >= 0) {
            mDatas.remove(position);
        }
        return this;
    }

    //+++++++++++++++++++++++++++++++++++++++++++
    //  collection delegate end
    //+++++++++++++++++++++++++++++++++++++++++++
}
