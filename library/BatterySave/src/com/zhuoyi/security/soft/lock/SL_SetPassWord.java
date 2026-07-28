package com.zhuoyi.security.soft.lock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.zhuoyi.security.batterysave.R;
import com.zhuoyi.security.batterysave.util.SL_Util;
import com.zhuoyi.security.batterysave.views.SL_GlobalActivity;

public class SL_SetPassWord extends SL_GlobalActivity implements OnClickListener{

    private ImageButton num0,num1,num2,num3,num4,num5,num6,num7,num8,num9;
    private ImageButton numReinput;
    private ImageButton numBack;
    private EditText etLockviewPwd;
    private TextView etLockview;
    private int inputTimes=0;
    private String psd00;
    private SharedPreferences settingsLock;
    private SharedPreferences sharedLock;
    private Editor editorLock;
    private boolean changePassWord = false;

    private String from ="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sl_layout_privacy_digital_psw_enter2);
        from = getIntent().getStringExtra("LOCK_FORM"); // SecurityCentreSet
        changePassWord = getIntent().getBooleanExtra("change_pass_word", false);
        initSharePreference();
        initView();
        setOnClick();

        if(!settingsLock.getBoolean("Fist_start", true)){
            inputTimes = 1;
            etLockview.setText(R.string.sl_enter_psw_16);
        }

        if(changePassWord){
            inputTimes = 0;
            etLockview.setText(R.string.sl_enter_new_psw_16);
        }

    }

    private void initView(){
        etLockviewPwd = (EditText)findViewById(R.id.sl_et_lockview_pwd);
        etLockview = (TextView)findViewById(R.id.sl_et_lockview);
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

        etLockviewPwd.addTextChangedListener(watcher);

    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.sl_num0) {
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
        } else if (id == R.id.sl_num_reinput) {
            SL_Util.reInputEditText(etLockviewPwd);
        } else if (id == R.id.sl_num_back) {
            SL_Util.backEditText(etLockviewPwd, etLockview);
        }
    }

    private TextWatcher watcher = new TextWatcher() {
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }
        
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {

        }
        
        @Override
        public void afterTextChanged(Editable s) {
            int length = etLockviewPwd.length();
            if (length == 6) {
                if(settingsLock.getBoolean("Fist_start", true)){
                    buttonSure();
                }else  {
                    if(changePassWord){
                        buttonSureChangePass();
                    }else{
                        buttonSure1();
                    }
                }
            }
        }
    };

    private void buttonSure1(){
        if(etLockviewPwd.getText().toString().equals(settingsLock.getString("Pass_Word", ""))){
            Intent mIntent = new Intent();
            if(!TextUtils.isEmpty(from)&&from.equals("SecurityCentreSet")){
                mIntent.setClass(this, SL_LockSettingActivity.class);
            }else{
                mIntent.setClass(this, SL_LockAppListActivity.class);
            }
            startActivity(mIntent);
            finish();
        }else{
            etLockviewPwd.setText("");
            etLockview.setText(R.string.sl_enter_psw_16_error);
        }
    }

    private void buttonSure() {

        if (inputTimes == 0) {
            psd00 = etLockviewPwd.getText().toString();
            etLockviewPwd.setText("");
            etLockview.setText(R.string.sl_enter_new_psw_16_again);
            inputTimes = 1;
        } else if (inputTimes == 1) {
            if (etLockviewPwd.getText().toString().equals(psd00)) {
                inputTimes = 0;
                Intent mIntent = new Intent();
                mIntent.setClass(this, SL_LockAppListActivity.class);
                startActivity(mIntent);
                editorLock.putBoolean("Fist_start", false);
                editorLock.putString("Pass_Word", etLockviewPwd.getText().toString());
                editorLock.commit();
                finish();
            } else {
                etLockviewPwd.setText("");
                etLockview.setText(R.string.sl_enter_psw_16_again_error);
            }
        } else {
            etLockviewPwd.setText("");
            etLockview.setText(R.string.sl_enter_psw_16_error);
        }
    }

    private void buttonSureChangePass(){
        if (inputTimes == 0) {
            psd00 = etLockviewPwd.getText().toString();
            etLockviewPwd.setText("");
            if (settingsLock.getString("Pass_Word", "").equals(psd00)) {
                etLockview.setText(R.string.sl_enter_new_psw);
            } else {
                etLockview.setText(R.string.sl_enter_psw_16_again);
                inputTimes = 1;
            }
        } else if (inputTimes == 1) {

            if (etLockviewPwd.getText().toString().equals(psd00)) {
                inputTimes = 0;
                editorLock.putBoolean("Fist_start", false);
                editorLock.putString("Pass_Word", etLockviewPwd.getText().toString());
                editorLock.commit();
                Toast.makeText(this, getString(R.string.sl_change_password_sucessed), 0).show();
                finish();
            } else {
                etLockviewPwd.setText("");
                etLockview.setText(R.string.sl_enter_psw_16_again_error);
            }
        }
    }

    private void initSharePreference(){
        settingsLock = getSharedPreferences("LOCK_SOFT",1);
        sharedLock = getSharedPreferences("LOCK_SOFT",1);
        editorLock=sharedLock.edit();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }
}
