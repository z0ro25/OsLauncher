package com.zhuoyi.security.soft.lock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.ios.sc.common.utils.C_SC_Service_Communication;
import com.zhuoyi.security.batterysave.R;
import com.zhuoyi.security.batterysave.views.SL_GlobalActivity;
import com.zhuoyi.security.batterysave.views.SL_TitleBar;

public class SL_LockSettingActivity extends SL_GlobalActivity implements OnClickListener, SL_TitleBar.CallBack{
    private ImageView leftButton ;
    private CheckBox mLockSwitchBox;
//    private ImageButton leftButton;
    private boolean mLockSwitchButton =true;
    private LinearLayout switchLinearLayout , changePassword, facebookLay;
    private SharedPreferences settingsLock;
    private SharedPreferences sharedLock;
    private Editor editorLock;
    private SL_TitleBar slSettingTitle;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sl_lock_setting_activity);
        initSharePreference();
        mLockSwitchButton = settingsLock.getBoolean("LockSwitch_button", true);
        initView();

    }

    private void initView(){
        slSettingTitle = (SL_TitleBar) findViewById(R.id.sl_setting_title);
        slSettingTitle.setOnCallBack(SL_LockSettingActivity.this);
        mLockSwitchBox = (CheckBox)findViewById(R.id.sl_lock_swtich_box);
        mLockSwitchBox.setChecked(mLockSwitchButton);
        mLockSwitchBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean check) {
                // TODO Auto-generated method stub
                if (check) {
                    editorLock.putBoolean("LockSwitch_button", true);
                    mLockSwitchBox.setChecked(true);
                } else {
                    editorLock.putBoolean("LockSwitch_button", false);
                    mLockSwitchBox.setChecked(false);
                }
                editorLock.commit();
            }
        });
        switchLinearLayout = (LinearLayout)findViewById(R.id.sl_lock_switchbutton_linearlayout);
        switchLinearLayout.setOnClickListener(this);
        changePassword = (LinearLayout)findViewById(R.id.sl_lock_switchbutton_linearlayout_change_password);
        changePassword.setOnClickListener(this);
        facebookLay = (LinearLayout) findViewById(R.id.sl_facebook);
        facebookLay.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.sl_lock_switchbutton_linearlayout){
            switchCloudScanOnOff();
        } else if (id == R.id.sl_lock_switchbutton_linearlayout_change_password) {
            changePasswordIntent();
        } else if (id == R.id.sl_facebook) {
            Uri uri = Uri.parse("https://www.facebook.com/droigroup");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }

    private void switchCloudScanOnOff() {
//        mLockSwitchBox.doSwitch();
        if (mLockSwitchBox.isChecked()) {
            editorLock.putBoolean("LockSwitch_button", false);
            mLockSwitchBox.setChecked(false);
        } else {
            editorLock.putBoolean("LockSwitch_button", true);
            mLockSwitchBox.setChecked(true);
        }
        editorLock.commit();
        Intent myIntent = C_SC_Service_Communication.getServiceIntent(C_SC_Service_Communication.SOFT_LOCK_SWITCH);
        C_SC_Service_Communication.startServiceForIntent(SL_LockSettingActivity.this, myIntent);
    }

    private void initSharePreference(){
        settingsLock = getSharedPreferences("LOCK_SOFT",1);
        sharedLock = getSharedPreferences("LOCK_SOFT",1);
        editorLock=sharedLock.edit();
    }


    private void changePasswordIntent(){

        Intent i = new Intent();
        i.putExtra("change_pass_word", true);
        i.setClass(this, SL_SetPassWord.class);
        startActivity(i);



    }

    @Override
    public void onLeftClick() {
        // TODO Auto-generated method stub
        finish();
    }

    @Override
    public void onCenterClick() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRightClick() {
        // TODO Auto-generated method stub

    }


}
