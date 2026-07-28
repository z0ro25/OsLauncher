package com.amz.ios.themeclub.ui.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.amz.ios.launcher.views.CustomTextView;
import com.amz.ios.themeclub.R;

/**
 * Created by server on 16-11-14.
 */
public class PlaceholderChildFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";

    public PlaceholderChildFragment() {
    }

    public static PlaceholderChildFragment newInstance(int sectionNumber) {
        PlaceholderChildFragment fragment = new PlaceholderChildFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.themeclub_fragment_main, container, false);
        CustomTextView textView = (CustomTextView) rootView.findViewById(R.id.section_label);
        return rootView;
    }
}
