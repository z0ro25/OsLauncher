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

/**
 * Created by ZhangMingZhe on 12/12/16.
 */

public class ThemeMinimalistFragment extends BaseFragment implements IViewShowDatas<ThemeNewestBean> {
    private final String TAG = ThemeMinimalistFragment.class.getSimpleName();
    private int mStartNum = 0;
    private int mPageNum = 3;
    private ThemeNewestPresenter presenter;
    private RecyclerView themeMinimalistRecycle;
    private RelativeLayout mProgress;
    private LinearLayout mNoNet;
    private IOSLoadingView mNoNetClickArea;
    private ThemeNewestAdapter mAdapter;
    private ArrayList<IOSNAdResponse> mAdList;
    private int mReloadCount= 0;
    private int mPosition = 0;
    private IOSListAd mAd;
    private IOSListAdListener mIOSAdListener;


    @Override
    protected View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saveInstaceState) {
        return inflater.inflate(R.layout.theme_minimalist_fragment,null);
    }

    @Override
    protected void init(View view) {
        initAdData();
        themeMinimalistRecycle = (RecyclerView) view.findViewById(R.id.theme_mamimalist_ryclecle);
        mProgress = (RelativeLayout) view.findViewById(R.id.progress);
        mNoNet = (LinearLayout) view.findViewById(R.id.no_net);
        mNoNetClickArea = (IOSLoadingView) view.findViewById(R.id.no_net_click_area);
        mNoNetClickArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgress();
                fragmentLoadData();
            }
        });
        initConfig();
        showProgress();
    }

    private void initConfig() {
        mAdapter = new ThemeNewestAdapter(getContext(),null);
        themeMinimalistRecycle.setLayoutManager(new LinearLayoutManager(getContext()));
        themeMinimalistRecycle.setAdapter(mAdapter);
        themeMinimalistRecycle.addItemDecoration(new ItemDecoration(0, 0, 0, 20));

        themeMinimalistRecycle.addOnItemTouchListener(new OnItemChildClickListener() {
//            @Override
//            public void SimpleOnItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
//                Intent intent = new Intent(getContext(), OnlineThemeDetailActivity.class);
//                intent.putExtra("themebean",mAdapter.getData().get(i));
//                startActivity(intent);
//            }

            @Override
            public void onSimpleItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
                Intent intent = new Intent(getContext(), OnlineThemeDetailActivity.class);
                intent.putExtra("themebean",mAdapter.getData().get(i));
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
                        DebugLog.w("TryAgain3", "=================onLoadMoreRequested");
                        Toast.makeText(getContext(), getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
                    }
//                    mAdapter.hideLoadingMore();
                    mAdapter.loadMoreEnd();
                    return;
                }
                themeMinimalistRecycle.post(new Runnable() {
                    @Override
                    public void run() {
                        ThemeNewestRequest request = new ThemeNewestRequest(getContext(), mStartNum, mPageNum, -1, "", AppConfig.COLUMN_MANIMALIST);
                        presenter.getDatas(request);
                    }
                });
            }
        });
        loadingView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtils.dip2px(getContext(),40)));
//        mAdapter.setLoadingView(loadingView);
    }

    @Override
    protected void fragmentLoadData(){
        if(presenter==null){
            presenter = new ThemeNewestPresenter(this);
        }
        ThemeNewestRequest request = new ThemeNewestRequest(getContext(),mStartNum,mPageNum,-1,"", AppConfig.COLUMN_MANIMALIST);
        presenter.getDatas(request);
        getAdData();
    }

    private void initAdData() {
        mAdList = new ArrayList<IOSNAdResponse>();
        mAd = IOSAdManager.getInstance(getContext()).getListAd(IOSAdConfig.ID_LIST_MIN_THEME);
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
        Log.e(TAG,"ONCE");
        if(mAd != null) {
            mAd.setNativeAdListener(mIOSAdListener);
            mAd.load();
        }
    }

    @Override
    public void showDatas(ThemeNewestBean bean) {
//        mAdapter.hideLoadingMore();
        mAdapter.loadMoreEnd();
        closeProgress();
        if (bean!=null && bean.getErrorCode() == 0) {
            Log.d(TAG,"line(45) + #showThemeNewest="+bean.toString());
            if(mNoNet.getVisibility() == View.VISIBLE){
                closeNoNetConnectPage();
            }
            if(getActivity() instanceof SourceDetailActivity && ( (SourceDetailActivity)getActivity()).isDescriptionNull()){
                ( (SourceDetailActivity)getActivity()).setTopViews(bean.getSourceDescription());
            }
            if (bean.getTotal() == 0) {
                String result ="";
                int toastID;
                if(mStartNum==0){
                    toastID = R.string.themeclub_server_nodata;
                    showNoNetConnectPage();
                }else{
//                    mAdapter.loadComplete();
                    mAdapter.loadMoreComplete();
                    toastID =  R.string.themeclub_no_more_data;
                }
                if(isAdded()&&getParentFragment()==null){
                    Toast.makeText(getContext(), getString(toastID), Toast.LENGTH_SHORT).show();
                }else{
                    if(isAdded()&&getParentFragment().getUserVisibleHint()){
                        Toast.makeText(getContext(), getString(toastID), Toast.LENGTH_SHORT).show();
                    }
                }
                return;
            }
            if(mAdList!=null&&mPageNum == bean.getTotal()){
                if(mAdList.size()!=0){
                    ThemesBean temp = new ThemesBean();
                    temp.setmAd(mAdList.get(mPosition));
                    bean.getThemes().add(temp);
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
            if (isAdded() && getParentFragment() != null && getParentFragment().getUserVisibleHint() && !NetWorkUtils.isNetworkConnected(getContext())) {
                DebugLog.w("TryAgain4", "=================showDatas");
                Toast.makeText(getContext(),getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void showProgress() {
        mProgress.setVisibility(View.VISIBLE);
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
    public void onDestroy() {
        super.onDestroy();
        if(presenter!=null){
            presenter.destroyRefrence();
        }
    }
}
