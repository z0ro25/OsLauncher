package com.amz.ios.themeclub.ui.fragment;

import android.content.Intent;
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
import com.amz.ios.themeclub.adapter.WallPaperSelectionAdapter;
import com.amz.ios.themeclub.base.BaseFragment;
import com.amz.ios.themeclub.bean.WallPaperSelectionPieceBean;
import com.amz.ios.themeclub.bean.WallPapersBean;
import com.amz.ios.themeclub.bean.request.WallPaperSelectionRequest;
import com.amz.ios.themeclub.intertfaces.IViewShowDatas;
import com.amz.ios.themeclub.intertfaces.ItemZClickListener;
import com.amz.ios.themeclub.presenter.WallPaperSelectionPresenter;
import com.amz.ios.themeclub.tools.ItemClickListener;
import com.amz.ios.themeclub.ui.activity.OnlineWallpaperDetailActivity;
import com.amz.ios.themeclub.ui.activity.OnlineWallpapersDetailActivity;
import com.amz.ios.themeclub.ui.activity.WallPaperSelectionSpecialActivity;
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

public class WallPaperSelectionFragment extends BaseFragment implements IViewShowDatas<WallPaperSelectionPieceBean> {
    RecyclerView wallpaperSelection;
    RelativeLayout mProgress;
    LinearLayout mNoNet;
    IOSLoadingView mNoNetClickArea;
    private WallPaperSelectionPresenter mPresenter;
    private WallPaperSelectionAdapter mAdapter;
    private final String TAG = WallPaperSelectionFragment.class.getSimpleName();
    private ArrayList<WallPaperSelectionPieceBean.IssuesBean> mDatas = new ArrayList<>();
    private int mStartNum = 0;
    private int mPageNumber = 3;
    private final int TOP_TITLE = 6;
    private IOSLoadingView mLoadingView;

    private int getCurrentPosition(int id) {
        int position = -1;
        if(id==R.id.img_one||id==R.id.img_1){
            position = 0;
        }else if(id == R.id.img_two|| id == R.id.img_2){
            position = 1;
        }else if(id == R.id.img_three || id == R.id.img_3){
            position = 2;
        }else if(id == R.id.img_4){
            position = 3;
        }else if(id == R.id.img_5){
            position = 4;
        }else if(id == R.id.img_6){
            position = 5;
        }else if(id == R.id.top_area){
            position = TOP_TITLE;
        }
        return position;
    }

    @Override
    protected void fragmentLoadData() {
        if (mPresenter == null) {
            mPresenter = new WallPaperSelectionPresenter(this);
        }
        WallPaperSelectionRequest request = new WallPaperSelectionRequest(getContext(), mStartNum, mPageNumber);
        mPresenter.getDatas(request);
    }

    @Override
    protected View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saveInstaceState) {
        return inflater.inflate(R.layout.wallpaper_selection_fragment,null);
    }

    @Override
    protected void init(View v) {
        wallpaperSelection = (RecyclerView) v.findViewById(R.id.wallpaper_selection);
        mLoadingView = (IOSLoadingView) v.findViewById(R.id.iosloadingView);
        mLoadingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoadingView.setVisibility(View.VISIBLE);
            }
        });
        mProgress = (RelativeLayout) v.findViewById(R.id.progress);
        mNoNet = (LinearLayout) v.findViewById(R.id.no_net);
        mNoNetClickArea = (IOSLoadingView) v.findViewById(R.id.no_net_click_area);
        mNoNetClickArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!NetWorkUtils.isNetworkConnected(getContext())&&isAdded()){
                    DebugLog.w("TryAgain14", "=================NoNetClick");
                    Toast.makeText(getContext(),getResources().getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
                    return;
                }
                showProgress();
                fragmentLoadData();
            }
        });
        mAdapter = new WallPaperSelectionAdapter(getContext(), mDatas);
        mAdapter.setOnItemZClickListener(new ItemZClickListener() {
            @Override
            public void onClick(WallPapersBean wallPapersBean) {
                Intent detailIntent = new Intent(getContext(), OnlineWallpaperDetailActivity.class);
                detailIntent.putExtra("wallpaperbean",wallPapersBean);
                startActivity(detailIntent);
            }
        });
        wallpaperSelection.setLayoutManager(new LinearLayoutManager(getContext()));
        wallpaperSelection.addItemDecoration(new ItemDecoration(0, 0, 0, 20));
//        mAdapter.openLoadMore(mPageNumber);
        mAdapter.openLoadAnimation(mPageNumber);
        wallpaperSelection.setAdapter(mAdapter);
        mAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                wallpaperSelection.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!NetWorkUtils.isNetworkConnected(getContext())) {
                            if (isAdded() && !NetWorkUtils.isNetworkConnected(getContext())) {
                                DebugLog.w("TryAgain15", "=================onLoadMoreRequested");
                                Toast.makeText(getContext(), getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
                            }
//                            mAdapter.hideLoadingMore();
                            mAdapter.loadMoreEnd();
                            return;
                        }
                        WallPaperSelectionRequest request = new WallPaperSelectionRequest(getContext(), mStartNum, mPageNumber);
                        mPresenter.getDatas(request);
//                        mAdapter.hideLoadingMore();
                    }
                });
            }
        });
        wallpaperSelection.addOnItemTouchListener(new ItemClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
                int position = getCurrentPosition(view.getId());
                if (position == -1) {
                    return;
                } else if (position == TOP_TITLE) {
                    Intent intent = new Intent(getContext(), WallPaperSelectionSpecialActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("data",mAdapter.getData().get(i));
                    intent.putExtras(bundle);
                    startActivity(intent);
                    return;
                } else {
                    List<WallPapersBean> wallpapers = new ArrayList<WallPapersBean>();
                    wallpapers.addAll(mAdapter.getData().get(i).getWallPapers());
                    Intent intent = new Intent(getContext(), OnlineWallpapersDetailActivity.class);
                    intent.putExtra(WallpaperUtil.ONLINEWALLPAPERLIST,(Serializable)wallpapers);
                    intent.putExtra(WallpaperUtil.ONLINEWALLPAPER_POSITION,position);
                    startActivity(intent);
                }
            }
        });
//        mAdapter.setLoadingView(loadingView);
        loadingView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtils.dip2px(getContext(),40)));
        showProgress();
    }

    @Override
    public void showDatas(WallPaperSelectionPieceBean bean) {
        closeProgress();
//        mAdapter.hideLoadingMore();
        mAdapter.loadMoreEnd();
        if (bean != null && bean.getErrorCode() == 0) {
            if(mNoNet.getVisibility() == View.VISIBLE){
                closeNoNetConnectPage();
            }
            if (bean.getTotal() == 0) {
                int toastID ;
                if(mStartNum == 0){
                    toastID = R.string.themeclub_server_nodata;
                    showNoNetConnectPage();
                }else{
//                    mAdapter.loadComplete();
                    mAdapter.loadMoreComplete();
                    toastID =  R.string.themeclub_no_more_data;
                }
                if(isAdded()&&getParentFragment()!=null&&getParentFragment().getUserVisibleHint()){
                    Toast.makeText(getContext(), getString(toastID), Toast.LENGTH_SHORT).show();
                }
                return;
            }
            mAdapter.addData(bean.getIssues());
            changeStartNumber(bean.getIssues().size());
        } else {
            if (mStartNum == 0) {
                showNoNetConnectPage();
            }
            if (isAdded() && !NetWorkUtils.isNetworkConnected(getContext())) {
                DebugLog.w("TryAgain16", "=================showDatas");
                Toast.makeText(getContext(), getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void changeStartNumber(int size) {
        mStartNum += size;
    }

    @Override
    public void showProgress() {
        mProgress.setVisibility(View.VISIBLE);
        mLoadingView.setVisibility(View.VISIBLE);
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
        if(mPresenter!=null){
            mPresenter.destroyReference();
        }
    }
}
