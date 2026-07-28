package com.amz.ios.themeclub.ui.fragment;


import android.content.Context;
import android.content.Intent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.adapter.ThemeNewestAdapter;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.bean.ThemeNewestBean;
import com.amz.ios.themeclub.bean.ThemesBean;
import com.amz.ios.themeclub.bean.request.ThemeNewestRequest;
import com.amz.ios.themeclub.intertfaces.IViewShowDatas;
import com.amz.ios.themeclub.presenter.ThemeNewestPresenter;
import com.amz.ios.themeclub.ui.activity.OnlineThemeDetailActivity;
import com.amz.ios.themeclub.util.DensityUtils;
import com.amz.ios.themeclub.util.NetWorkUtils;
import com.amz.ios.themeclub.view.ItemDecoration;

import java.util.ArrayList;
import java.util.List;

import static com.amz.ios.themeclub.ThemeClubApplication.getContext;

public class LoadView implements IViewShowDatas<ThemeNewestBean> {
    private final String TAG  = LoadView.class.getSimpleName();
    private Context mContext;
    RecyclerView themeNewestRecycle;
    private LinearLayoutManager mLinearLayoutManager;
    private ThemeNewestAdapter mAdapter;
    private ThemeNewestPresenter mPresenter;
    private ArrayList<ThemesBean> list = new ArrayList<>();
    private int mStartNum = 0;
    private int mPageNum = 3;
    private String mSource = "";
    private int mId = -1;
    private  ArrayList<IOSNAdResponse> mAdList ;
    private int mPosition = 0;
    private RelativeLayout mProgressBar;
    private LinearLayout mLoadFailed;
    private IOSListAd ad;
    private IOSListAdListener mIOSAdListener;
    public LoadView(final Context context, final RecyclerView view, final FrameLayout space) {
        mContext = context;
        themeNewestRecycle = view;
        addProgress(space);
        initAD();
        getAdData();
        initViewConfig();
    }

    private void addProgress(FrameLayout space) {
        mProgressBar = (RelativeLayout) LayoutInflater.from(getContext()).inflate(R.layout.themeclub_loading_layout,null);
        space.addView(mProgressBar);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.themeclub_load_fail_page,null);
        mLoadFailed = (LinearLayout) view.findViewById(R.id.no_net);
        mLoadFailed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!NetWorkUtils.isNetworkConnected(getContext())){
                    DebugLog.w("TryAgain2", "=================LoadFailed Click");
                    Toast.makeText(getContext(),getContext().getResources().getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mLoadFailed.getVisibility()==View.VISIBLE){
                    mLoadFailed.setVisibility(View.GONE);
                }
                showProgress();
                fragmentLoadData();
            }
        });
        space.addView(view);
    }

    public void refresh() {
        themeNewestRecycle.setVisibility(View.GONE);

        if(mAdapter!=null){
            mAdapter.getData().clear();
            mAdapter.notifyDataSetChanged();
        }
        mStartNum = 0;
        Log.d(TAG,"line(63) + #refresh");
        showProgress();
        fragmentLoadData();
    }

    private void initAD() {
        mAdList = new ArrayList<IOSNAdResponse>();
        ad = IOSAdManager.getInstance(getContext()).getListAd(IOSAdConfig.ID_LIST_THEME);
        mIOSAdListener = new IOSListAdListener() {
            @Override
            public void onError(com.amz.ios.ioslite.common.ad.IOSAdError error) {
                Log.e(TAG, "error = " + error.getErrorMessage());
            }

            @Override
            public void onAdLoaded(List<? extends IOSNAdResponse> responses) {
                mAdList.addAll(responses);
            }
        };
    }

    private void getAdData() {
        if(ad != null) {
            ad.setNativeAdListener(mIOSAdListener);
            ad.load();
        }
    }

    public LinearLayoutManager getLinearLayoutManager() {
        return mLinearLayoutManager;
    }

    protected void fragmentLoadData() {
        //如果父fragment 为不显示状态不加载数据
        //显示，加载数据
        if(mPresenter == null){
            mPresenter = new ThemeNewestPresenter(this);
        }

        ThemeNewestRequest request = new ThemeNewestRequest(mContext, mStartNum, mPageNum, mId, mSource, AppConfig.COLUMN_NEWEEST);
        mPresenter.getDatas(request);

    }

    private void initViewConfig() {
        mLinearLayoutManager = new LinearLayoutManager(mContext);
        themeNewestRecycle.setLayoutManager(new LinearLayoutManager(mContext));
        mAdapter = new ThemeNewestAdapter(mContext, list);
        themeNewestRecycle.setAdapter(mAdapter);
        themeNewestRecycle.addItemDecoration(new ItemDecoration(0, 0, 0, 20));
        themeNewestRecycle.addOnItemTouchListener(new OnItemChildClickListener() {
//            @Override
//            public void SimpleOnItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
//                Intent intent = new Intent(mContext, OnlineThemeDetailActivity.class);
//                intent.putExtra("themebean", mAdapter.getData().get(i));
//                mContext.startActivity(intent);
//            }

            @Override
            public void onSimpleItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
                Intent intent = new Intent(mContext, OnlineThemeDetailActivity.class);
                intent.putExtra("themebean", mAdapter.getData().get(i));
                mContext.startActivity(intent);

            }
        });
//        mAdapter.openLoadMore(2);
        mAdapter.openLoadAnimation(2);
        mAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                if (!NetWorkUtils.isNetworkConnected(mContext)) {
//                    mAdapter.hideLoadingMore();
                    mAdapter.loadMoreEnd();
                    return;
                }
                themeNewestRecycle.post(new Runnable() {
                    @Override
                    public void run() {
                        ThemeNewestRequest request = new ThemeNewestRequest(mContext, mStartNum, mPageNum, mId, mSource, AppConfig.COLUMN_NEWEEST);
                        mPresenter.getDatas(request);
                    }
                });
            }
        });
        View loadingview =  LayoutInflater.from(mContext).inflate(R.layout.loading_view, null);
        loadingview.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtils.dip2px(mContext, 40)));
//        mAdapter.setLoadingView(loadingview);

    }

    @Override
    public void showDatas(ThemeNewestBean bean) {
//        mAdapter.hideLoadingMore();
        mAdapter.loadMoreEnd();
        closeProgress();
        //请求成功
        if (bean!=null && bean.getErrorCode() == 0) {
            Log.d(TAG,"line(141) + #showThemeNewest"+bean.toString());

            themeNewestRecycle.setVisibility(View.VISIBLE);
            if (bean.getTotal() == 0) {
                String result ="";
                if(mStartNum==0){
                    //第一次加载数据，为０，则服务器无数据
                    showNoNetConnectPage();
                }else{
                    //不为第一次加载，服务器返回数据为０，则服务器数据读完
//                    mAdapter.loadComplete();
                    mAdapter.loadMoreComplete();
                }

                return;
            }
            if(mAdList!=null&&mPageNum == bean.getTotal()){
                if(mAdList.size()!=0){
                    ThemesBean temp = new ThemesBean();
                    temp.setmAd(mAdList.get(mPosition));
                    bean.getThemes().add(temp);
                    //只要不为-1，就表示为广告
                    temp.setPosition(2);
                    mPosition++;
                    if(mPosition == mAdList.size()){
                        mPosition = 0;
                        getAdData();
                    }
                }
            }
            mAdapter.addData(bean.getThemes());
            mStartNum += bean.getTotal();
        }else{
//            mAdapter.hideLoadingMore();
            mAdapter.loadMoreEnd();
            if(mStartNum == 0){
                showNoNetConnectPage();
            }
        }
    }

    @Override
    public void showProgress() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void showNoNetConnectPage() {
        mLoadFailed.setVisibility(View.VISIBLE);
    }

    @Override
    public void closeProgress() {
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void closeNoNetConnectPage() {
        mLoadFailed.setVisibility(View.GONE);
    }
}
