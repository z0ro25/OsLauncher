package com.amz.ios.themeclub.ui.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.RelativeLayout;

import com.amz.ios.ioslite.common.ContextHelper;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.ioslite.common.launcher.LauncherRouter;
import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.ioslite.common.util.ToastUtil;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.adapter.MineThemeAdpter;
import com.amz.ios.themeclub.base.BaseFragment;
import com.amz.ios.themeclub.bean.ThemeInfo;
import com.amz.ios.themeclub.dao.DataBaseOpenHelper;
import com.amz.ios.themeclub.delegate.AbsListViewDelegate;
import com.amz.ios.themeclub.intertfaces.IProgressView;
import com.amz.ios.themeclub.tools.ScrollableFragmentListener;
import com.amz.ios.themeclub.tools.ScrollableListener;
import com.amz.ios.themeclub.ui.activity.LocalThemeDetailActivity;
import com.amz.ios.themeclub.util.ThemeUtils;

/**
 * Created by GL on 16-11-16.
 */

public class MineThemeFragment extends BaseFragment implements ScrollableListener, IProgressView {
    private static final String TAG = MineThemeFragment.class.getSimpleName();
    private static final String BUNDLE_FRAGMENT_INDEX = "BaseFragment.BUNDLE_FRAGMENT_INDEXccc";

    private final String [] columns = {
            ThemeInfo.Impl._ID,
            ThemeInfo.Impl.THEME_PACKAGE_NAME,
            ThemeInfo.Impl.THEME_TITLE,
            ThemeInfo.Impl.THEME_THUMB,
            ThemeInfo.Impl.THEME_DESCRIPTION,
            ThemeInfo.Impl.THEME_PATH,
            ThemeInfo.Impl.THEME_LASTUPDATETIME};
//    private final String orderBy = ThemeInfo.Impl.THEME_LASTUPDATETIME + " DESC";
    private final String orderBy = ThemeInfo.Impl.THEME_DESCRIPTION + " ASC";

    private GridView mGridView;
    private RelativeLayout mProgress;

    protected int mFragmentIndex;

    private Cursor mCursor;
    private SQLiteDatabase db;
    private DataBaseOpenHelper helper;

    private MineThemeAdpter mMineThemeAdpter;
    protected ScrollableFragmentListener mListener;
    private AbsListViewDelegate mAbsListViewDelegate = new AbsListViewDelegate();

    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if(mMineThemeAdpter != null) {
                mMineThemeAdpter.notifyDataSetChanged();
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null && action.equals(ThemeUtils.BROADCAST_THEME_NEED_UPDATE)){
//                new LoadThemeTask().execute();
                new LoadThemeTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    };

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String packageNmae = null;
            Cursor c = (Cursor) mMineThemeAdpter.getItem(position);
            if (c == null) {
                return;
            }
            packageNmae = c.getString(c.getColumnIndex(ThemeInfo.Impl.THEME_PACKAGE_NAME));
            if (getContext().getPackageName().equals(packageNmae)) {
                if( !IOSSettings.getString(
                        getContext().getContentResolver(),
                        IOSSettings.Launcher.LAUNCHER_THEME_PACKAGE,
                        getContext().getPackageName()).equals(getContext().getPackageName()) ) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage(R.string.themeclub_chang_to_default_theme);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        LauncherRouter.launchToNewTheme(getActivity(), getContext().getPackageName());
                        IOSSettings.setWallpaperId(getActivity(), -1);
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                    return;
                }
            }else {
                if (IOSSettings.getString(getContext().getContentResolver(), IOSSettings.Launcher.LAUNCHER_THEME_PACKAGE, getContext().getPackageName()).equals(packageNmae)) {
                    ToastUtil.show(getContext(), R.string.themeclub_theme_has_been_applied);
                }else {
                    Intent intent2Detial = new Intent(getActivity(), LocalThemeDetailActivity.class);
                    intent2Detial.putExtra("themeInfo_packageName", packageNmae);
                    intent2Detial.putExtra("themeInfo_title", c.getString(c.getColumnIndex(ThemeInfo.Impl.THEME_TITLE)));
                    intent2Detial.putExtra("themeInfo_description", c.getString(c.getColumnIndex(ThemeInfo.Impl.THEME_DESCRIPTION)));
                    intent2Detial.putExtra("themeInfo_themePath", c.getString(c.getColumnIndex(ThemeInfo.Impl.THEME_PATH)));
                    startActivity(intent2Detial);
                }
            }

        }
    };

    @Override
    protected View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saveInstaceState) {
        return inflater.inflate(R.layout.fragment_mine_wallpaper_theme, container, false);
    }

    @Override
    protected void init(View rootView) {
        findView(rootView);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mFragmentIndex = bundle.getInt(BUNDLE_FRAGMENT_INDEX, 1);
        }
        if (mListener != null) {
            mListener.onFragmentAttached(this, 1);
        }
        mMineThemeAdpter = new MineThemeAdpter(getContext(), mCursor, true);
        mGridView.setAdapter(mMineThemeAdpter);
        mGridView.setOnItemClickListener(mItemClickListener);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ThemeUtils.BROADCAST_THEME_NEED_UPDATE);
        ContextHelper.registerReceiver(getActivity(), mBroadcastReceiver, filter);
        getActivity().getContentResolver().registerContentObserver(
                IOSSettings.getUriFor(IOSSettings.Launcher.LAUNCHER_THEME_PACKAGE),
                false,
                mObserver);
    }

    private void findView(View rootView){
        mGridView = (GridView) rootView.findViewById(R.id.grid_view);
        mProgress = (RelativeLayout) rootView.findViewById(R.id.progress);
    }

    @Override
    public void onDestroyView() {
        DebugLog.w(TAG, "==================onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DebugLog.w(TAG, "==================onDestroy");
        getActivity().unregisterReceiver(mBroadcastReceiver);
        getContext().getContentResolver().unregisterContentObserver(mObserver);
        if (db != null) {
            db.close();
        }
        if (mCursor != null) {
            mCursor.close();
        }
    }

    @Override
    public boolean isViewBeingDragged(MotionEvent event) {
        return mAbsListViewDelegate.isViewBeingDragged(event, mGridView);
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

    @Override
    public void onDetach() {
        if (mListener != null) {
            mListener.onFragmentDetached(this, mFragmentIndex);
        }
        super.onDetach();
        mListener = null;
    }

    @Override
    protected void fragmentLoadData() {
        new LoadThemeTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void showProgress() {
        if(mProgress!=null){
            mProgress.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void closeProgress() {
        mProgress.setVisibility(View.GONE);
//        mGridView.smoothScrollToPosition(getActiveCellIndex());
    }

    class LoadThemeTask extends AsyncTask<Void, Void ,Cursor>{

        @Override
        protected void onPreExecute() {
            showProgress();
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            mMineThemeAdpter.changeCursor(cursor);
            mCursor = cursor;
            closeProgress();
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            Cursor newCursor = null;
            if(helper == null){
                helper = new DataBaseOpenHelper(getContext());
            }
            if (db == null) {
                db = helper.getReadableDatabase();
            }
            newCursor = db.query(
                    DataBaseOpenHelper.DOWNLOAD_THEME_TABLE, columns, null, null, null, null, orderBy);

            return newCursor;
        }
    }

    private int getActiveCellIndex() {
        int nSelectedIndex = 0;
        while(mCursor.moveToNext()) {
            Context context = getContext();
            String packageName = mCursor.getString(mCursor.getColumnIndex(ThemeInfo.Impl.THEME_PACKAGE_NAME));
            if (IOSSettings.getString(context.getContentResolver(),
                    IOSSettings.Launcher.LAUNCHER_THEME_PACKAGE, context.getPackageName()).equals(packageName)) {
                return nSelectedIndex;
            }

            nSelectedIndex ++;
        }

        return 0;
    }
}