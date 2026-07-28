package com.amz.ios.themeclub.ui.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.RelativeLayout;

import com.amz.ios.ioslite.common.ContextHelper;
import com.amz.ios.ioslite.common.util.ToastUtil;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.adapter.MineLockscreenAdapter;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.base.BaseFragment;
import com.amz.ios.themeclub.bean.LockscreenInfo;
import com.amz.ios.themeclub.delegate.AbsListViewDelegate;
import com.amz.ios.themeclub.intertfaces.IProgressView;
import com.amz.ios.themeclub.tools.ScrollableFragmentListener;
import com.amz.ios.themeclub.tools.ScrollableListener;
import com.amz.ios.themeclub.ui.activity.LocalLockScreenDetailActivity;
import com.amz.ios.themeclub.util.LockScreenUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by ubuntu on 20/06/17.
 */

public class MineLockScreenFragment extends BaseFragment implements ScrollableListener, IProgressView {

    private static final String TAG = MineLockScreenFragment.class.getSimpleName();
    private GridView mGrid;
    private RelativeLayout mProgress;
    private MineLockscreenAdapter mAdapter;
    private ArrayList<LockscreenInfo> mLockscreenPackagesList = new ArrayList<>();
    private List<LockscreenInfo> mLockScreenList;
    private String[] mPackageNames;
    private String[] mPackagePaths;
    private String[] mTitles;
    private final static String PACKAGE = "package";
    private AbsListViewDelegate mAbsListViewDelegate = new AbsListViewDelegate();
    protected ScrollableFragmentListener mListener;
    private static final String BUNDLE_FRAGMENT_INDEX = "BaseFragment.BUNDLE_FRAGMENT_INDEXddd";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme(PACKAGE);
        ContextHelper.registerReceiver(getActivity(), mPMSReceiver, filter);
    }

    @Override
    protected void fragmentLoadData() {
        initData();
    }

    @Override
    protected View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            saveInstaceState) {
        return inflater.inflate(R.layout.mine_fragmen_lockscreen, container, false);
    }

    @Override
    protected void init(View v) {
        Bundle bundle = getArguments();
        if (bundle != null) {
            mFragmentIndex = bundle.getInt(BUNDLE_FRAGMENT_INDEX, 2);
        }
        if (mListener != null) {
            mListener.onFragmentAttached(this, 2);
        }
        mProgress = (RelativeLayout) v.findViewById(R.id.progress);
        mGrid = (GridView) v.findViewById(R.id.grid_view);
        mAdapter = new MineLockscreenAdapter(getActivity());
        mGrid.setAdapter(mAdapter);
        mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mLockScreenList!=null&&mLockScreenList.size()>0){
                    final LockscreenInfo lockscreenInfo = mLockScreenList.get(position);
                    final String packageName = lockscreenInfo.getPackageName();
                    if (LockScreenUtils.getLockscreenPackage(getActivity()).equals(packageName)) {
                        ToastUtil.show(getContext(), R.string.themeclub_lockscreen_has_bean_applied);
                    } else if (getContext().getPackageName().equals(packageName)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage(R.string.themeclub_chang_to_default_lockscreen);
                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                LockScreenUtils.applyLockScreen(MineLockScreenFragment.this.getActivity(),lockscreenInfo);
                                ToastUtil.show(getContext(), R.string.themeclub_set_lockscreen_succeed);
                            }
                        });
                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }else {
                        startLocalScreenDetail(lockscreenInfo);
                    }
                }
            }
        });
    }

    private void startLocalScreenDetail(LockscreenInfo lockscreenInfo) {
        final Intent intent = new Intent(getActivity(), LocalLockScreenDetailActivity.class);
        intent.putExtra("lockscreenInfo_packageName", lockscreenInfo.getPackageName());
        intent.putExtra("lockscreenInfo_title", lockscreenInfo.getTitle());
        intent.putExtra("lockscreenInfo_path", lockscreenInfo.getPackagePath());
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mPMSReceiver);
        super.onDestroy();
    }

    private static final String START_STRING = "com.ios.lockscreen.";


    private BroadcastReceiver mPMSReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            initData();
            mAdapter.notifyDataSetChanged();
        }
    };

    private LoadLockscreenPackageTask mLoadTask = null;

    private void initData() {
        if (mLoadTask != null && !mLoadTask.isCancelled()) {
            mLoadTask.cancel(true);
            mLoadTask = null;
        }
        mLoadTask = new LoadLockscreenPackageTask(getActivity());
        mLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private synchronized ArrayList<LockscreenInfo> loadLockscreenPackages() {
        if (null != mLockscreenPackagesList) {
            mLockscreenPackagesList.clear();
        }

        if (getActivity() == null) {
            return null;
        }
        List<PackageInfo> packages = getActivity().getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);

            if (packageInfo.packageName.startsWith(START_STRING) || packageInfo.packageName.equals(AppConfig.THEMECLUB_PREVIEW_DEFAULT)) {

                LockscreenInfo lockscreenInfo = new LockscreenInfo(getActivity(),packageInfo.packageName, packageInfo.packageName,
                        packageInfo.applicationInfo.sourceDir);

                if (lockscreenInfo.isLockscreenPackage() || packageInfo.packageName.equals(AppConfig.THEMECLUB_PREVIEW_DEFAULT)) {
                    mLockscreenPackagesList.add(lockscreenInfo);
                    Log.e(TAG,packageInfo.packageName);
                }
            }
        }

        mPackageNames = new String[mLockscreenPackagesList.size()];
        mPackagePaths = new String[mLockscreenPackagesList.size()];
        mTitles = new String[mLockscreenPackagesList.size()];
        Collections.sort(mLockscreenPackagesList, new Comparator<LockscreenInfo>() {

            @Override
            public int compare(LockscreenInfo lhs, LockscreenInfo rhs) {
                if (lhs.getTitle().equals(android.os.Build.MODEL)) {
                    return -1;
                } else if (rhs.getTitle().equals(android.os.Build.MODEL)) {
                    return 1;
                }
                return 0;
            }

        });
        int i = 0;
        for (LockscreenInfo lockscreenInfo : mLockscreenPackagesList) {
            mPackageNames[i] = lockscreenInfo.getPackageName();
            mPackagePaths[i] = lockscreenInfo.getPackagePath();
            mTitles[i] = lockscreenInfo.getTitle();
            i++;
        }

        return mLockscreenPackagesList;
    }

    @Override
    public boolean isViewBeingDragged(MotionEvent event) {
        return mAbsListViewDelegate.isViewBeingDragged(event, mGrid);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (ScrollableFragmentListener) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    context.toString() + " must implement ScrollableFragmentListener");
        }
    }
    protected int mFragmentIndex;

    @Override
    public void onDetach() {
        if (mListener != null) {
            mListener.onFragmentDetached(this, mFragmentIndex);
        }
        super.onDetach();
        mListener = null;
    }

    @Override
    public void showProgress() {
        if(mProgress!=null){
            mProgress.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void closeProgress() {
        mProgress.setVisibility(View.GONE);
    }

    class LoadLockscreenPackageTask extends AsyncTask<Void, Void, List<LockscreenInfo>> {
        public LoadLockscreenPackageTask(Context context) {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress();
        }

        @Override
        protected List<LockscreenInfo> doInBackground(Void... pramas) {

            return MineLockScreenFragment.this.loadLockscreenPackages();
        }

        @Override
        protected void onPostExecute(List<LockscreenInfo> result) {
            mLockScreenList = result;
            mAdapter.setData(result);
            closeProgress();
            mAdapter.notifyDataSetChanged();
        }
    }

}
