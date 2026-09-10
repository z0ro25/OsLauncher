package com.ezt.ios.utils

import com.truongnt.ios.launcher.PagedView.PageAnimationType

object LauncherInteractor {
    var mPageAnimationType = PageAnimationType.NONE

    fun setPageTransitionAnim(type: PageAnimationType) {
        mPageAnimationType = type
    }
}