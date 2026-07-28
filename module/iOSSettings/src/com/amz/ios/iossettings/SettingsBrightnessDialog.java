package com.amz.ios.iossettings;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.amz.ios.ioslite.common.util.FunctionUtil;
import com.ios.ioslite.odm.R;

public class SettingsBrightnessDialog extends Dialog {
    private Context mContext;
    private SeekBar mBrightnessSeekBar;
    private LayoutParams layoutParams;
    private LinearLayout mBrightnessLayout;
    private int mMinBright;

    public SettingsBrightnessDialog(Context context) {
        super(context);
        mContext = context.getApplicationContext();
        mMinBright = FunctionUtil.getInstance(mContext).getMinBright();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.settings_brightness_layout);
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();

        mBrightnessLayout = (LinearLayout) findViewById(R.id.brightness_layout);
        mBrightnessSeekBar = (SeekBar) findViewById(R.id.brightness_seekbar);
        layoutParams = mBrightnessLayout.getLayoutParams();
        layoutParams.width = width / 5 * 4;
        layoutParams.height = height / 10 * 1;
        mBrightnessLayout.setLayoutParams(layoutParams);
        mBrightnessSeekBar.setMax(255 - mMinBright);

        mBrightnessSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                try {
                    Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, progress + mMinBright);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        setLightProgress();
        super.onStart();
    }

    private void setLightProgress() {
        int currentBrightness = 0;
        currentBrightness = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1) - mMinBright;
        if (currentBrightness != -1) {
            mBrightnessSeekBar.setProgress(currentBrightness);
        } else {
            mBrightnessSeekBar.setProgress(100 - mMinBright);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setLightProgress();
        }
    }
}
