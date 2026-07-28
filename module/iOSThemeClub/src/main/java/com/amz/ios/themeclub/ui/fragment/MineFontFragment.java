package com.amz.ios.themeclub.ui.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.amz.ios.ioslite.common.ContextHelper;
import com.amz.ios.launcher.config.Settings;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.ThemeClubApplication;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.base.BaseFragment;
import com.amz.ios.themeclub.delegate.AbsListViewDelegate;
import com.amz.ios.themeclub.intertfaces.IProgressView;
import com.amz.ios.themeclub.model.FontModel;
import com.amz.ios.themeclub.tools.ScrollableFragmentListener;
import com.amz.ios.themeclub.tools.ScrollableListener;
import com.amz.ios.themeclub.ui.activity.CropImageActivity;
import com.amz.ios.themeclub.ui.activity.FontDetailActivity;
import com.amz.ios.themeclub.util.FileUtils;
import com.amz.ios.themeclub.util.WallpaperUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by
 */

public class MineFontFragment extends BaseFragment implements ScrollableListener,IProgressView {
    private final String TAG = "MineFontFragment";
    protected static final String BUNDLE_FRAGMENT_INDEX = "BaseFragment.BUNDLE_FRAGMENT_INDEX";
    private final String wallpaperNativePath = FileUtils.getInnerSDCardPath()+ AppConfig.WALLPAPER_NATIVE_PATH;
    private final int GALLERY_ID = 0;
    private final int LIVEWALLPAPER_ID = 1;
    private final int REQUEST_READ_EXTERNAL_STORAGE = 100;
    private final int CODE_GALLERY_WALLPAPER = 1001;

    private GridView mGridView;
    private List<FontModel> fontModels = new ArrayList<FontModel>();
    private View mLayout;
    private View mProgress;

    protected int mFragmentIndex;
    private Cursor mCursor;
    protected ScrollableFragmentListener mListener;
    private AbsListViewDelegate mAbsListViewDelegate = new AbsListViewDelegate();
    private MineFontAdapter mMineFontAdapter;
    private File wallpapaerDirectory;
    private ArrayList<String> mWallpapersPaths;
    private LoadFontTask mLoadTask;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Log.d(TAG, "onReceive: gaol action="+action);
            if(action != null && action.equals(WallpaperUtil.WALLPAPER_NEED_UPDATE)){
                Log.d(TAG, "onReceive: gaol WALLPAPER_NEED_UPDATE");
                getTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    };

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(getContext(), FontDetailActivity.class);
            intent.putExtra("fontdata", fontModels.get(position));
            startActivity(intent);
        }
    };

    private void findView(View rootView){
        mGridView = (GridView) rootView.findViewById(R.id.grid_view);
        mGridView.setNumColumns(2);
        mLayout = rootView.findViewById(R.id.mine_wallpaper);
        mProgress = rootView.findViewById(R.id.progress);

//        AppConfig.getDisplaySize(getContext());
    }

    @Override
    protected View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saveInstaceState) {
        return inflater.inflate(R.layout.fragment_mine_wallpaper_theme, container, false);
    }

    @Override
    protected void init(View rootView) {
        findView(rootView);
        RequestManager glide = Glide.with(MineFontFragment.this);
        mMineFontAdapter = new MineFontAdapter(getContext(),glide);
        mGridView.setAdapter(mMineFontAdapter);
        mGridView.setOnItemClickListener(mItemClickListener);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mFragmentIndex = bundle.getInt(BUNDLE_FRAGMENT_INDEX, 0);
        }
        if (mListener != null) {
            mListener.onFragmentAttached(this, 2);
        }
        if (ActivityCompat.checkSelfPermission(ThemeClubApplication.getContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestReadExternalStoragePermission();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WallpaperUtil.WALLPAPER_NEED_UPDATE);
        ContextHelper.registerReceiver(getActivity(), mBroadcastReceiver, intentFilter);
    }

    private synchronized void initData(){
        Log.d(TAG, "initData: ");
        fontModels.clear();
        loadFonts();
    }

    private void loadFonts(){
        fontModels.clear();
        int fontType = Settings.getWorkspaceTextFont(getContext());

        for (int i = 0; i < FontModel.FONT_COUNT; i++){
            FontModel model = new FontModel(i);
            model.setChecked(false);
            if (model.getType() == fontType){
                model.setChecked(true);
            }
            fontModels.add(model);
        }

//        mMineFontAdapter.notifyDataSetChanged();
    }


    private boolean checkIsImageFile(String path) {
        if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")) {
            return true;
        }
        return false;
    }

    private void requestReadExternalStoragePermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Snackbar.make(getActivity().getWindow().getDecorView().findViewById(R.id.main_content), R.string.themeclub_permission_read_external_storage_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            requestPermissions(
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    REQUEST_READ_EXTERNAL_STORAGE);
                            Log.d(TAG, "requestReadExternalStoragePermission onClick");
                        }
                    })
                    .show();
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: requestCode="+requestCode + "   permissions="+permissions + "  grantResults="+grantResults);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(mLayout, R.string.themeclub_permision_available_read_external_storage_rationale,
                        Snackbar.LENGTH_SHORT).show();
                getTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                Snackbar.make(mLayout, R.string.themeclub_permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CODE_GALLERY_WALLPAPER){
            if(data != null){
                Uri imageUri = Uri.parse(data.getDataString());
                Intent intent2Crop = new Intent(getActivity(), CropImageActivity.class);
                intent2Crop.putExtra("path", imageUri.toString());
                intent2Crop.putExtra("isUriOrPath",true);
                startActivity(intent2Crop);
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        loadFonts();
        mMineFontAdapter.notifyDataSetChanged();
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
    public void onDestroyView() {
        getActivity().unregisterReceiver(mBroadcastReceiver);
        if(mLoadTask!=null && mLoadTask.getStatus() == AsyncTask.Status.RUNNING) {
            mLoadTask.cancel(true);
            mLoadTask =null;
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Glide.get(getContext()).clearMemory();
    }

    @Override
    public boolean isViewBeingDragged(MotionEvent event) {
        return mAbsListViewDelegate.isViewBeingDragged(event, mGridView);
    }

    @Override
    protected void fragmentLoadData() {
        Log.d(TAG, "fragmentLoadData: ");
        if (ActivityCompat.checkSelfPermission(ThemeClubApplication.getContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            getTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
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
    }

//    public ArrayList<String> getWallpaperPaths() {
//        mWallpapersPaths = new ArrayList<>();
//        for(int i = 0;i< wallpapers.size();i++) {
//            mWallpapersPaths.add(wallpapers.get(i).path);
//        }
//        return mWallpapersPaths;
//    }

    class SpecialItem{
        private int id;
        private int titleResourcesId;
        private int imageResourcesId;
        private int backgroundColor;

        public SpecialItem(int id, int title, int resourcesId, int backgroundColor){
            this.id = id;
            this.titleResourcesId = title;
            this.imageResourcesId = resourcesId;
            this.backgroundColor = backgroundColor;
        }

        public int getTitleResourcesId() {
            return titleResourcesId;
        }

        public int getResourcesId() {
            return imageResourcesId;
        }

        public int getId(){
            return id;
        }

        public int getBackgroundColor() {
            return backgroundColor;
        }
    }


    class MineFontAdapter extends BaseAdapter {
        LayoutInflater mInflater;
        RequestManager mGlide;

        public MineFontAdapter(Context context,RequestManager glide){
            mInflater = LayoutInflater.from(context);
            mGlide = glide;
        }

        @Override
        public int getCount() {
            return fontModels.size();
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return fontModels.get(position);

        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rootView = null;
            ViewHolder viewHolder = null;

//            if ( convertView == null )
//            {
                rootView = mInflater.inflate(R.layout.font_item, null);
                viewHolder = new ViewHolder();
                viewHolder.mImg = (ImageView) rootView.findViewById(R.id.theme_view);
                viewHolder.iv_check = (ImageView) rootView.findViewById(R.id.check_view);
                viewHolder.tv_fontName = (CustomTextView) rootView.findViewById(R.id.font_name);
                viewHolder.themeLayout = (RelativeLayout) rootView.findViewById(R.id.theme_layout);
                viewHolder.tv_sumaryName = (CustomTextView) rootView.findViewById(R.id.sumary_textview);

//                final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(AppConfig.display_width/2-10, AppConfig.display_height/5);
//                viewHolder.themeLayout.setLayoutParams(layoutParams);

                final FontModel model = fontModels.get(position);
                Integer resId = model.getThumbResId();
                viewHolder.tv_fontName.setText(model.getFontName());
                viewHolder.tv_fontName.setSpecialFont(model.getType());
                viewHolder.tv_sumaryName.setSpecialFont(model.getType());
                viewHolder.tv_sumaryName.setText(R.string.themeclub_sumary);

                if (position == 0){
                    viewHolder.tv_sumaryName.setVisibility(View.VISIBLE);
                }
                else
                    viewHolder.tv_sumaryName.setVisibility(View.GONE);

                setCheckButtonState(model, viewHolder);
                rootView.setTag(viewHolder);

//            }else {
//                rootView = convertView;
//                viewHolder = (ViewHolder) convertView.getTag();
//            }
//            mGlide.load(fontModels.get(position)).into(holder.thumbnail);

            return rootView;
        }

        private void setCheckButtonState(FontModel model, ViewHolder holder){
            if (model.getChecked() == false){
                holder.iv_check.setVisibility(View.GONE);
            }
            else {
                holder.iv_check.setVisibility(View.VISIBLE);
            }
        }

        class ViewHolder{
            ImageView mImg;
            CustomTextView tv_fontName;
            ImageView iv_check;
            RelativeLayout themeLayout;
            CustomTextView tv_sumaryName;

        }
    }

    static class LoadFontTask extends AsyncTask{
        MineFontFragment mainContext;

        LoadFontTask(MineFontFragment context) {
            mainContext = context;
        }

        @Override
        protected void onPreExecute() {
            mainContext.showProgress();
        }

        @Override
        protected void onPostExecute(Object o) {
            mainContext.mMineFontAdapter.notifyDataSetChanged();
            mainContext.closeProgress();
        }

        @Override
        protected Object doInBackground(Object[] params) {
            if(isCancelled()) {
                return null;
            }
            mainContext.initData();
            return null;
        }
    }

    public LoadFontTask getTask() {
        if(mLoadTask == null) {
            return new LoadFontTask(this);
        }
        return mLoadTask;
    }

}