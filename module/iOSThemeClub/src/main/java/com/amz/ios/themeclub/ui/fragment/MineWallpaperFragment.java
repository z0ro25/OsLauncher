package com.amz.ios.themeclub.ui.fragment;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import com.amz.ios.ioslite.common.config.ThemeConfig;
import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.ThemeClubApplication;
import com.amz.ios.themeclub.app.AppConfig;
import com.amz.ios.themeclub.base.BaseFragment;
import com.amz.ios.themeclub.bean.NativeWallpaperBean;
import com.amz.ios.themeclub.delegate.AbsListViewDelegate;
import com.amz.ios.themeclub.intertfaces.IProgressView;
import com.amz.ios.themeclub.partner.Partner;
import com.amz.ios.themeclub.tools.ScrollableFragmentListener;
import com.amz.ios.themeclub.tools.ScrollableListener;
import com.amz.ios.themeclub.ui.activity.CropImageActivity;
import com.amz.ios.themeclub.ui.activity.ShowDownloadedWallpaperActivity;
import com.amz.ios.themeclub.util.FileUtils;
import com.amz.ios.themeclub.util.WallpaperUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by server on
 *
 */

public class MineWallpaperFragment extends BaseFragment implements ScrollableListener,IProgressView {
    private final String TAG = "MineWallpaperFragment";
    protected static final String BUNDLE_FRAGMENT_INDEX = "BaseFragment.BUNDLE_FRAGMENT_INDEX";
    private final String wallpaperNativePath = FileUtils.getInnerSDCardPath()+AppConfig.WALLPAPER_NATIVE_PATH;
    public static final String ALLOW_EXTENTION = "png";
    public static final String SYSTEM_WALLPAPER_FOLDER = "/system/media/wallpaper";
//    public static final String SYSTEM_WALLPAPER_FOLDER = "/mnt/sdcard/wallpaper";

    private final int GALLERY_ID = 0;
    private final int LIVEWALLPAPER_ID = 1;
    private final int REQUEST_READ_EXTERNAL_STORAGE = 100;
    private final int CODE_GALLERY_WALLPAPER = 1001;

    private GridView mGridView;
    private List<NativeWallpaperBean> wallpapers = new ArrayList<NativeWallpaperBean>();
    private View mLayout;
    private View mProgress;

    protected int mFragmentIndex;
    private Cursor mCursor;
    private List<SpecialItem> mSpecialItems;
    protected ScrollableFragmentListener mListener;
    private AbsListViewDelegate mAbsListViewDelegate = new AbsListViewDelegate();
    private MineWallpaperAdapter mMineWallpaperAdapter;
    private File wallpapaerDirectory;
    private ArrayList<String> mWallpapersPaths;
    private LoadWallpaperTask mLoadTask;
    private int mSelectedWallpaperId = -1;

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
            Intent intent;
            int specialId = -1;
            if(position < mSpecialItems.size()) {
                specialId = mSpecialItems.get(position).getId();
            }
            switch (specialId){
                case GALLERY_ID:
                    if (ActivityCompat.checkSelfPermission(ThemeClubApplication.getContext(),
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestReadExternalStoragePermission();
                    }else {
                        intent = new Intent();
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                        }else {
                            intent.setAction(Intent.ACTION_PICK);
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                        }
                        intent.setType("image/*");
                        startActivityForResult(intent, CODE_GALLERY_WALLPAPER);
                    }
                    return;
                case LIVEWALLPAPER_ID:
                    intent = new Intent(Intent.ACTION_VIEW);
                    intent.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                    startActivity(intent);
                    return;
            }

            Intent intent2SetWallpapaer = new Intent(getActivity(), ShowDownloadedWallpaperActivity.class);
            intent2SetWallpapaer.putExtra(WallpaperUtil.LOCALWALLPAPER_POSITION, position-mSpecialItems.size());
            intent2SetWallpapaer.putExtra(WallpaperUtil.LOCALWALLPAPER_PATHS,getWallpaperPaths());
            startActivity(intent2SetWallpapaer);
        }
    };

    private void findView(View rootView){
        mGridView = (GridView) rootView.findViewById(R.id.grid_view);
        mLayout = rootView.findViewById(R.id.mine_wallpaper);
        mProgress = rootView.findViewById(R.id.progress);
    }

    @Override
    protected View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saveInstaceState) {
        return inflater.inflate(R.layout.fragment_mine_wallpaper_theme, container, false);
    }

    @Override
    protected void init(View rootView) {
        findView(rootView);
        initSpecialItem();
        RequestManager glide = Glide.with(MineWallpaperFragment.this);
        mMineWallpaperAdapter = new MineWallpaperAdapter(getContext(),glide);
        mGridView.setAdapter(mMineWallpaperAdapter);
        mGridView.setOnItemClickListener(mItemClickListener);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mFragmentIndex = bundle.getInt(BUNDLE_FRAGMENT_INDEX, 0);
        }
        if (mListener != null) {
            mListener.onFragmentAttached(this, 0);
        }
        if (ActivityCompat.checkSelfPermission(ThemeClubApplication.getContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestReadExternalStoragePermission();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WallpaperUtil.WALLPAPER_NEED_UPDATE);
        ContextHelper.registerReceiver(getActivity(), mBroadcastReceiver, intentFilter);
    }

    private int getWallpaperId(Context context) {
        return IOSSettings.getInt(context.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WALLPAPER_ID, -1);
    }

    private synchronized void initData(){
        Log.d(TAG, "initData: ");
        mSelectedWallpaperId = getWallpaperId(getContext());
        wallpapers.clear();
        loadSystemWallpapers();
        loadDownloadedWallpaper();
    }

    private void loadSystemWallpapers() {

        final PackageManager pm = ThemeClubApplication.getContext().getPackageManager();
        Partner partner = Partner.get(pm);
        if (partner != null) {
            final Resources partnerRes = partner.getResources();
            // Add system wallpapers
            File systemDir = partner.getWallpaperDirectory();
            if (systemDir != null && systemDir.isDirectory()) {
                for (File file : systemDir.listFiles()) {
                    if (!file.isFile()) {
                        continue;
                    }
                    String name = file.getName();
                    int dotPos = name.lastIndexOf('.');
                    String extension = "";
                    if (dotPos >= -1) {
                        extension = name.substring(dotPos);
                        name = name.substring(0, dotPos);
                    }

                    if (name.endsWith("_small")) {
                        // it is a thumbnail
                        continue;
                    }

                    File wallpaper = new File(systemDir, name + extension);
                    NativeWallpaperBean nativeWallpaperBean = new NativeWallpaperBean();
                    nativeWallpaperBean.path = wallpaper.getAbsolutePath();
                    nativeWallpaperBean.lastModified = wallpaper.lastModified();
                    wallpapers.add(nativeWallpaperBean);
                }
            }
        }


        File systemDir = new File(SYSTEM_WALLPAPER_FOLDER);
        if (systemDir != null && systemDir.isDirectory()) {
            File[] files = systemDir.listFiles();
            if (files == null)
                return;

            for (File file : systemDir.listFiles()) {
                if (!file.isFile()) {
                    continue;
                }

                String fileExtention = file.getName();
                fileExtention = fileExtention.substring(fileExtention.length() - 3).toLowerCase();
                if (fileExtention.contentEquals(ALLOW_EXTENTION)) {
                    NativeWallpaperBean nativeWallpaperBean = new NativeWallpaperBean();
                    nativeWallpaperBean.path = file.getAbsolutePath();
                    nativeWallpaperBean.lastModified = file.lastModified();
                    wallpapers.add(nativeWallpaperBean);
                }
            }
        }
    }

    private void loadDownloadedWallpaper(){
        if (ActivityCompat.checkSelfPermission(ThemeClubApplication.getContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestReadExternalStoragePermission();
        } else {
            wallpapaerDirectory = new File(wallpaperNativePath);
            File[] files = wallpapaerDirectory.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (checkIsImageFile(files[i].getName())) {
                        NativeWallpaperBean nativeWallpaperBean = new NativeWallpaperBean();
                        nativeWallpaperBean.path = files[i].getAbsolutePath();
                        nativeWallpaperBean.lastModified = files[i].lastModified();
                        wallpapers.add(nativeWallpaperBean);
                    }
                }
            }
        }
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

    private void initSpecialItem(){
        mSpecialItems = new ArrayList<SpecialItem>();
        mSpecialItems.add(new SpecialItem(GALLERY_ID, R.string.themeclub_gallery, R.drawable.theme_club_gallery, R.color.mine_gallery_bg));
        if(ThemeConfig.isLiveWallpaperEnable()) {
            mSpecialItems.add(new SpecialItem(LIVEWALLPAPER_ID, R.string.themeclub_live_wallpaper, R.drawable.themeclub_tab_mine, R.color.mine_livewallpaper_bg));
        }
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

    public ArrayList<String> getWallpaperPaths() {
        mWallpapersPaths = new ArrayList<>();
        for(int i = 0;i< wallpapers.size();i++) {
            mWallpapersPaths.add(wallpapers.get(i).path);
        }
        return mWallpapersPaths;
    }

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


    class MineWallpaperAdapter extends BaseAdapter {
        LayoutInflater mInflater;
        RequestManager mGlide;

        public MineWallpaperAdapter(Context context,RequestManager glide){
            mInflater = LayoutInflater.from(context);
            mGlide = glide;
        }

        @Override
        public int getCount() {
            return mSpecialItems.size()+wallpapers.size();
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if(position < mSpecialItems.size()){
                return 0;
            }
            return 1;
        }

        @Override
        public Object getItem(int position) {
            if(position < mSpecialItems.size()){
                return mSpecialItems.get(position);
            }
            return wallpapers.get(position-mSpecialItems.size());
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rootView = null;
            ViewHolder holder = null;

            if(position < mSpecialItems.size()){
                rootView = mInflater.inflate(R.layout.mine_wallpaper_special_item, null);
                RelativeLayout container = (RelativeLayout) rootView.findViewById(R.id.container);
                ImageView image = (ImageView) rootView.findViewById(R.id.image);
                CustomTextView text = (CustomTextView) rootView.findViewById(R.id.text);
                container.setBackgroundColor(getContext().getResources().getColor(mSpecialItems.get(position).getBackgroundColor()));
                image.setImageResource(mSpecialItems.get(position).getResourcesId());
                text.setText(mSpecialItems.get(position).getTitleResourcesId());
            }else {
                if ( convertView == null ) {
                    rootView = mInflater.inflate(R.layout.themeclub_mine_wallpaper_item, null);
                    holder = new ViewHolder();
                    holder.thumbnail = (ImageView) rootView.findViewById(R.id.thumbnail);
                    holder.resource = (ImageView) rootView.findViewById(R.id.resouce);
                    holder.selectedView = (RelativeLayout) rootView.findViewById(R.id.selected);
                    rootView.setTag(holder);
                }else {
                    rootView = convertView;
                    holder = (ViewHolder) convertView.getTag();
                }
                mGlide.load(wallpapers.get(position-mSpecialItems.size()).path).into(holder.thumbnail);

                if (mSelectedWallpaperId == (position-mSpecialItems.size())) {
                    holder.selectedView.setVisibility(View.VISIBLE);
                } else {
                    holder.selectedView.setVisibility(View.GONE);
                }
            }
            return rootView;
        }

        class ViewHolder{
            public ImageView thumbnail;
            public ImageView resource;
            public RelativeLayout selectedView;
        }
    }

    class LoadWallpaperTask extends AsyncTask{
        @Override
        protected void onPreExecute() {
            showProgress();
        }

        @Override
        protected void onPostExecute(Object o) {
            mMineWallpaperAdapter.notifyDataSetChanged();
            closeProgress();
        }

        @Override
        protected Object doInBackground(Object[] params) {
            if(isCancelled()) {
                return null;
            }
            initData();
            return null;
        }
    }

    public LoadWallpaperTask getTask() {
        if(mLoadTask == null) {
            return new LoadWallpaperTask();
        }
        return mLoadTask;
    }

}