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
import com.amz.ios.ioslite.common.ad.IOSAdConfig;
import com.amz.ios.ioslite.common.ad.IOSAdManager;
import com.amz.ios.ioslite.common.ad.IOSListAd;
import com.amz.ios.ioslite.common.ad.IOSListAdListener;
import com.amz.ios.ioslite.common.ad.IOSNAdResponse;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.view.IOSLoadingView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.ThemeClubApplication;
import com.amz.ios.themeclub.adapter.WallPaperNewestAdapter;
import com.amz.ios.themeclub.base.BaseFragment;
import com.amz.ios.themeclub.bean.WallPaperJsonBean;
import com.amz.ios.themeclub.bean.WallPaperNewestPieceBean;
import com.amz.ios.themeclub.bean.WallPapersBean;
import com.amz.ios.themeclub.bean.request.WallPaperNewestRequest;
import com.amz.ios.themeclub.intertfaces.IViewShowDatas;
import com.amz.ios.themeclub.presenter.WallPaperNewestPresenter;
import com.amz.ios.themeclub.tools.ItemClickListener;
import com.amz.ios.themeclub.ui.activity.OnlineWallpapersDetailActivity;
import com.amz.ios.themeclub.ui.activity.SourceDetailActivity;
import com.amz.ios.themeclub.util.DensityUtils;
import com.amz.ios.themeclub.util.NetWorkUtils;
import com.amz.ios.themeclub.util.WallpaperUtil;
import com.amz.ios.themeclub.view.ItemDecoration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by ZhangMingZhe on 11/16/16.
 */

public class WallPaperNewestFragment extends BaseFragment implements IViewShowDatas<WallPaperJsonBean> {
    RecyclerView mRecycleView;
    RelativeLayout mProgress;
    LinearLayout mNoNetPage;
    IOSLoadingView mNoDataClickArea;
    private WallPaperNewestAdapter mAdapter;
    private List<WallPaperNewestPieceBean> mData = new ArrayList<>();
    private WallPaperNewestPresenter mPresenter;
    private int mStartNumber = 0;
    private final int mPageNumber = 6;
    private int mId =-1;
    private String mSource="";
    private IOSLoadingView mLoadingView;
    private int mAdPosition = 0;
    //ÍøÂçÁ´½Ó²»ºÃÊ±£¬ÖØÐÂ¼ÓÔØµÄ´ÎÊý£®³¬¹ý¶þ´Î²»ÔÙÇëÇó
    private int mReLoadCount = 1;
    private int mLastItemType = -1;
    private ArrayList<IOSNAdResponse> mAdList;
    private IOSListAd ad;
    private IOSListAdListener mIOSAdListener;
    private final String TAG = getClass().getSimpleName();


    @Override
    protected View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saveInstaceState) {
        return inflater.inflate(R.layout.themeclub_newest_fragment, null);
    }

    @Override
    protected void init(View v) {
        mAdapter = new WallPaperNewestAdapter(ThemeClubApplication.getContext(), mData);
        findViewById(v);
        mRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecycleView.setAdapter(mAdapter);
//        mAdapter.openLoadMore(2);
        mAdapter.openLoadAnimation(2);
        mAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                DebugLog.w(TAG,"================onLoadMoreListener");
                mRecycleView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!NetWorkUtils.isNetworkConnected(getContext())) {
                            if (isAdded() && !NetWorkUtils.isNetworkConnected(getContext())) {
                                DebugLog.w("TryAgain11", "=================onLoadMoreRequested");
                                Toast.makeText(getContext(), getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
                            }
//                            mAdapter.hideLoadingMore();
                            mAdapter.loadMoreEnd();
                            return;
                        }
                        mPresenter.getDatas(new WallPaperNewestRequest(getContext(), mStartNumber, 3 * mPageNumber,mId, mSource));
                    }
                });
            }
        });
        mRecycleView.addOnItemTouchListener(new ItemClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
                int position = getCurrentPositin(view.getId());
                if (position == -1) {
                    return;
                }
                List<WallPapersBean> wallpapers = new ArrayList<WallPapersBean>();
                wallpapers.addAll(mAdapter.getData().get(i).getmData());
                if(mAdapter.getData().size() > i+1&& mAdapter.getData().get(i+1).getmData()!=null) {
                    wallpapers.addAll(mAdapter.getData().get(i+1).getmData());
                }
                Intent intent = new Intent(getContext(), OnlineWallpapersDetailActivity.class);
                intent.putExtra(WallpaperUtil.ONLINEWALLPAPERLIST,(Serializable)wallpapers);
                intent.putExtra(WallpaperUtil.ONLINEWALLPAPER_POSITION,position);
                startActivity(intent);
            }
        });
        loadingView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtils.dip2px(getContext(),40)));
//        mAdapter.setLoadingView(loadingView);
        mRecycleView.addItemDecoration(new ItemDecoration(0, 0, 0, 4));
        showProgress();
    }


    private void findViewById(View v) {
        mRecycleView = (RecyclerView) v.findViewById(R.id.newest_rcy);
        mProgress = (RelativeLayout) v.findViewById(R.id.progress);
        mNoNetPage = (LinearLayout) v.findViewById(R.id.no_net);
        mLoadingView = (IOSLoadingView) v.findViewById(R.id.iosloadingView);
        mLoadingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoadingView.setVisibility(View.VISIBLE);
            }
        });
        mNoDataClickArea = (IOSLoadingView) v.findViewById(R.id.no_net_click_area);
        mNoDataClickArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!NetWorkUtils.isNetworkConnected(getContext())&&isAdded()){
                    DebugLog.w("TryAgain12", "=================NoDataClick");
                    Toast.makeText(getContext(),getResources().getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
                    return;
                }
                showProgress();
                fragmentLoadData();
            }
        });
    }


    private int getCurrentPositin(int id) {
        int position = -1;
        if(id == R.id.img_1 || id ==R.id.y_img_1){
            position = 0;
        }else if(id == R.id.img_2 || id == R.id.y_img_2){
            position = 1;
        }else if(id ==  R.id.img_3 || id ==R.id.y_img_3){
            position = 2;
        }else if(id == R.id.img_4 || id == R.id.y_img_4){
            position = 3;
        }else if(id == R.id.img_5 || id == R.id.y_img_5){
            position = 4;
        }else if(id == R.id.img_6 || id == R.id.y_img_6){
            position = 5;
        }

        return position;
    }


    @Override
    protected void fragmentLoadData() {
        if(getArguments()!=null){
            mId = getArguments().getInt("id");
            mSource = getArguments().getString("source");
        }
        if(mPresenter == null){
            mPresenter = new WallPaperNewestPresenter(this);
        }
        WallPaperNewestRequest request = new WallPaperNewestRequest(getContext(), mStartNumber, 3 * mPageNumber, mId,mSource);
        mPresenter.getDatas(request);
        initAd();
        getAdData();
    }



    public void getAdData() {
        Log.e(TAG,"ONCE");
        if(ad != null) {
            ad.setNativeAdListener(mIOSAdListener);
            ad.load();
        }
    }

    public void initAd() {
        mAdList = new ArrayList<IOSNAdResponse>();
        ad = IOSAdManager.getInstance(getContext()).getListAd(IOSAdConfig.ID_LIST_WALLPAPER);
        mIOSAdListener = new IOSListAdListener() {
            @Override
            public void onError(com.amz.ios.ioslite.common.ad.IOSAdError error) {
                Log.e(TAG, "error = " + error.getErrorMessage());
                if (mReLoadCount < 2) {
                    mReLoadCount += 1;
                    getAdData();
                }
            }

            @Override
            public void onAdLoaded(List<? extends IOSNAdResponse> responses) {
                mAdList.addAll(responses);
            }
        };
    }


    @Override
    public void showProgress() {
        mProgress.setVisibility(View.VISIBLE);
        mLoadingView.setVisibility(View.VISIBLE);
    }

    @Override
    public void showNoNetConnectPage() {
        mNoNetPage.setVisibility(View.VISIBLE);
    }

    @Override
    public void closeProgress() {
        mProgress.setVisibility(View.GONE);
    }

    @Override
    public void closeNoNetConnectPage() {
        mNoNetPage.setVisibility(View.GONE);
    }

    @Override
    public void showDatas(WallPaperJsonBean wallPaperJsonBean) {
        closeProgress();
        //ÇëÇó³É¹¦
        if (wallPaperJsonBean != null && wallPaperJsonBean.getErrorCode() == 0) {

            if(mNoNetPage.getVisibility() == View.VISIBLE){
                closeNoNetConnectPage();
            }

            if(getActivity() instanceof SourceDetailActivity && ( (SourceDetailActivity)getActivity()).isDescriptionNull()){
                ( (SourceDetailActivity)getActivity()).setTopViews(wallPaperJsonBean.getSourceDescription());
            }

            if (wallPaperJsonBean.getTotal() == 0) {
                int toastId ;
                if(mStartNumber == 0){
                    toastId = R.string.themeclub_server_nodata;
                    showNoNetConnectPage();
                }else{
                    //²»ÎªµÚÒ»´Î¼ÓÔØ£¬·þÎñÆ÷·µ»ØÊý¾ÝÎª£°£¬Ôò·þÎñÆ÷Êý¾Ý¶ÁÍê
//                    mAdapter.loadComplete();
                    mAdapter.loadMoreComplete();
                    toastId = R.string.themeclub_no_more_data;
                }
                //Ã»ÓÐ¸ü¶à
                if(getParentFragment()==null&&isAdded()){
                    //À´Ô´ÏêÇéµ÷ÓÃ£¬parent Îªnull
                    Toast.makeText(getContext(), getString(toastId), Toast.LENGTH_SHORT).show();
                }else{
                    if(getParentFragment()!=null&&getParentFragment().getUserVisibleHint()&&isAdded()){
                        Toast.makeText(getContext(), getString(toastId), Toast.LENGTH_SHORT).show();
                    }
                }
                return;
            }
            int piece = wallPaperJsonBean.getTotal() / 6;
            if (piece == 0) {
                //ÉÙÓÚ6,Ö±½ÓÍË³ö
//                mAdapter.loadComplete();
                mAdapter.loadMoreComplete();
                int toastId = R.string.themeclub_no_more_data;
                if(mStartNumber==0){
                    toastId = R.string.themeclub_server_nodata;
                }
                if(isAdded()){
                    Toast.makeText(getContext(), getString(toastId), Toast.LENGTH_SHORT).show();
                }
                return;
            }
            ArrayList<WallPaperNewestPieceBean> datas = new ArrayList<>();
            //×Ô¼º·â×°³É¿é×´Êý¾Ý,Ã¿¿é£¶Ìõ wallpaper
            int TypeA = WallPaperNewestPieceBean.WALLPAPER_TYPE_X;
            int TypeB = WallPaperNewestPieceBean.WALLPAPER_TYPE_Y;
            if(mLastItemType != -1){
                if(mLastItemType == WallPaperNewestPieceBean.WALLPAPER_TYPE_X){
                    TypeA = WallPaperNewestPieceBean.WALLPAPER_TYPE_Y;
                    TypeB = WallPaperNewestPieceBean.WALLPAPER_TYPE_X;
                }else{
                    TypeA = WallPaperNewestPieceBean.WALLPAPER_TYPE_X;
                    TypeB = WallPaperNewestPieceBean.WALLPAPER_TYPE_Y;
                }
            }
            for (int i = 0, j = 0; i < piece; i++, j += mPageNumber) {
                WallPaperNewestPieceBean bean = new WallPaperNewestPieceBean();
                bean.setmSourceDescription(wallPaperJsonBean.getSourceDescription());
                bean.setmSourceUrl(wallPaperJsonBean.getSourceUrl());
                bean.setmData(wallPaperJsonBean.getWallPapers().subList(j, j + mPageNumber));

                bean.setItemType(i % 2 == 0 ? TypeA: TypeB);
                datas.add(bean);
                if(i == piece-1){
                    mLastItemType = bean.getItemType();
                }
            }
            if(piece == 3 && mAdList!=null){
                if(mAdList.size()!=0){
                    IOSNAdResponse adData = mAdList.get(mAdPosition);
                    //Ã¿Èý¿éÌí¼ÓÒ»Ìõ¹ã¸æ
                    WallPaperNewestPieceBean adBean = new WallPaperNewestPieceBean();
                    adBean.setItemType(WallPaperNewestPieceBean.WALLPAPER_TYPE_AD);
                    adBean.setAdBean(adData);
                    mAdPosition++;
                    if(mAdPosition==mAdList.size()){
                        Log.e(TAG,"ad List = " + mAdList.size());
                        //µ½×îºóÒ»Ìõºó£¬ÔÙ¶ÁµÚÒ»Ìõ
                        mAdPosition = 0;
                        //ÖØÐÂ¼ÓÔØÊý¾Ý
                        getAdData();
                    }
                    datas.add(adBean);
                }
            }
            mAdapter.addData(datas);
            changemStartNumber(wallPaperJsonBean.getWallPapers().size());
            datas.clear();
        } else {
//            mAdapter.hideLoadingMore();
            mAdapter.loadMoreEnd();
            //µÚÒ»´ÎÇëÇóµÄÊ±ºò£¬ÎÞÖµ
            if (mStartNumber == 0) {
                showNoNetConnectPage();
            }
            //½çÃæÉÏ»¹ÓÐÊý¾Ý,ÌáÊ¾Ò»ÏÂ
            if (isAdded() && !NetWorkUtils.isNetworkConnected(getContext())) {
                DebugLog.w("TryAgain13", "=================showDatas");
                Toast.makeText(getContext(),getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * @author ZhangMingZhe
     * create at 11/21/16 4:14 PM
     * ÇëÇó½á¹û½øÐÐÀÛ¼Ó
     */
    private void changemStartNumber(int size) {
        mStartNumber += size;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mPresenter!=null){
            mPresenter.destroyReference();
        }
    }
}
