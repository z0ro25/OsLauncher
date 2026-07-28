package com.amz.ios.launcher.widget.configure;

import android.appwidget.AppWidgetManager;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.amz.ios.launcher.widget.widgetprovider.PictureAppWidgetProvider;
import com.theartofdev.edmodo.cropper.CropImageActivity;
import com.theartofdev.edmodo.cropper.CropImageOptions;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;

import static com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_EXTRA_RESULT;

public class PictureAppWidgetProviderConfigureActivity extends AppCompatActivity {

    SharedPreferences mDefaultPreference;
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public static final int CROP_ACTIVITY_REQUEST_CODE = 203;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDefaultPreference = PreferenceManager.getDefaultSharedPreferences(this);
        setResult(RESULT_CANCELED);
        Bundle bundle = getIntent().getExtras();

        if (bundle != null){
            mAppWidgetId = bundle.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
         }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID){
            finish();
            return;
        }

        startCropActivity("appwidget_" + mAppWidgetId);
    }

    public void startCropActivity(String str){

        Uri uri;

        try {
            uri = Uri.fromFile(
                    new File(
                            new ContextWrapper(this).getDir("image", 0),
                            str.replace("/", "_") + ".jpg"
                    )
            );
        } catch (Throwable unused) {
            uri = null;
        }

        CropImageOptions options = new CropImageOptions();
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        options.fixAspectRatio = true;
        options.cropShape = CropImageView.CropShape.OVAL;
        options.activityTitle = "My Photos";
        options.outputCompressFormat = Bitmap.CompressFormat.JPEG;
        options.guidelines = CropImageView.Guidelines.ON;
        options.outputRequestWidth = 0;
        options.outputRequestHeight = 0;
        options.outputCompressQuality = 100;
        options.outputUri = uri;
        options.validate();

        Bundle bundle = new Bundle();
        bundle.putParcelable("CROP_IMAGE_EXTRA_SOURCE", null);
        bundle.putParcelable("CROP_IMAGE_EXTRA_OPTIONS", options);

        Intent intent = new Intent(this, CropImageActivity.class);
        intent.putExtra("CROP_IMAGE_EXTRA_BUNDLE", bundle);

        startActivityForResult(
                intent,
                CROP_ACTIVITY_REQUEST_CODE
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CROP_ACTIVITY_REQUEST_CODE && data != null){
            Uri uri;
            uri = data.getParcelableExtra(CROP_IMAGE_EXTRA_RESULT);
            if (uri == null) return;
            String path = uri.toString();
            if(TextUtils.isEmpty(path)) return;

            SharedPreferences.Editor editor = mDefaultPreference.edit();
            editor.putString("appwidget_" + mAppWidgetId, path);
            if (!editor.commit()) editor.apply();
            PictureAppWidgetProvider.updateWidget(
                    this,
                    AppWidgetManager.getInstance(this),
                    mAppWidgetId
            );

            Intent intent = new Intent();
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, intent);
        }

        finish();
    }

}
