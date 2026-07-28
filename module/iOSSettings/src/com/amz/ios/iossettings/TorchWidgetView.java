package com.amz.ios.iossettings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.amz.ios.ioslite.common.ContextHelper;
import com.amz.ios.ioslite.common.setting.IOSSettings;
import com.amz.ios.ioslite.common.util.FunctionUtil;
import com.amz.ios.ioslite.common.util.Sputil;
import com.amz.ios.launcher.views.CustomTextView;
import com.ios.ioslite.odm.R;

import static android.hardware.camera2.CameraManager.TorchCallback;
import android.os.BatteryManager;

/**
 * Torch Widget View;
 */
public class TorchWidgetView extends FrameLayout implements View.OnClickListener {
    private Context mContext;
    private final String NOTIFICATION_LIGHT_STATES = "notification_follow_up_status";
    private final String TORCH_LIGHT_STATES = "torch_widget_view_status";
    private final String SETTINGS_LIGHT_STATES = "settings_follow_up_status";
    private String lightKey = "isLight";
    public CustomTextView mIconName;
    public View mIconViewContainer;
    public ImageView mTorchIcon;

    private int mLabelSize;
    private int mLabelColor;
    private int mIconViewSize;
    private int mIconViewTextPadding;

    private boolean mIsTorchOpen;
    private FunctionUtil mFunctionUtils;
    private final String fileName = "torchfile";
    private final String SELF = "self";


    private Drawable mTorchNormalDrawable;
    private Drawable mTorchOpenDrawable;
    @RequiresApi(Build.VERSION_CODES.M)
    private CameraManager mCameraManager;
    @RequiresApi(Build.VERSION_CODES.M)
    private TorchCallback mTorchCallback;
    private long mLastTime;

    public TorchWidgetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;

        ContentResolver resolver = context.getContentResolver();
        int defaultLabelSize = mContext.getResources().getDimensionPixelSize(R.dimen.torch_widget_text_default_size);
        int defaultIconViewSize = mContext.getResources().getDimensionPixelSize(R.dimen.torch_widget_icon_default_size);
        int defaultIconTextPadding = mContext.getResources().getDimensionPixelSize(R.dimen.torch_widget_icon_default_padding);

        mLabelSize = IOSSettings.getInt(resolver, IOSSettings.Launcher.LAUNCHER_APP_LABEL_SIZE_PX, defaultLabelSize);
        mLabelColor = IOSSettings.getInt(resolver, IOSSettings.Launcher.LAUNCHER_WORKSPACE_ICON_LABEL_COLOR);
        mIconViewSize = IOSSettings.getInt(resolver, IOSSettings.Launcher.LAUNCHER_APP_ICON_SIZE_PX, defaultIconViewSize);
        mIconViewTextPadding = IOSSettings.getInt(resolver, IOSSettings.Launcher.LAUNCHER_APP_ICON_LABEL_PADDING_PX, defaultIconTextPadding);

        initTorchDrawable(context);
    }

    private void initTorchDrawable(Context context) {
        String themePakcageName = IOSSettings.getString(context.getContentResolver(), IOSSettings.Launcher.LAUNCHER_THEME_PACKAGE, context.getPackageName());

        if (!context.getPackageName().equals(themePakcageName)) {
            mTorchNormalDrawable = readDrawableByResName(context, "ic_app_torch", themePakcageName);
            mTorchOpenDrawable = readDrawableByResName(context, "ic_app_torch_on", themePakcageName);
        }

        if (mTorchOpenDrawable == null || mTorchNormalDrawable == null) {
            mTorchNormalDrawable = context.getResources().getDrawable(R.drawable.ic_app_torch);
            mTorchOpenDrawable = context.getResources().getDrawable(R.drawable.ic_app_torch_on);
        }
    }


    public TorchWidgetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TorchWidgetView(Context context) {
        this(context, null);
    }


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("广播", TorchWidgetView.this.toString() + ":" + intent.getStringExtra(SELF) + ":" + TorchWidgetView.this.toString().equals(intent.getStringExtra(SELF)));
            if (TorchWidgetView.this.toString().equals(intent.getStringExtra(SELF))) {
                return;
            }
            String action = intent.getAction();
            mIsTorchOpen = intent.getBooleanExtra(lightKey, false);
            if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
                // Locale changed ,update icon text string;
                mIconName.setText(mContext.getString(R.string.torch_widget_name));
            } else {
                // Torch light state change , synchronize it here;
                updateWidgetView();
            }
        }
    };

    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {


        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(IOSSettings.getUriFor(IOSSettings.Launcher.LAUNCHER_WORKSPACE_ICON_LABEL_COLOR))) {
                mLabelColor = IOSSettings.getInt(mContext.getContentResolver(), IOSSettings.Launcher.LAUNCHER_WORKSPACE_ICON_LABEL_COLOR);
                mIconName.setTextColor(mLabelColor);

            } else if (uri.equals(IOSSettings.getUriFor(IOSSettings.Launcher.LAUNCHER_APP_LABEL_SIZE_PX))) {
                mLabelSize = IOSSettings.getInt(mContext.getContentResolver(), IOSSettings.Launcher.LAUNCHER_APP_LABEL_SIZE_PX, 1);
                updateTextSize();
            }
        }

    };

    private void updateTextSize() {
        mIconName.setTextSize(TypedValue.COMPLEX_UNIT_PX, mLabelSize);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mIconViewContainer = findViewById(R.id.iconViewContainer);
        mTorchIcon = (ImageView) findViewById(R.id.torchIcon);
        mIconName = (CustomTextView) findViewById(R.id.widgetName);
        mIconName.setTextColor(mLabelColor);
        updateTextSize();

        LinearLayout.LayoutParams llp;
        llp = (LinearLayout.LayoutParams) mTorchIcon.getLayoutParams();
        llp.height = mIconViewSize;
        llp.width = mIconViewSize;
        mTorchIcon.setLayoutParams(llp);


        llp = (LinearLayout.LayoutParams) mIconName.getLayoutParams();
        llp.topMargin = mIconViewTextPadding;
        mIconName.setLayoutParams(llp);

        mIsTorchOpen = isTorchOpen();
        updateWidgetView();
        mLastTime = System.currentTimeMillis();
        setOnClickListener(this);
        mFunctionUtils = FunctionUtil.getInstance(mContext);
        mLightThread = new LightThread();
    }

    @Override
    public void onClick(View v) {
        if (!mFunctionUtils.requestCamera((Activity) mContext)) {
            return;
        }
        /*/ios.cgf.0904 fix bug 0046098
        switchTorch(!mIsTorchOpen);
        new Thread(mLightThread).start();
        /*/
        if (getBatteryLevel() <= MIN_BATTERY_LEVEL) {
            Toast.makeText(mContext,mContext.getResources().getString(R.string.low_battery_flashlight_canot_use),Toast.LENGTH_SHORT).show();
        }else{
            switchTorch(!mIsTorchOpen);
            new Thread(mLightThread).start();
        }
        //*/
    }

    private LightThread mLightThread;

    private class LightThread implements Runnable {

        @Override
        public void run() {
            synchronized (this) {
                if (mIsTorchOpen == true) {
                    mFunctionUtils.openTorch();
                } else {
                    mFunctionUtils.closeTorch();
                }
                Intent intent = new Intent(SETTINGS_LIGHT_STATES);
                intent.putExtra(lightKey, mIsTorchOpen);
                mContext.sendBroadcast(intent);
                Intent intent1 = new Intent(TORCH_LIGHT_STATES);
                intent1.putExtra(SELF, TorchWidgetView.this.toString());
                intent1.putExtra(lightKey, mIsTorchOpen);
                mContext.sendBroadcast(intent1);
                Sputil.saveTorchState(mContext, mIsTorchOpen, lightKey, fileName);
            }
        }
    }


    private boolean isTorchOpen() {
        return Sputil.getTorchState(mContext, lightKey, fileName);
    }

    private void switchTorch(boolean open) {
        mIsTorchOpen = open;
        updateWidgetView();
    }

    private void updateWidgetView() {
        if (mIsTorchOpen) {
            mTorchIcon.setImageDrawable(mTorchOpenDrawable);
        } else {
            mTorchIcon.setImageDrawable(mTorchNormalDrawable);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TORCH_LIGHT_STATES);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
        mContext.getContentResolver().registerContentObserver(IOSSettings.getUriFor(IOSSettings.Launcher.LAUNCHER_WORKSPACE_ICON_LABEL_COLOR), false, mContentObserver);
        mContext.getContentResolver().registerContentObserver(IOSSettings.getUriFor(IOSSettings.Launcher.LAUNCHER_APP_LABEL_SIZE_PX), false, mContentObserver);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mCameraManager == null) {
                mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            }
            if (mTorchCallback == null) {
                mTorchCallback = new TorchCallback() {
                    @Override
                    public void onTorchModeChanged(@NonNull String cameraId, final boolean enabled) {
                        if (System.currentTimeMillis() - mLastTime > 1000) {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    mIsTorchOpen = enabled;
                                    Sputil.saveTorchState(mContext, mIsTorchOpen, lightKey, fileName);
                                    updateWidgetView();
                                }
                            });
                        }
                    }
                };
            }
            mCameraManager.registerTorchCallback(mTorchCallback, new Handler());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mContext.unregisterReceiver(mReceiver);
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
        super.onDetachedFromWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mCameraManager.unregisterTorchCallback(mTorchCallback);
        }
    }

    private Drawable readDrawableByResName(Context context, String resName, String packageName) {
        Context themeContext;
        Resources res;
        try {
            themeContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            res = themeContext.getResources();
        } catch (Exception e) {
            return null;
        }
        int identifier = res.getIdentifier(resName, "drawable", packageName);
        if (identifier > 0) {
            return res.getDrawable(identifier);
        }
        return null;
    }

    //*/ios.cgf.0904 fix.bug 0046098
    private int getBatteryLevel(){
        IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = ContextHelper.registerReceiver(getContext(),null, batteryFilter);
        if (intent != null) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            return (level * 100 / scale);
        } else {
            return 100;
        }
    }
    private final int MIN_BATTERY_LEVEL = 15;
    //*/
}

