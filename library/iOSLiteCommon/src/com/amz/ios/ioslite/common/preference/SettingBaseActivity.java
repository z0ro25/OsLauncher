package com.amz.ios.ioslite.common.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.amz.ios.ioslite.common.CommonAppCompatActivity;
import com.amz.ios.ioslite.common.R;


public class SettingBaseActivity extends CommonAppCompatActivity {
    protected boolean mUseExitAnim = true;
    protected Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
    }

    protected void startActivity(Class<? extends Activity>  target) {
        Intent intent = new Intent(this, target);
        startActivity(intent);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
    }

    @Override
    public void finish() {
        super.finish();
        if (mUseExitAnim) {
            overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);

        }
    }


}
