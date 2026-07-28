package com.amz.ios.themeclub.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.ioslite.common.debug.DebugLog;
import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;
import com.amz.ios.themeclub.intertfaces.IProgressView;
import com.amz.ios.themeclub.presenter.WallpaperPresenter;
import com.amz.ios.themeclub.util.AppUtils;
import com.amz.ios.themeclub.util.ImageLoader;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.io.InputStream;


/**
 * Created by server on 16-11-23.
 */

public class CropImageActivity extends CommonAppCompatActivity implements View.OnClickListener {
    private final String TAG = "CropImageActivity";

    private static final int FIXED = 1;
    private static final int SCROLLABLE = 2;

    private CropImageView mCropImageView;
    private Button setAsWallpaperBtn;
    private Uri imageUri;
    private Intent mIntent;
    private View fixed;
    private View fixedImage;
    private View scrollable;
    private View scrollableImage;
    private View fixedText;
    private View scrollableText;
    private int aspectRatioType = FIXED;
    private int imageHeight;
    private int imageWidth;
    RelativeLayout mProgress;
    private Bitmap mBitmap;
    private int mSelectedBitmapId;

    private IProgressView mIProgressView = new IProgressView() {
        @Override
        public void showProgress() {
            if (mProgress != null) {
                mProgress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void closeProgress() {
            if (mProgress != null) {
                mProgress.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.themeclub_activity_crop_image_activity);

        mCropImageView = (CropImageView) findViewById(R.id.cropImageView);
        setAsWallpaperBtn = (Button) findViewById(R.id.set_as_wallpaper);
        fixed = (RelativeLayout) findViewById(R.id.fixed);
        scrollable = (RelativeLayout) findViewById(R.id.scrollable);
        fixed.setOnClickListener(this);
        scrollable.setOnClickListener(this);
        fixedImage = (ImageView) findViewById(R.id.fixed_image);
        scrollableImage = (ImageView) findViewById(R.id.scrollable_image);
        fixedText = (CustomTextView) findViewById(R.id.fixed_text);
        scrollableText = (CustomTextView) findViewById(R.id.scrollable_text);
        setAsWallpaperBtn.setOnClickListener(this);
        mProgress = (RelativeLayout) findViewById(R.id.progress_set);

        Log.d(TAG, "onCreate: " + AppUtils.getLCD(this, false));
        Log.d(TAG, "onCreate: getScreenHeight=" + AppUtils.getScreenHeight(this) + "   getScreenWidth()=" + AppUtils.getScreenWidth(this));
        Log.d(TAG, "onCreate: getStatusHeight()=" + AppUtils.getStatusHeight(this));

        mIntent = getIntent();
        if (mIntent != null) {

            mSelectedBitmapId = mIntent.getIntExtra("selectedId", -1);
            if (mBitmap == null) {
                if (mIntent.hasExtra("path")) {
                    if (mIntent.getBooleanExtra("isUriOrPath", false)) {
                        imageUri = Uri.parse(mIntent.getStringExtra("path"));
                        LoadBitmapTask mLoadBitmapTask = new LoadBitmapTask();
                        mLoadBitmapTask.execute(imageUri);
                    } else {
                        loadWallPaper();
                    }
                } else if (mIntent.hasExtra("bitmapurl")) {
                    Glide.with(CropImageActivity.this)
                            .asBitmap()
                            .load(mIntent.getStringExtra("bitmapurl"))
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                                    mBitmap = bitmap;
                                    mCropImageView.setImageBitmap(mBitmap);
                                    mCropImageView.setGuidelines(CropImageView.Guidelines.OFF);
                                    mCropImageView.setAutoZoomEnabled(false);
                                    imageHeight = mBitmap.getHeight();
                                    imageWidth = mBitmap.getWidth();
                                    reset();
                                    toggleAspectRatio(aspectRatioType);
                                    initToolbar();
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable drawable) {

                                }
                            });
                }
                ;
            }

        }
    }


    private void loadWallPaper() {
        String wallpaperPath = getIntent().getStringExtra("path");
        if (wallpaperPath.startsWith("file:///android_asset/wallpapers/")) {
            String[] filename = wallpaperPath.split("/");
            AssetManager assetManager = getAssets();
            InputStream is = null;
            try {
                is = assetManager.open("wallpapers/" + filename[5]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (is != null) {
                mBitmap = BitmapFactory.decodeStream(is);
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            mBitmap = BitmapFactory.decodeFile(wallpaperPath);
        }
        if (mBitmap == null) {
            Toast.makeText(CropImageActivity.this, R.string.themeclub_picture_not_found, Toast.LENGTH_LONG).show();
            finish();
        } else {
            mCropImageView.setImageBitmap(mBitmap);
            mCropImageView.setGuidelines(CropImageView.Guidelines.OFF);
            mCropImageView.setAutoZoomEnabled(false);
            imageHeight = mBitmap.getHeight();
            imageWidth = mBitmap.getWidth();
            reset();
            toggleAspectRatio(aspectRatioType);
            initToolbar();
        }
    }

    private class LoadBitmapTask extends AsyncTask<Uri, Void, Bitmap> {
        int mBitmapSize;
        Context mContext;
        Rect mOriginalBounds;
        int mOrientation;

        public LoadBitmapTask() {
            mBitmapSize = getScreenImageSize();
            mContext = getApplicationContext();
            mOriginalBounds = new Rect();
            mOrientation = 0;
        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            Uri uri = params[0];
            mBitmap = ImageLoader.loadConstrainedBitmap(uri, mContext, mBitmapSize,
                    mOriginalBounds, false);
            return mBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result == null) {
                Toast.makeText(CropImageActivity.this, R.string.themeclub_picture_not_found, Toast.LENGTH_LONG).show();
                finish();
            } else {
                mCropImageView.setImageBitmap(result);
                mCropImageView.setGuidelines(CropImageView.Guidelines.OFF);
                mCropImageView.setAutoZoomEnabled(false);
                imageHeight = result.getHeight();
                imageWidth = result.getWidth();
                reset();
                toggleAspectRatio(aspectRatioType);
                initToolbar();
            }
        }
    }

    private int getScreenImageSize() {
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        return (int) Math.max(outMetrics.heightPixels, outMetrics.widthPixels);
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.crop_iamge_toolbar);
        toolbar.setNavigationIcon(R.drawable.themeclub_selection_white_back);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.themeclub_menu_wallpaper_crop, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected:item.getItemId()=" + item.getItemId());
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.reset) {
            reset();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.set_as_wallpaper) {
            if (mBitmap == null) {
                Toast.makeText(CropImageActivity.this, R.string.themeclub_picture_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            DebugLog.w(TAG, "===================set as wallpaper click:" + mBitmap);
            new WallpaperPresenter(this, aspectRatioType).applyWallpaper(
                    this,
                    mIProgressView,
                    mCropImageView.getCroppedImage(),
                    null,
                    LayoutInflater.from(this).inflate(R.layout.themeclub_activity_crop_image_activity, null),
                    mSelectedBitmapId);

        } else if (v.getId() == R.id.fixed) {
            toggleAspectRatio(FIXED);
        } else if (v.getId() == R.id.scrollable) {
            toggleAspectRatio(SCROLLABLE);
        }
    }

    private void toggleAspectRatio(int aspect) {
        if (aspect == FIXED) {
            fixedImage.setSelected(true);
            fixedText.setSelected(true);
            scrollableImage.setSelected(false);
            scrollableText.setSelected(false);
            aspectRatioType = FIXED;
            mCropImageView.setAspectRatio(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight());
        } else if (aspect == SCROLLABLE) {
            fixedImage.setSelected(false);
            fixedText.setSelected(false);
            scrollableImage.setSelected(true);
            scrollableText.setSelected(true);
            aspectRatioType = SCROLLABLE;
            mCropImageView.setAspectRatio(getWindowManager().getDefaultDisplay().getWidth() * 2, getWindowManager().getDefaultDisplay().getHeight());
        }
    }

    private void reset() {
        mCropImageView.setCropRect(new Rect(0, 0, imageWidth, imageHeight));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}