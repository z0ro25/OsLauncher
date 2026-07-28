package com.amz.ios.themeclub.ui.fragment;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.view.IOSLoadingView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.adapter.LockScreenNewestAdapter;
import com.amz.ios.themeclub.base.BaseFragment;
import com.amz.ios.themeclub.bean.LockScreenBean;
import com.amz.ios.themeclub.bean.LockScreenNewestBean;
import com.amz.ios.themeclub.bean.LockScreenNewestItem;
import com.amz.ios.themeclub.intertfaces.IViewShowDatas;
import com.amz.ios.themeclub.presenter.LockScreenNewstPresenter;
import com.amz.ios.themeclub.util.DensityUtils;
import com.amz.ios.themeclub.util.NetWorkUtils;
import com.amz.ios.themeclub.view.ItemDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by server on 17-8-31.
 */
public abstract class LockScreenBaseFragment extends BaseFragment implements IViewShowDatas<LockScreenNewestBean> {
    private RecyclerView mRecyclerView;
    public LockScreenNewstPresenter mPresenter;
    public int mRequestNum = 9;
    public int mStartNum = 0;
    private LockScreenNewestAdapter mAdapter;
    private ArrayList<LockScreenNewestItem> mList = new ArrayList<>();

    private RelativeLayout mProgress;
    private LinearLayout mNoNet;
    private IOSLoadingView mNoNetClickArea;
    private final String TAG = getClass().getSimpleName();
    private int mItemDataSize = 3;
    private boolean mLoadMoreEnd = false;

    protected abstract void loadData();

    @Override
    protected void fragmentLoadData() {
        loadData();
    }

    @Override
    protected View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            saveInstaceState) {
        return inflater.inflate(R.layout.theme_newest_fragment, null);
    }

    @Override
    protected void init(View v) {
        mRecyclerView = (RecyclerView) v.findViewById(R.id.theme_newest_recycle);
        mProgress = (RelativeLayout) v.findViewById(R.id.progress);
        mNoNet = (LinearLayout) v.findViewById(R.id.no_net);
        mNoNetClickArea = (IOSLoadingView) v.findViewById(R.id.no_net_click_area);
        mNoNetClickArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetWorkUtils.isNetworkConnected(getContext()) && isAdded()) {
                    Toast.makeText(getContext(), getResources().getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
                    return;
                }
                showProgress();
                loadData();
            }
        });
        setUpView();
        showProgress();
    }

    private void setUpView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new LockScreenNewestAdapter(getContext(), null);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new ItemDecoration(0, 0, 0, DensityUtils.dip2px(getActivity(), 2)));
//        mAdapter.openLoadMore(1);
        mAdapter.openLoadAnimation(1);
        mAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                DebugLog.w(TAG, "=================onLoadMoreRequested");
                if (!NetWorkUtils.isNetworkConnected(getContext())) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
                    }
//                    mAdapter.hideLoadingMore();
                    mAdapter.loadMoreEnd();

                    return;
                }

                if (!mLoadMoreEnd) {
//                    mAdapter.hideLoadingMore();
                    mAdapter.loadMoreEnd();

                    return;
                }

                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        mLoadMoreEnd = false;
                        loadData();
                    }
                });
            }
        });
        loadingView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtils.dip2px(getContext(), 40)));
//        mAdapter.setLoadingView(loadingView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPresenter != null) {
            mPresenter.destroyRefrence();
        }
    }

    private int mPageIndex = 0;

    @Override
    public void showDatas(LockScreenNewestBean bean) {
//        mAdapter.hideLoadingMore();
        mAdapter.loadMoreEnd();
        closeProgress();
        if (bean != null && bean.getErrorCode() == 0) {
            if (mNoNet.getVisibility() == View.VISIBLE) {
                closeNoNetConnectPage();
            }
            List<LockScreenBean> lockScreenBeanList = bean.getScreens();
            if (lockScreenBeanList != null && lockScreenBeanList.size() == 0) {
                int toastID;
                if (mStartNum == 0) {
                    toastID = R.string.themeclub_server_nodata;
                    showNoNetConnectPage();
                } else {
//                    mAdapter.loadComplete();
                    mAdapter.loadMoreComplete();
                    toastID = R.string.themeclub_no_more_data;
                }
                if (isAdded() && getParentFragment() == null) {
                    Toast.makeText(getContext(), getString(toastID), Toast.LENGTH_SHORT).show();
                } else {
                    if (isAdded() && getParentFragment() != null && getParentFragment().getUserVisibleHint()) {
                        Toast.makeText(getContext(), getString(toastID), Toast.LENGTH_SHORT).show();
                    }
                }
                return;
            }
            mList.clear();
            int size = lockScreenBeanList.size();
            final int sizeTemp = size % mItemDataSize;
            if (sizeTemp == 1) {
                lockScreenBeanList.add(new LockScreenBean());
                lockScreenBeanList.add(new LockScreenBean());
            } else if (sizeTemp == 2) {
                lockScreenBeanList.add(new LockScreenBean());
            }
            for (int i = 0; i < lockScreenBeanList.size(); i = i + 3) {
                List<LockScreenBean> lockScreenBeans = new ArrayList<>();
                lockScreenBeans.add(lockScreenBeanList.get(i));
                lockScreenBeans.add(lockScreenBeanList.get(i + 1));
                lockScreenBeans.add(lockScreenBeanList.get(i + 2));
                LockScreenNewestItem lockScreenNewestItem = new LockScreenNewestItem();
                lockScreenNewestItem.setmLockScreenBeans(lockScreenBeans);
                mList.add(lockScreenNewestItem);
            }
            mAdapter.addData(mList);
            mPageIndex++;
            mStartNum = mPageIndex * mRequestNum;
        } else {
//            mAdapter.hideLoadingMore();
            mAdapter.loadMoreEnd();
            if (mStartNum == 0) {
                showNoNetConnectPage();
            }
            if (isAdded() && getParentFragment() != null && getParentFragment().getUserVisibleHint()) {
                Toast.makeText(getContext(), getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
            }
        }
        mLoadMoreEnd = true;
    }

    @Override
    public void showProgress() {
        if (mProgress != null) mProgress.setVisibility(View.VISIBLE);

    }

    @Override
    public void showNoNetConnectPage() {
        if (mNoNet != null) mNoNet.setVisibility(View.VISIBLE);
    }

    @Override
    public void closeProgress() {
        if (mProgress != null) mProgress.setVisibility(View.GONE);
    }

    @Override
    public void closeNoNetConnectPage() {
        if (mNoNet != null) mNoNet.setVisibility(View.GONE);
    }
}
