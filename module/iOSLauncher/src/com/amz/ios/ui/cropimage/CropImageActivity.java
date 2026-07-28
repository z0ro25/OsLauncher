package com.ios.ui.cropimage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.amz.ios.launcher.R;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageOptions;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;

import static com.theartofdev.edmodo.cropper.CropImage.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE;
import static com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE;
import static com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_EXTRA_BUNDLE;
import static com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_EXTRA_OPTIONS;
import static com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_EXTRA_RESULT;
import static com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_EXTRA_SOURCE;
import static com.theartofdev.edmodo.cropper.CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE;
import static com.theartofdev.edmodo.cropper.CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE;

public class CropImageActivity extends AppCompatActivity implements View.OnClickListener, CropImageView.OnCropImageCompleteListener, CropImageView.OnSetImageUriCompleteListener{

    CropImageView mCropImageView;
    CropImageOptions mCropImageOptions;
    Uri mUri;
    View mBackBtn;
    View mOkBtn;
    View mTitleTV;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crop_image_activity);
        B(findViewById(R.id.root_layout),true);
        setUpView();
        config(savedInstanceState);
        setListeners();
    }

    void setUpView(){
        mCropImageView = findViewById(R.id.cropImageView);
        mBackBtn = findViewById(R.id.action_back);
        mOkBtn = findViewById(R.id.ok_crop_button);
        mTitleTV = findViewById(R.id.action_bar_label);
    }

    void setListeners(){
        mBackBtn.setOnClickListener(this);
        mOkBtn.setOnClickListener(this);
    }

    void config(Bundle bundle){
        Bundle bundleExtra = getIntent().getBundleExtra(CROP_IMAGE_EXTRA_BUNDLE);
        mUri = bundleExtra.getParcelable(CROP_IMAGE_EXTRA_SOURCE);
        mCropImageOptions = (CropImageOptions) bundleExtra.getParcelable(CROP_IMAGE_EXTRA_OPTIONS);
        if (bundle == null){
            if (mUri == null || mUri.equals(Uri.EMPTY)){
                if (CropImage.isExplicitCameraPermissionRequired(this)){
                    ActivityCompat.requestPermissions(
                        this,
                            new String[]{"android.permission.CAMERA"},
                            CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE
                    );
                }
                else {
                    startActivityForResult(
                            CropImage.getPickImageChooserIntent(
                                    this,
                                    "My Photo",
                                    true,
                                    true
                            ),
                            PICK_IMAGE_CHOOSER_REQUEST_CODE
                    );
                }
            }
            else if (CropImage.isReadExternalStoragePermissionsRequired(this,mUri)){
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{"android.permission.READ_EXTERNAL_STORAGE"},
                        PICK_IMAGE_PERMISSIONS_REQUEST_CODE
                );
            }
            else {
                mCropImageView.setImageUriAsync(mUri);
            }
        }
    }

    public final void B(View view, boolean z) {
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(z ? 12290 : 4098);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mOkBtn){
            if (mCropImageOptions.noOutputImage){
                crop(
                        null,
                        null,
                        1
                );
            }

            Uri uri = mCropImageOptions.outputUri;

            if (uri == null || uri.equals(Uri.EMPTY)){
                try {
                    Bitmap.CompressFormat compressFormat = this.mCropImageOptions.outputCompressFormat;
                    uri = Uri.fromFile(File.createTempFile("cropped", compressFormat == Bitmap.CompressFormat.JPEG ? ".jpg" : compressFormat == Bitmap.CompressFormat.PNG ? ".png" : ".webp", getCacheDir()));
                }
                catch (IOException e){
                    throw new RuntimeException("Failed to create temp file for output image", e);
                }

            }

            if (mCropImageView.getOnCropCompleteListener() == null){
                throw new IllegalArgumentException("mOnCropImageCompleteListener is not set");
            }

            mCropImageView.startCropWorkerTask(
                    mCropImageOptions.outputRequestWidth,
                    mCropImageOptions.outputRequestHeight,
                    mCropImageOptions.outputRequestSizeOptions,
                    uri,
                    mCropImageOptions.outputCompressFormat,
                    mCropImageOptions.outputCompressQuality
            );
        }
        else if (v == mBackBtn){
            cancel();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cancel();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        String action;
        if (requestCode == PICK_IMAGE_CHOOSER_REQUEST_CODE){
            if (resultCode == RESULT_CANCELED) cancel();
            else if (resultCode == RESULT_OK){
                mUri = ((intent == null || intent.getData() == null || ((action = intent.getAction()) != null && action.equals("android.media.action.IMAGE_CAPTURE"))) || intent.getData() == null) ? CropImage.getCaptureImageOutputUri(this) : intent.getData();
                if (CropImage.isReadExternalStoragePermissionsRequired(this,mUri)){
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{"android.permission.READ_EXTERNAL_STORAGE"},
                            PICK_IMAGE_PERMISSIONS_REQUEST_CODE
                    );
                }
                else {
                    mCropImageView.setImageUriAsync(mUri);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PICK_IMAGE_PERMISSIONS_REQUEST_CODE){
            if (mUri == null || grantResults.length <= 0 || grantResults[0] != 0){
                cancel();
            }
            else
                mCropImageView.setImageUriAsync(mUri);
        }
        if (requestCode == CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE){
            startActivityForResult(
                    CropImage.getPickImageChooserIntent(
                            this,
                            "My Photos",
                            true,
                            true
                    ),
                    PICK_IMAGE_CHOOSER_REQUEST_CODE
            );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCropImageView.setOnSetImageUriCompleteListener(this);
        mCropImageView.setOnCropImageCompleteListener(this);
    }

    public void cancel(){
        setResult(RESULT_CANCELED);
        finish();
    }

    public void crop(Uri uri, Exception e, int sampleSize){

        CropImage.ActivityResult result = new CropImage.ActivityResult(
                mCropImageView.getImageUri(),
                uri,
                e,
                mCropImageView.getCropPoints(),
                mCropImageView.getCropRect(),
                mCropImageView.getRotatedDegrees(),
                mCropImageView.getWholeImageRect(),
                sampleSize
        );

        Intent intent = new Intent();
        intent.putExtras(getIntent());
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_RESULT,result);
        setResult(e == null ? RESULT_OK : 204);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCropImageView.setOnSetImageUriCompleteListener(null);
        mCropImageView.setOnCropImageCompleteListener(null);
    }

    /** Crop Image Uri Complete Listener **/

    @Override
    public void onSetImageUriComplete(CropImageView view, Uri uri, Exception error) {

    }

    /** Crop Image Complete Listener **/

    @Override
    public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {
        Log.e("Result","Complete");
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putParcelable(CROP_IMAGE_EXTRA_RESULT,result.getUri());
        intent.putExtras(bundle);
        setResult(RESULT_OK,intent);
        finish();
    }

}
