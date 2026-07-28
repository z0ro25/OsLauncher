package com.zhuoyi.security.batterysave;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ios.sc.clean.task.utils.CT_Utils;
import com.zhuoyi.security.batterysave.util.BS_BatteryStatsHelper;
import com.zhuoyi.security.batterysave.util.BS_PowerProfileUtil;
import com.zhuoyi.security.batterysave.util.BS_SettingsUtil;
import com.zhuoyi.security.batterysave.util.BS_Utils;
import com.zhuoyi.security.batterysave.views.BS_PrcProgressBar;
import com.zhuoyi.security.batterysave.views.BS_TitleBar;

import java.util.Set;

import static android.media.AudioManager.RINGER_MODE_NORMAL;
import static android.media.AudioManager.RINGER_MODE_VIBRATE;

public class BS_MainActivity extends Activity implements View.OnClickListener, BS_TitleBar.CallBack {
    private static final String TAG = "BS_MainActivity";

    private BS_PrcProgressBar prcProgressbar;
    private int chargeState = -1;//unset
    private final int UnchargeState = 0;
    private final int ChargingState = 1;
    private final int ChargedState = 2;
    private final int BatteryLevelChange = 3;
    private final int BatteryAppChange = 4;

    private int mBaterryLevel = -1; //unset
    //private float mBaterryUseRate = -1;//unset, 每毫秒走的电量
    private float mBaterryChargeRate = -1; //unset, 每毫秒充电的电量
    private long mBaterryChargeLeftTime = 0;//unset , 没充电时。剩余的时间
    private long mBaterryUseLeftTime;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UnchargeState:
                    if (chargeState != UnchargeState) {
                        chargeState = UnchargeState;
                        prcProgressbar.setChargeState(chargeState);
                        if (mBaterryUseLeftTime > 0) {
                            String leftTime = BS_Utils.getHourAndMinute((long) mBaterryUseLeftTime);
                            String leftDay = BS_Utils.getDay(mCtx, (long) mBaterryUseLeftTime);
                            prcProgressbar.setTime(leftTime, leftDay);

                        }
                    }
                    break;
                case ChargingState:
                    if (chargeState != ChargingState) {
                        chargeState = ChargingState;
                        prcProgressbar.setChargeState(chargeState);
                        if (mBaterryUseLeftTime > 0) {
                            String chargeTime = BS_Utils.getHourAndMinute((long) mBaterryChargeLeftTime);
                            prcProgressbar.setTimeValue(chargeTime);
                        }
                    }
                    break;
                case ChargedState:
                    if (chargeState != ChargedState) {
                        chargeState = ChargedState;
                        prcProgressbar.setChargeState(chargeState);
                    }
                    break;
                case BatteryLevelChange:
                    prcProgressbar.setProcess(mBaterryLevel);
                    if (chargeState == UnchargeState) {
                        String leftTime = BS_Utils.getHourAndMinute((long) mBaterryUseLeftTime);
                        String leftDay = BS_Utils.getDay(mCtx, (long) mBaterryUseLeftTime);
                        //BS_FileUtils.initData("handler>>"+leftDay+"=="+leftTime+"=="+mBaterryUseLeftTime);
                        prcProgressbar.setTime(leftTime, leftDay);
                    } else if (chargeState == ChargingState) {
                        String chargeTime = BS_Utils.getHourAndMinute((long) mBaterryChargeLeftTime);
                        prcProgressbar.setTimeValue(chargeTime);
                    }
                    break;
                case BatteryAppChange:
                    if (mAppcount > 0) {
                        mAppcount--;
                        batteryAppNothingText.setText(String.valueOf(mAppcount));
                    } else {
                        batteryAppbtn.setVisibility(View.VISIBLE);
                        batteryAppNothingBtn.setVisibility(View.INVISIBLE);
                       /* int batteryLevel = (int) Math.min((mBaterryLevel + mLevelOffset), 100);
                        //Toast.makeText(mCtx,mBaterryLevel+"(mBaterryLevel)+"+mLevelOffset+"(mLevelOffset)"
                                //+"="+batteryLevel+"(batteryLevel)", Toast.LENGTH_SHORT).show();
                        prcProgressbar.setProcess(batteryLevel);*/
                        if (chargeState == UnchargeState) {
                            mBaterryUseLeftTime += mTimeOffset;
                            BS_Utils.setUnchargeLeftTime(mCtx,mBaterryUseLeftTime);
                            String leftTime = BS_Utils.getHourAndMinute((long) mBaterryUseLeftTime);
                            String leftDay = BS_Utils.getDay(mCtx, (long) mBaterryUseLeftTime);
                            prcProgressbar.setTime(leftTime, leftDay);
                            //Toast.makeText(mCtx,leftTime+"(leftTime):"+leftDay+"(leftDay)", Toast.LENGTH_SHORT).show();
                        } else if (chargeState == ChargingState) {
                            if ((mBaterryChargeLeftTime - mLevelOffset * 0.5) > 0) {

                                mBaterryChargeLeftTime = (long) Math.max((mBaterryChargeLeftTime - mLevelOffset * 0.5),
                                        mBaterryChargeLeftTime - 30 * 60 * 1000);
                                mBaterryChargeLeftTime = Math.min(mBaterryChargeLeftTime, 100);
                            } else {

                            }
                            String chargeTime = BS_Utils.getHourAndMinute((long) mBaterryChargeLeftTime);
                            prcProgressbar.setTimeValue(chargeTime);
                        }
                        batteryAppLl.setClickable(true);
                        belowButton.setText(R.string.bs_bottom_btn);
                        mCanClean = false;

                    }
                    break;

            }

        }
    };
    private ProgressBar loadingImage;
    private LinearLayout loadingLayout;
    private LinearLayout cardLayout;
    private Button belowButton;
    private Context mCtx;
    private BS_TitleBar titleBar;
    private ImageView batteryAppbtn;
    private FrameLayout batteryAppNothingBtn;
    private TextView batteryAppNothingText;
    private RelativeLayout root;
    //private FrameLayout batteryAppImgFl;
    private int mAppcount = 0;
    private LinearLayout batteryAppLl;
    private double mLevelOffset = 0;
    private double mTimeOffset = 0;
    private long mOnclickAppTime = 0;
    private boolean mCanClean = true;
    private BS_PowerProfileUtil powerProfile;

    private boolean mGpsStat = false;
    private boolean mSyncStat = false;
    private boolean mLightStat = false;
    private boolean mRotateStat = false;
    private boolean mOvertimeStat = false;
    private boolean mVibrateStat = false;
    private boolean mDataStat = false;
    private boolean mWifiapStat = false;
    private boolean mWifiStat = false;
    private boolean mBluetoothStat = false;
    private boolean mRingerStat = false;
    private boolean mTouchStat = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bs_activity_my_main);
        //String filePath = Environment.getExternalStorageDirectory()+"/SC_Test/";
        //Log.e("zengrui",filePath);
        //BS_FileUtils.initData("start");
        mCtx = getApplicationContext();
        powerProfile = new BS_PowerProfileUtil(mCtx);
        bindBatteryBroadcastRecever();

        titleBar = (BS_TitleBar) findViewById(R.id.BS_bar);
        //titleBar.setOnCallBack(this);
       /* BS_Utils.setImmersiveBars(this,titleBar);*/

        loadingImage = (ProgressBar) findViewById(R.id.bs_loading);
        startLoadingAnimation();

        prcProgressbar = (BS_PrcProgressBar) findViewById(R.id.arcProgressbar);
        if (chargeState != -1) {
            prcProgressbar.setChargeState(chargeState);
        }
        if (mBaterryLevel != -1) {
            prcProgressbar.setProcess(mBaterryLevel);
        }

        loadingLayout = (LinearLayout) findViewById(R.id.bs_checking);
        cardLayout = (LinearLayout) findViewById(R.id.card);
        belowButton = (Button) findViewById(R.id.bs_bellow_btn);

        batteryAppLl = (LinearLayout) findViewById(R.id.app_baterry);
        batteryAppbtn = (ImageView) findViewById(R.id.app_baterry_btn);
        batteryAppNothingBtn = (FrameLayout) findViewById(R.id.app_baterry_nothing_btn);
        batteryAppNothingText = (TextView) findViewById(R.id.app_baterry_nothing_text);

        batteryAppLl.setOnClickListener(this);
        findViewById(R.id.ranking_baterry).setOnClickListener(this);
        findViewById(R.id.switch_baterry).setOnClickListener(this);
        belowButton.setOnClickListener(this);

        root = (RelativeLayout) findViewById(R.id.activity_my_main);
        initData();

        //TEST
       /*
        powerProfile.getsPowerMap();
        double a = //powerProfile.getAveragePowerOrDefault("camera.avg",0);
                BS_BatteryStatsHelper.cpuPowerCalculator(mCtx);
        Log.e("zengrui", "-----------" + a);*/
    }

    private void initData() {
        mGpsStat = BS_SettingsUtil.gpsCheck(mCtx);
        mSyncStat = BS_SettingsUtil.isSyncSwitchOn(mCtx);
        mLightStat =BS_SettingsUtil.brightnessCheck(mCtx);
        mRotateStat = BS_SettingsUtil.rotationCheck(mCtx);
        mOvertimeStat = BS_SettingsUtil.timeCheck(mCtx);
        int audioMode  = BS_SettingsUtil.getRingerMode(mCtx);
        mVibrateStat = RINGER_MODE_VIBRATE == audioMode;
        mDataStat = BS_SettingsUtil.getMobileDataState(mCtx);
        mWifiapStat = BS_SettingsUtil.isWifiApEnabled(mCtx);
        mWifiStat = BS_SettingsUtil.wifiCheck(mCtx);
        mBluetoothStat = BS_SettingsUtil.bluetoothCheck();
        mRingerStat = RINGER_MODE_NORMAL == audioMode;
        mTouchStat = BS_SettingsUtil.vibrateCheck(mCtx);
    }

    long[] min = {
            31,
            6,
            12,
            1,
            2,
            1,
            5,
            37,
            31,
            12,
            1,
            1
    };

    @Override
    protected void onResume() {
        super.onResume();
        resetData();
        long spLeftTime = BS_Utils.getUnchargeLeftTime(mCtx);
        //BS_FileUtils.initData("onresume>>>"+spLeftTime+"==="+mBaterryUseLeftTime);
        if (mBaterryUseLeftTime != spLeftTime){
            mBaterryUseLeftTime = spLeftTime;
            mHandler.sendEmptyMessage(BatteryLevelChange);
        }


    }

    public void resetData() {
        long leftTime = BS_Utils.getUnchargeLeftTime(mCtx);
        boolean gspStat = BS_SettingsUtil.gpsCheck(mCtx);
        if (!(gspStat == mGpsStat)){
            mGpsStat = gspStat;
            if (gspStat){
                leftTime -= min[0]*60*1000;
                //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
            } else {
                 leftTime += min[0]*60*1000;
                //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
            }

        }
        boolean syncStat = BS_SettingsUtil.isSyncSwitchOn(mCtx);
        //Toast.makeText(mCtx,syncStat+"==="+mSyncStat,Toast.LENGTH_SHORT).show();
        if (!(syncStat == mSyncStat)) {
            mSyncStat = syncStat;
            if (syncStat){
                leftTime -= min[1]*60*1000;
                //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
            } else {
                leftTime += min[1]*60*1000;
                //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
            }
        }
        boolean lightStat = BS_SettingsUtil.brightnessCheck(mCtx);
        if (!(lightStat == mLightStat)) {
            mLightStat = lightStat;
            if (lightStat){
                leftTime -= min[2]*60*1000;
                //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
            } else {
                leftTime += min[2]*60*1000;
                //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
            }
        }

        boolean rotateStat = BS_SettingsUtil.rotationCheck(mCtx);
        //Log.e("zengrui",rotateStat+"==rotateStat=="+mRotateStat);
        if (!(rotateStat == mRotateStat)) {
            mRotateStat = rotateStat;
            if (rotateStat){
                leftTime -= min[3]*60*1000;
                //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
            } else {
                leftTime += min[3]*60*1000;
                //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
            }
        }
        boolean overtimeStat = BS_SettingsUtil.timeCheck(mCtx);
        if (!(overtimeStat == mOvertimeStat)) {
            mOvertimeStat = overtimeStat;
            if (overtimeStat){
                leftTime -= min[4]*60*1000;
                //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
            } else {
                leftTime += min[4]*60*1000;
                //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
            }
        }

        int audioMode  = BS_SettingsUtil.getRingerMode(mCtx);
        boolean vibrateStat = RINGER_MODE_VIBRATE == audioMode;
        //Log.e("zengrui",vibrateStat+"==vibrateStat=="+mVibrateStat);
        if (!(vibrateStat == mVibrateStat)) {
            mVibrateStat = vibrateStat;
            if (vibrateStat){
                leftTime -= min[5]*60*1000;
                //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
            } else {
                leftTime += min[5]*60*1000;
                //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
            }
        }

        boolean dataStat = BS_SettingsUtil.getMobileDataState(mCtx);
        if (!(dataStat == mDataStat)) {
            mDataStat = dataStat;
            if (dataStat){
                leftTime -= min[6]*60*1000;
                //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
            } else {
                leftTime += min[6]*60*1000;
                //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
            }
        }

        boolean wifiapStat = BS_SettingsUtil.isWifiApEnabled(mCtx);
        if (!(wifiapStat == mWifiapStat)) {
            mWifiapStat = wifiapStat;
            if (wifiapStat){
                leftTime -= min[7]*60*1000;
                //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
            } else {
                leftTime += min[7]*60*1000;
                //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
            }
        }

        boolean wifiStat = BS_SettingsUtil.wifiCheck(mCtx);
        if (!(wifiStat == mWifiStat)) {
            mWifiStat = wifiStat;
            if (wifiStat){
                leftTime -= min[8]*60*1000;
                //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
            } else {
                leftTime += min[8]*60*1000;
                //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
            }
        }

        boolean bluetoothStat = BS_SettingsUtil.bluetoothCheck();
        if (!(bluetoothStat == mBluetoothStat)) {
            mBluetoothStat = bluetoothStat;
            if (bluetoothStat){
                leftTime -= min[9]*60*1000;
                //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
            } else {
                leftTime += min[9]*60*1000;
                //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
            }
        }

        boolean ringerStat =  RINGER_MODE_NORMAL == audioMode;
        Log.e("zengrui",ringerStat+"==ringerStat=="+mRingerStat);
        if (!(ringerStat == mRingerStat)) {
            mRingerStat = ringerStat;
            if (ringerStat){
                leftTime -= min[10]*60*1000;
                //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
            } else {
                leftTime += min[10]*60*1000;
                //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
            }
        }

        boolean touchStat = BS_SettingsUtil.vibrateCheck(mCtx);
        if (!(touchStat == mTouchStat)) {
            mTouchStat = touchStat;
            if (ringerStat){
                leftTime -= min[11]*60*1000;
                //BS_FileUtils.initData("----"+info.getExplain()+"==="+leftTime);
            } else {
                leftTime += min[11]*60*1000;
                //BS_FileUtils.initData("+++++"+info.getExplain()+"==="+leftTime);
            }
        }

        BS_Utils.setUnchargeLeftTime(mCtx,leftTime);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.app_baterry) {
            if (mCanClean) {
                batteryAppLl.setClickable(false);

                BS_Utils.setOnClickAppTime(mCtx, System.currentTimeMillis());
                BS_Utils.setOnClickAppOffsetLevel(mCtx, mLevelOffset + "");
                BS_Utils.setOnClickAppOffsetTime(mCtx, mTimeOffset + "");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        CT_Utils.killBgCleanTask(BS_MainActivity.this, null);
                        while (mAppcount > 0) {
                            SystemClock.sleep(200);
                            mHandler.sendEmptyMessage(BatteryAppChange);
                        }

                    }
                }).start();
            } else {
                Toast.makeText(mCtx, getString(R.string.bs_clean_tost), Toast.LENGTH_SHORT).show();
            }

        } else if (id == R.id.ranking_baterry) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
                intent.setPackage("com.android.settings");
                startActivity(intent);
            }else {
                Intent intent = new Intent(this, BS_RankingActivity.class);
                startActivity(intent);
            }
        } else if (id == R.id.bs_bellow_btn) {
            String wait = getResources().getString(R.string.bs_bellow_btn);
            String save = getResources().getString(R.string.bs_bottom_btn_save);
            String finish = getResources().getString(R.string.bs_bottom_btn);
            String text = belowButton.getText().toString();
            if (save.equals(text)) {
                batteryAppLl.setClickable(false);

                BS_Utils.setOnClickAppTime(mCtx, System.currentTimeMillis());
                BS_Utils.setOnClickAppOffsetLevel(mCtx, mLevelOffset + "");
                BS_Utils.setOnClickAppOffsetTime(mCtx, mTimeOffset + "");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        CT_Utils.killBgCleanTask(BS_MainActivity.this, null);
                        while (mAppcount > 0) {
                            SystemClock.sleep(1000);
                            mHandler.sendEmptyMessage(BatteryAppChange);
                        }

                    }
                }).start();
            } else if (finish.equals(text)) {
                finish();
            }
        } else if (id == R.id.switch_baterry) {
            Intent sintent = new Intent(this, BS_SettingsActivity.class);
            startActivity(sintent);
        }
    }

    @Override
    protected void onDestroy() {
        clearBatteryBroadcastRecvier();
        super.onDestroy();
    }

    void bindBatteryBroadcastRecever() {
        if (null != mBatterBroadcastRecevier) {
            IntentFilter mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerReceiver(mBatterBroadcastRecevier, mIntentFilter,RECEIVER_EXPORTED);
            }else registerReceiver(mBatterBroadcastRecevier, mIntentFilter);
        }
    }

    void clearBatteryBroadcastRecvier() {
        if (null != mBatterBroadcastRecevier) {
            try {
                unregisterReceiver(mBatterBroadcastRecevier);
            } catch (Exception err) {
                Log.e(TAG, "MainActivity" + "::::" + err.toString());
            }

        }
    }

    private void startLoadingAnimation() {
        RotateAnimation myRotateAnimation =
                new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        myRotateAnimation.setRepeatCount(3);
        myRotateAnimation.setDuration(1000);
        myRotateAnimation.setInterpolator(new LinearInterpolator());
        loadingImage.setAnimation(myRotateAnimation);
        myRotateAnimation.setAnimationListener(new Animation.AnimationListener() { //设置动画监听事件
            @Override
            public void onAnimationStart(Animation arg0) {


            }

            @Override
            public void onAnimationRepeat(Animation arg0) {


            }

            @Override
            public void onAnimationEnd(Animation arg0) {
                if (chargeState == -1) {
                    startLoadingAnimation();
                } else {
                    loadingLayout.setVisibility(View.GONE);
                    cardLayout.setVisibility(View.VISIBLE);

                    mAppcount = (int) Math.ceil(Math.random() * 10);//(1~10)
                    mLevelOffset = Math.min(mAppcount * (0.2 + Math.random()), 5);
                    mTimeOffset = mLevelOffset * 3 * 60 * 1000;
                    mOnclickAppTime = BS_Utils.getOnClickAppTime(mCtx);
                    if (mOnclickAppTime < System.currentTimeMillis() - 60 * 1000) {
                        mCanClean = true;
                       /* BS_Utils.setOnClickAppTime(mCtx, System.currentTimeMillis());
                        BS_Utils.setOnClickAppOffsetLevel(mCtx, mLevelOffset + "");
                        BS_Utils.setOnClickAppOffsetTime(mCtx, mTimeOffset + "");*/
                        batteryAppbtn.setVisibility(View.GONE);
                        batteryAppNothingBtn.setVisibility(View.VISIBLE);
                        batteryAppNothingText.setText(String.valueOf(mAppcount));
                        belowButton.setText(R.string.bs_bottom_btn_save);

                    } else {//within 1 min
                        mCanClean = false;
                        batteryAppbtn.setVisibility(View.VISIBLE);
                        batteryAppNothingBtn.setVisibility(View.GONE);
                        batteryAppNothingText.setText("");
                        belowButton.setText(R.string.bs_bottom_btn);
                    }


                }

            }
        });
    }


    private double mCapacity;

    private double mCurCapacity;
    BroadcastReceiver mBatterBroadcastRecevier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Set<String> keys = intent.getExtras().keySet();
            /*for (String key : keys) {
                Object value = intent.getExtras().get(key);
                Log.e("zengrui", key + ":" + value.toString());
            }*/
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int health = intent.getIntExtra("health", BatteryManager.BATTERY_HEALTH_UNKNOWN);
                if (BatteryManager.BATTERY_HEALTH_GOOD == health) {

                }

                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                int voltage = intent.getIntExtra("voltage", 0);
                int temperature = intent.getIntExtra("temperature", 0);
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                int status = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
                Log.d(TAG, "MainActivity" + "::::level===" + level);

                if (level <= 20){
                    root.setBackgroundResource(R.color.bs_low_baterry_bg);
                } else {
                    root.setBackgroundResource(R.color.bs_baterry_bg);
                }
                if (BatteryManager.BATTERY_STATUS_CHARGING == status) {
                    mHandler.sendEmptyMessage(ChargingState);

                } else if (BatteryManager.BATTERY_STATUS_FULL == status && plugged != 0) {

                    mHandler.sendEmptyMessage(ChargedState);
                } else {
                    mHandler.sendEmptyMessage(UnchargeState);

                }

                String baterryInfo = BS_Utils.getBatteryInfo(mCtx);
                long timeOffset = 0;
                if (!TextUtils.isEmpty(baterryInfo)) {
                    timeOffset = Long.valueOf((BS_Utils.getValueforKey(mCtx, BS_Utils.BatteryKeyTime))) - System.currentTimeMillis();
                }

                if (TextUtils.isEmpty(baterryInfo) || timeOffset > 0.5 * 60 * 60 * 1000) {// 0.5 hour
                    baterryInfo = status + ":" + level + ":" + System.currentTimeMillis();
                    BS_Utils.setBatteryInfo(mCtx, baterryInfo);
                }

                int spLevel = Integer.valueOf(BS_Utils.getValueforKey(mCtx, BS_Utils.BatteryKeyLevel));
                long spTime = Long.valueOf((BS_Utils.getValueforKey(mCtx, BS_Utils.BatteryKeyTime)));
                int spState = Integer.valueOf(BS_Utils.getValueforKey(mCtx, BS_Utils.BatteryKeyStatus));
                float spChargeRate = BS_Utils.getBatteryChargeRate(mCtx);
                long spChargeRateTime = BS_Utils.getBatteryChargeRateTime(mCtx);
                // float spUseRate = BS_Utils.getBatteryUseRate(mCtx);
                // long spUseRateTime = BS_Utils.getBatteryUseRateTime(mCtx);
                long spUnchargeLeftTime = BS_Utils.getUnchargeLeftTime(mCtx);
                long spSetUnchargeCurTime = BS_Utils.getSpUnchargeCurTime(mCtx);

                if (mBaterryLevel == -1 || mBaterryLevel < (level - 3) || mBaterryLevel > (level + 3) || status != spState) {

                    mBaterryLevel = level * 100 / scale;
                    mCapacity = powerProfile.getBatteryCapacity();
                    mCurCapacity = mCapacity * level / 100;

                    if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                        //Log.e("zengrui","receiver>>charging>>"+level+"==="+spLevel);
                        if (level - spLevel > 2) {
                            float baterryChargeRateTemp = ((level - spLevel) / (float) (System.currentTimeMillis() - spTime)+spChargeRate)/2;
                            if (status == spState) {
                                if (spChargeRate != 0 && spChargeRateTime != 0 && System.currentTimeMillis() - spChargeRateTime < 60 * 1000) {
                                    mBaterryChargeRate = spChargeRate;
                                } else {
                                    if (baterryChargeRateTemp != 0) {
                                        String tempInfo = baterryChargeRateTemp + ":" + System.currentTimeMillis();
                                        BS_Utils.setBatteryChargeRateInfo(mCtx, tempInfo);
                                        mBaterryChargeRate = baterryChargeRateTemp;
                                    }
                                }
                            } else {
                                if (baterryChargeRateTemp != 0) {
                                    String tempInfo = baterryChargeRateTemp + ":" + System.currentTimeMillis();
                                    BS_Utils.setBatteryChargeRateInfo(mCtx, tempInfo);
                                    mBaterryChargeRate = baterryChargeRateTemp;
                                }
                            }

                            mBaterryChargeLeftTime = (long) ((scale - level) / mBaterryChargeRate);

                           /* if (mBaterryChargeRate != 0) {
                                BS_Utils.setBatteryChargeRate(mCtx,mBaterryChargeRate);
                                mBaterryChargeLeftTime = (long) ((scale - level) / mBaterryChargeRate);
                            }*/
                        } else if (spChargeRate != 0) {
                            mBaterryChargeRate = spChargeRate;
                            mBaterryChargeLeftTime = (long) ((scale - level) / mBaterryChargeRate);
                        } else {// not real
                            if (level > 50) {
                                mBaterryChargeLeftTime = (long) (1.9 * 60 * 60 * 1000);//2 hour
                            } else {
                                mBaterryChargeLeftTime = (long) (2.8 * 60 * 60 * 1000);//3 hour
                            }
                        }

                    } else if (status == BatteryManager.BATTERY_STATUS_FULL) {

                    } else {

                        // float baterryUseRateTemp = (spLevel - level) / (float) (System.currentTimeMillis() - spTime) ;
                        double curCapacity = powerProfile.getBatteryCapacity() * level / 100;

                        if (status == spState && spUnchargeLeftTime != 0 &&
                                spSetUnchargeCurTime != 0 && System.currentTimeMillis() - spSetUnchargeCurTime < 5 * 60 * 1000) {
                            mBaterryUseLeftTime = spUnchargeLeftTime;
                            //Toast.makeText(context,"receiver>>uncharge>>1111>>=="+mBaterryUseLeftTime,Toast.LENGTH_SHORT).show();
                            //BS_FileUtils.initData("receiver>>uncharge>>1111>>=="+mBaterryUseLeftTime);

                        } else {
                            double rate = BS_BatteryStatsHelper.cpuPowerCalculator(context);//cup
                            if (BS_SettingsUtil.wifiCheck(context)) {
                                rate += BS_BatteryStatsHelper.wifiPowerCalculator(context);
                            }
                            if (BS_SettingsUtil.bluetoothCheck()) {
                                rate += BS_BatteryStatsHelper.bluetoothPowerCalculator(context);
                            }

                            if (BS_SettingsUtil.getMobileDataState(context)) {
                                rate += BS_BatteryStatsHelper.radioPowerCalculator(context);
                            }

                            if (BS_SettingsUtil.gpsCheck(context)) {
                                rate += BS_BatteryStatsHelper.sensorPowerCalculator(context);
                            }

                            mBaterryUseLeftTime = (long) (curCapacity / rate * 60 * 60 * 1000);
                            //Toast.makeText(context,"receiver>>uncharge>>2222>>"+curCapacity+"=="+rate+"=="+mBaterryUseLeftTime,Toast.LENGTH_SHORT).show();
                            //BS_FileUtils.initData("receiver>>uncharge>>2222>>"+curCapacity+"=="+rate+"=="+mBaterryUseLeftTime);
                            BS_Utils.setUnchargeLeftTime(context, mBaterryUseLeftTime);
                            //BS_FileUtils.initData("receiver>>uncharge>>2222==111>>"+mBaterryUseLeftTime+"===="+BS_Utils.getUnchargeLeftTime(context));
                            BS_Utils.setSpUnchargeCurTime(context, System.currentTimeMillis());
                        }

                        //mBaterryUseLeftTime = (long) ((scale - level) / mBaterryUseRate);

                            /*if (mBaterryUseRate != 0) {
                                BS_Utils.setBatteryUseRate(mCtx,mBaterryUseRate);
                                mBaterryUseLeftTime = level / mBaterryUseRate;
                            }*/

                    }

                    if (status != spState) {
                        baterryInfo = status + ":" + level + ":" + System.currentTimeMillis();
                        BS_Utils.setBatteryInfo(mCtx, baterryInfo);
                    }

                    /*if (spOnclickAppTime >= System.currentTimeMillis() - 60 * 1000) {// within one mints
                        mBaterryLevel = Math.min(mBaterryLevel + (int) spOffsetLevel, 100);
                        mBaterryChargeLeftTime = (long) (mBaterryChargeLeftTime - mBaterryUseLeftTime);
                        mBaterryUseLeftTime = mBaterryUseLeftTime + mBaterryUseLeftTime;

                    }*/

                    mHandler.sendEmptyMessage(BatteryLevelChange);
                }


            }
        }

    };

    @Override
    public void onLeftClick() {

    }

    @Override
    public void onCenterClick() {

    }

    @Override
    public void onRightClick() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse("https://www.facebook.com/droigroup");
        intent.setData(content_url);
        startActivity(intent);
    }
}
