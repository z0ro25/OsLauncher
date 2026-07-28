package com.amz.ios.themeclub.ui.fragment;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.amz.ios.themeclub.R;

/**
 * Created by ZhangMingZhe on 11/16/16.
 */

public class ThemeSpeedFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.theme_speed_fragment,null);
        return v;
    }
}
