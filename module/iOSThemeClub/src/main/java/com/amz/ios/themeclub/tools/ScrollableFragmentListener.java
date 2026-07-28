package com.amz.ios.themeclub.tools;

import com.amz.ios.themeclub.tools.ScrollableListener;

/**
 * Created by server on 16-11-17.
 */

public interface ScrollableFragmentListener {

    public void onFragmentAttached(ScrollableListener fragment, int position);

    public void onFragmentDetached(ScrollableListener fragment, int position);
}
