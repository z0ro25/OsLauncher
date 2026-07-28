package com.amz.ios.themeclub.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemChildClickListener;
import com.amz.ios.ioslite.common.ad.IOSAdConfig;
import com.amz.ios.ioslite.common.ad.IOSAdManager;
import com.amz.ios.ioslite.common.ad.IOSListAd;
import com.amz.ios.ioslite.common.ad.IOSListAdListener;
import com.amz.ios.ioslite.common.ad.IOSNAdResponse;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.view.IOSLoadingView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.adapter.ThemeNewestAdapter;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.base.BaseFragment;
import com.amz.ios.themeclub.bean.ThemeNewestBean;
import com.amz.ios.themeclub.bean.ThemesBean;
import com.amz.ios.themeclub.bean.request.ThemeNewestRequest;
import com.amz.ios.themeclub.intertfaces.IViewShowDatas;
import com.amz.ios.themeclub.presenter.ThemeNewestPresenter;
import com.amz.ios.themeclub.ui.activity.OnlineThemeDetailActivity;
import com.amz.ios.themeclub.ui.activity.SourceDetailActivity;
import com.amz.ios.themeclub.util.DensityUtils;
import com.amz.ios.themeclub.util.NetWorkUtils;
import com.amz.ios.themeclub.view.ItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class ThemeNewestFragment extends BaseFragment implements IViewShowDatas<ThemeNewestBean> {
    private final String TAG = ThemeNewestFragment.class.getSimpleName();
    RecyclerView themeNewestRecycle;
    RelativeLayout mProgress;
    LinearLayout mNoNet;
    IOSLoadingView mNoNetClickArea;
    private ThemeNewestAdapter mAdapter;
    private ThemeNewestPresenter mPresenter;
    private ArrayList<ThemesBean> list = new ArrayList<>();
    private int mStartNum = 0;
    private int mPageNum = 3;
    private String mSource = "";
    private int mId = -1;
    private int mReloadCount = 0;
    private ArrayList<IOSNAdResponse> mAdList;
    private int mPosition = 0;
    private IOSListAd mAd;
    private IOSListAdListener mIOSAdListener;

    @Override
    protected View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saveInstaceState) {
        return inflater.inflate(R.layout.theme_newest_fragment, null);
    }

    @Override
    protected void init(View v) {
        themeNewestRecycle = (RecyclerView) v.findViewById(R.id.theme_newest_recycle);
        mProgress = (RelativeLayout) v.findViewById(R.id.progress);
        mNoNet = (LinearLayout) v.findViewById(R.id.no_net);
        mNoNetClickArea = (IOSLoadingView) v.findViewById(R.id.no_net_click_area);
        mNoNetClickArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetWorkUtils.isNetworkConnected(getContext()) && isAdded()) {
                    DebugLog.w("TryAgain5", "=================NoNetClick");
                    Toast.makeText(getContext(), getResources().getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
                    return;
                }
                showProgress();
                fragmentLoadData();
            }
        });
        initViewConfig();
        initAdData();
        showProgress();
    }

    private void initAdData() {
        mAdList = new ArrayList<IOSNAdResponse>();
        mAd = IOSAdManager.getInstance(getContext()).getListAd(IOSAdConfig.ID_LIST_THEME);
        mIOSAdListener = new IOSListAdListener() {
            @Override
            public void onError(com.amz.ios.ioslite.common.ad.IOSAdError error) {
                Log.e(TAG, "error = " + error.getErrorMessage());
                if (mReloadCount < 2) {
                    mReloadCount += 1;
                    getAdData();
                }
            }

            @Override
            public void onAdLoaded(List<? extends IOSNAdResponse> responses) {
                mAdList.addAll(responses);
            }
        };
    }

    public void getAdData() {
        if(mAd != null) {
            mAd.setNativeAdListener(mIOSAdListener);
            mAd.load();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void initViewConfig() {
        themeNewestRecycle.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new ThemeNewestAdapter(getContext(), list);
        themeNewestRecycle.setAdapter(mAdapter);
        themeNewestRecycle.addItemDecoration(new ItemDecoration(0, 0, 0, 20));
        themeNewestRecycle.addOnItemTouchListener(new OnItemChildClickListener() {
//            @Override
//            public void SimpleOnItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
//                DebugLog.i(TAG, "==================SimpleOnItemChildClick:" + mAdapter.getData().get(i) + "/" + view + "/" + i);
//                Intent intent = new Intent(getContext(), OnlineThemeDetailActivity.class);
//                intent.putExtra("themebean", mAdapter.getData().get(i));
//                startActivity(intent);
//            }

            @Override
            public void onSimpleItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
                DebugLog.i(TAG, "==================SimpleOnItemChildClick:" + mAdapter.getData().get(i) + "/" + view + "/" + i);
                Intent intent = new Intent(getContext(), OnlineThemeDetailActivity.class);
                intent.putExtra("themebean", mAdapter.getData().get(i));
                startActivity(intent);

            }
        });
//        mAdapter.openLoadMore(3);
        mAdapter.openLoadAnimation(3);
        mAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                if (!NetWorkUtils.isNetworkConnected(getContext())) {
                    if (isAdded() && !NetWorkUtils.isNetworkConnected(getContext())) {
                        DebugLog.w("TryAgain6", "=================onLoadMoreRequested");
                        Toast.makeText(getContext(), getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
                    }
//                    mAdapter.hideLoadingMore();
                    mAdapter.loadMoreEnd();
                    return;
                }
                themeNewestRecycle.post(new Runnable() {
                    @Override
                    public void run() {
                        ThemeNewestRequest request = new ThemeNewestRequest(getContext(), mStartNum, mPageNum, mId, mSource, AppConfig.COLUMN_NEWEEST);
                        mPresenter.getDatas(request);
                    }
                });
            }
        });
        loadingView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtils.dip2px(getContext(), 40)));
//        mAdapter.setLoadingView(loadingView);
    }

    @Override
    protected void fragmentLoadData() {
        if (mPresenter == null) {
            mPresenter = new ThemeNewestPresenter(this);
        }
        if (getArguments() != null) {
            mId = getArguments().getInt("id");
            mSource = getArguments().getString("source");
        }
        ThemeNewestRequest request = new ThemeNewestRequest(getContext(), mStartNum, mPageNum, mId, mSource, AppConfig.COLUMN_NEWEEST);
        mPresenter.getDatas(request);
        initAdData();
        getAdData();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPresenter != null) {
            mPresenter.destroyRefrence();
        }
    }

    @Override
    public void showNoNetConnectPage() {
        mNoNet.setVisibility(View.VISIBLE);
    }

    @Override
    public void closeProgress() {
        mProgress.setVisibility(View.GONE);
    }

    @Override
    public void closeNoNetConnectPage() {
        mNoNet.setVisibility(View.GONE);
    }


    @Override
    public void showDatas(ThemeNewestBean bean) {
//        mAdapter.hideLoadingMore();
        mAdapter.loadMoreEnd();
        closeProgress();
        if (bean != null && bean.getErrorCode() == 0) {
            if (mNoNet.getVisibility() == View.VISIBLE) {
                closeNoNetConnectPage();
            }
            if (getActivity() instanceof SourceDetailActivity && ((SourceDetailActivity) getActivity()).isDescriptionNull()) {
                ((SourceDetailActivity) getActivity()).setTopViews(bean.getSourceDescription());
            }
            if (bean.getTotal() == 0) {
                String result = "";
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
            if (mAdList != null && mPageNum == bean.getTotal()) {
                if (mAdList.size() != 0) {
                    ThemesBean temp = new ThemesBean();
                    temp.setmAd(mAdList.get(mPosition));
                    bean.getThemes().add(temp);
                    temp.setPosition(2);
                    mPosition++;
                    if (mPosition == mAdList.size()) {
                        mPosition = 0;
                        getAdData();
                    }
                }
            }

            mAdapter.addData(bean.getThemes());
            mStartNum += bean.getTotal();
        } else {
//            mAdapter.hideLoadingMore();
            mAdapter.loadMoreEnd();
            if (mStartNum == 0) {
                showNoNetConnectPage();
            }
            if (isAdded() && getParentFragment() != null && getParentFragment().getUserVisibleHint() && !NetWorkUtils.isNetworkConnected(getContext())) {
                DebugLog.w("TryAgain7", "=================showDatas");
                Toast.makeText(getContext(), getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void showProgress() {
        if (mProgress != null) {
            mProgress.setVisibility(View.VISIBLE);
        }
    }
}