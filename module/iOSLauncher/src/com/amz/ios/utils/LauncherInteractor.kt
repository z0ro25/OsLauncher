package com.amz.ios.utils

import com.amz.ios.launcher.PagedView.PageAnimationType

object LauncherInteractor {
    var mPageAnimationType = PageAnimationType.NONE

    fun setPageTransitionAnim(type: PageAnimationType) {
        mPageAnimationType = type
    }
}