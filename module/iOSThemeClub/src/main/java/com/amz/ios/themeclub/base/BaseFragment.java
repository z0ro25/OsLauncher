package com.amz.ios.themeclub.base;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.themeclub.R;

/**
 * Created by server on 16-11-17.
 */


public abstract class BaseFragment extends Fragment {
    public View loadingView;
    private boolean mCreatViewFinish;
    private final String TAG = getClass().getSimpleName();
    private boolean mIsVisibleToUser;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        mIsVisibleToUser = isVisibleToUser;
        lazyLoad();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = createView(inflater,container,savedInstanceState);
        loadingView = inflater.inflate(R.layout.loading_view,null);
        init(v);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCreatViewFinish = true;
        lazyLoad();
    }

    private void lazyLoad() {
        DebugLog.w(TAG,"=================lazyLoad:"+mCreatViewFinish+"/"+mIsVisibleToUser);
        if (mCreatViewFinish&&mIsVisibleToUser){
            fragmentLoadData();
        }
    }

    protected abstract void fragmentLoadData();

    protected abstract View createView(LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle saveInstaceState);

    protected abstract void init(View v);

}