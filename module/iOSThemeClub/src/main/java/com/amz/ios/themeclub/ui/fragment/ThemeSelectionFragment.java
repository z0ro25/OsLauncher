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
import com.amz.ios.themeclub.adapter.ThemeSelectionAdapter;
import com.amz.ios.themeclub.base.BaseFragment;
import com.amz.ios.themeclub.bean.ThemeSelectionPieceBean;
import com.amz.ios.themeclub.bean.request.ThemeSelectionRequest;
import com.amz.ios.themeclub.intertfaces.IViewShowDatas;
import com.amz.ios.themeclub.presenter.ThemeSelectionPresenter;
import com.amz.ios.themeclub.tools.ItemClickListener;
import com.amz.ios.themeclub.ui.activity.OnlineThemeDetailActivity;
import com.amz.ios.themeclub.ui.activity.ThemeSelectionSpecialActivity;
import com.amz.ios.themeclub.util.DensityUtils;
import com.amz.ios.themeclub.util.NetWorkUtils;
import com.amz.ios.themeclub.view.ItemDecoration;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ZhangMingZhe on 11/16/16.
 */
public class ThemeSelectionFragment extends BaseFragment implements IViewShowDatas<ThemeSelectionPieceBean> {
    private final String TAG = ThemeSelectionFragment.class.getSimpleName();
    RecyclerView themeSelection;
    RelativeLayout mProgress;
    LinearLayout mNoNet;
    IOSLoadingView mNoDataClickArea;
    private ThemeSelectionAdapter mAdapter;
    private ArrayList<ThemeSelectionPieceBean.IssuesBean> mDatas = new ArrayList<>();
    private ThemeSelectionPresenter mPresenter;
    private int mStratNum = 0;
    private int mPageNumber = 6;


    @Override
    protected View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saveInstaceState) {
        return inflater.inflate(R.layout.theme_selection_fragment, null);
    }

    @Override
    protected void init(View v) {
        themeSelection = (RecyclerView) v.findViewById(R.id.theme_selection);
        mProgress = (RelativeLayout) v.findViewById(R.id.progress);
        mNoNet = (LinearLayout) v.findViewById(R.id.no_net);
        mNoDataClickArea = (IOSLoadingView) v.findViewById(R.id.no_net_click_area);
        mNoDataClickArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!NetWorkUtils.isNetworkConnected(getContext())&&isAdded()){
                    DebugLog.w("TryAgain8", "=================NoDataClick");
                    Toast.makeText(getContext(),getResources().getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
                    return;
                }
                showProgress();
                fragmentLoadData();
            }
        });
        initConfig();
        showProgress();
    }

    private void initConfig() {
        mAdapter = new ThemeSelectionAdapter(getContext(), mDatas);
        themeSelection.setLayoutManager(new LinearLayoutManager(getContext()));
        themeSelection.addItemDecoration(new ItemDecoration(0, 0, 0, 20));
        themeSelection.setAdapter(mAdapter);
        themeSelection.addOnItemTouchListener(new ItemClickListener() {

            @Override
            public void onItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
                int position = getCurrentPosition(view.getId());

                int type = baseQuickAdapter.getItemViewType(i);
                if (type == ThemeSelectionPieceBean.THEME_SELECTION_ITEM_X) {
                    if(position == -1){
                        return;
                    }
                    Intent xIntent = new Intent(getContext(), OnlineThemeDetailActivity.class);
                    xIntent.putExtra("themebean",mDatas.get(i).getThemes().get(position));
                    startActivity(xIntent);
                } else if (type == ThemeSelectionPieceBean.THEME_SELECTION_ITEM_Y) {
                    if (position == -1) {
                        return;
                    }else if(position == 3){
                        Intent intent = new Intent(getContext(), ThemeSelectionSpecialActivity.class);
                        intent.putExtra("title",mAdapter.getData().get(i).getTitle());
                        intent.putExtra("data",(Serializable) mAdapter.getData().get(i).getThemes());
                        startActivity(intent);
                    }else {
                        Intent yIntent = new Intent(getContext(), OnlineThemeDetailActivity.class);
                        yIntent.putExtra("themebean",mDatas.get(i).getThemes().get(position));
                        startActivity(yIntent);
                    }
                }
            }
        });
//        mAdapter.openLoadMore(mPageNumber);
        mAdapter.openLoadAnimation(mPageNumber);
        mAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                themeSelection.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!NetWorkUtils.isNetworkConnected(getContext())) {
                            if (isAdded() && !NetWorkUtils.isNetworkConnected(getContext())) {
                                DebugLog.w("TryAgain9", "=================onLoadMoreRequested");
                                Toast.makeText(getContext(), getString(R.string.themeclub_try_again), Toast.LENGTH_SHORT).show();
                            }
//                            mAdapter.hideLoadingMore();
                            mAdapter.loadMoreEnd();
                            return;
                        }
                        ThemeSelectionRequest request = new ThemeSelectionRequest(getContext(), mStratNum, mPageNumber);
                        mPresenter.getDatas(request);
                    }
                });
            }
        });
        loadingView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtils.dip2px(getContext(),40)));
//        mAdapter.setLoadingView(loadingView);
    }

    /**
     * @author ZhangMingZhe
     * create at 11/21/16 2:03 PM
     * »ñÈ¡±»µã»÷µÄImgViewµÄÎ»ÖÃ
     */
    private int getCurrentPosition(int id) {
        int position = -1;
        if(id == R.id.top_area){
            position = 3;
        }else if(id == R.id.theme_selection_y_1 || id == R.id.theme_selection_item_x){
            position = 0;
        }else if(id == R.id.theme_selection_y_2){
            position = 1;
        }else if(id == R.id.theme_selection_y_3){
            position = 2;
        }
        return position;
    }

    @Override
    protected void fragmentLoadData() {
        if(mPresenter==null){
            mPresenter = new ThemeSelectionPresenter(this);
        }
        ThemeSelectionRequest request = new ThemeSelectionRequest(getContext(), mStratNum, mPageNumber);
        mPresenter.getDatas(request);
    }

    @Override
    public void showDatas(ThemeSelectionPieceBean bean) {
        closeProgress();
//        mAdapter.hideLoadingMore();
        mAdapter.loadMoreEnd();
        //ÇëÇó³É¹¦
        if (bean!=null&&bean.getErrorCode() == 0) {
            if(mNoNet.getVisibility() == View.VISIBLE){
                closeNoNetConnectPage();
            }
            if (bean.getTotal() == 0) {
                String result ="";
                int toastID;
                if(mStratNum == 0){
                    toastID = R.string.themeclub_server_nodata;
                    showNoNetConnectPage();
                }else{
                    //²»ÎªµÚÒ»´Î¼ÓÔØ£¬·þÎñÆ÷·µ»ØÊý¾ÝÎª£°£¬Ôò·þÎñÆ÷Êý¾Ý¶ÁÍê
//                    mAdapter.loadComplete();
                    mAdapter.loadMoreComplete();
                    toastID = R.string.themeclub_no_more_data;
                }
                //Ã»ÓÐ¸ü¶à
                if(isAdded()&&getParentFragment()!=null&&getParentFragment().getUserVisibleHint()){
                    Toast.makeText(getContext(), getString(toastID), Toast.LENGTH_SHORT).show();
                }
                return;
            }
            mStratNum+=bean.getTotal();
            mAdapter.addData(bean.getIssues());
        }else{
//            mAdapter.hideLoadingMore();
            mAdapter.loadMoreEnd();
            if(mStratNum == 0){
                showNoNetConnectPage();
            }
            if (isAdded() && !NetWorkUtils.isNetworkConnected(getContext())) {
                DebugLog.w("TryAgain10", "=================showDatas");
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
        if(mPresenter!=null){
            mPresenter.destroyReference();
        }
    }
}