package com.zhuoyi.security.soft.lock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.ios.sc.common.utils.C_SC_Service_Communication;
import com.zhuoyi.security.batterysave.R;
import com.zhuoyi.security.batterysave.util.SL_Util;
import com.zhuoyi.security.batterysave.views.SL_GlobalActivity;

public class SL_EnterPassWord extends SL_GlobalActivity implements OnClickListener{
    private ImageButton num0,num1,num2,num3,num4,num5,num6,num7,num8,num9;
    private ImageButton numReinput;
    private ImageButton numBack;
    //private Button buttonSure;
    private EditText etLockviewPwd;

    private SharedPreferences settingsLock;
    private String packageName = null;
    private boolean inpak = false;
    private Context mContext = null;
    private LinearLayout fingerLay;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        C_SC_Service_Communication.startServiceForIntent(getApplicationContext(), getIntent());

        setContentView(R.layout.sl_layout_privacy_digital_psw_enter2);
        mContext = SL_EnterPassWord.this;
        packageName = getIntent().getStringExtra("packageName");
        initView();
        initSharePreference();
        setOnClick();
    }

        private void initView(){
            etLockviewPwd = (EditText)findViewById(R.id.sl_et_lockview_pwd);
            etLockviewPwd.setTextColor(getResources().getColor(R.color.sl_white));
            //buttonSure = (Button)findViewById(R.id.sl_button_sure);
            numReinput = (ImageButton)findViewById(R.id.sl_num_reinput);
            numBack = (ImageButton)findViewById(R.id.sl_num_back);
            num0 =(ImageButton)findViewById(R.id.sl_num0);
            num1 =(ImageButton)findViewById(R.id.sl_num1);
            num2 =(ImageButton)findViewById(R.id.sl_num2);
            num3 =(ImageButton)findViewById(R.id.sl_num3);
            num4 =(ImageButton)findViewById(R.id.sl_num4);
            num5 =(ImageButton)findViewById(R.id.sl_num5);
            num6 =(ImageButton)findViewById(R.id.sl_num6);
            num7 =(ImageButton)findViewById(R.id.sl_num7);
            num8 =(ImageButton)findViewById(R.id.sl_num8);
            num9 =(ImageButton)findViewById(R.id.sl_num9);
            fingerLay = (LinearLayout)findViewById(R.id.sl_finger_lay);
        }


        private void setOnClick(){
            num0.setOnClickListener(this);
            num1.setOnClickListener(this);
            num2.setOnClickListener(this);
            num3.setOnClickListener(this);
            num4.setOnClickListener(this);
            num5.setOnClickListener(this);
            num6.setOnClickListener(this);
            num7.setOnClickListener(this);
            num8.setOnClickListener(this);
            num9.setOnClickListener(this);
            numBack.setOnClickListener(this);
            numReinput.setOnClickListener(this);
            //buttonSure.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            etLockviewPwd.setHint(R.string.sl_enter_psw_16);
            int id = view.getId();
            if (id == R.id.sl_num0) {
                SL_Util.setEditText(etLockviewPwd,"0");
            } else if (id == R.id.sl_num1) {
                SL_Util.setEditText(etLockviewPwd,"1");
            } else if (id == R.id.sl_num2) {
                SL_Util.setEditText(etLockviewPwd,"2");
            } else if (id == R.id.sl_num3) {
                SL_Util.setEditText(etLockviewPwd,"3");
            } else if (id == R.id.sl_num4) {
                SL_Util.setEditText(etLockviewPwd,"4");
            } else if (id == R.id.sl_num5) {
                SL_Util.setEditText(etLockviewPwd,"5");
            } else if (id == R.id.sl_num6) {
                SL_Util.setEditText(etLockviewPwd,"6");
            } else if (id == R.id.sl_num7) {
                SL_Util.setEditText(etLockviewPwd,"7");
            } else if (id == R.id.sl_num8) {
                SL_Util.setEditText(etLockviewPwd,"8");
            } else if (id == R.id.sl_num9) {
                SL_Util.setEditText(etLockviewPwd,"9");
            }/* else if (id ==R.id.sl_button_sure) {
                buttonSure();
            }*/ else if (id == R.id.sl_num_reinput) {
                SL_Util.reInputEditText(etLockviewPwd);
            } else if (id == R.id.sl_num_back) {
                //SL_Util.backEditText(etLockviewPwd);
            }
            etLockviewPwd.setTextColor(getResources().getColor(R.color.sl_white));
        }

        private void buttonSure() {
            String pwd = etLockviewPwd.getText().toString();
            String pwd_sure = settingsLock.getString("Pass_Word", "");
            if (!TextUtils.isEmpty(pwd)) {
                if (pwd_sure.equals(pwd)) {
                    Intent myIntent = C_SC_Service_Communication.getServiceIntent(C_SC_Service_Communication.SOFT_LOCK_STATA_UPDATE);
                    myIntent.putExtra("slPkgName", packageName);
                    C_SC_Service_Communication.startServiceForIntent(mContext, myIntent);

                    inpak = true;
                    finish();
                } else {
                    etLockviewPwd.setText("");
                    etLockviewPwd.setHint(R.string.sl_enter_psw_16_error);
                }
            } else {
                etLockviewPwd.setHint(R.string.sl_enter_psw_16_empty);
            }

        }

    private void initSharePreference() {
        settingsLock = getSharedPreferences("LOCK_SOFT", 1);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            Intent intent = new Intent(Intent.ACTION_MAIN);
//               intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.addCategory(Intent.CATEGORY_HOME);
            Intent intent = new Intent("ioslite.intent.action.IOSLITE");
            this.startActivity(intent);
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
//        if(!inpak){
//             Intent i = new Intent("com.tyd.security.clean.task_ALL_KILL");
//             i.putExtra("packageName", packageName);
//             sendBroadcast(i);
//        }
//         finish();
        super.onPause();
    }


    @Override
    protected void onResume() {

        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        packageName = getIntent().getStringExtra("packageName");
        super.onNewIntent(intent);
    }

}



 