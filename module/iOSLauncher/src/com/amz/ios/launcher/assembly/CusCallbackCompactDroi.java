package com.amz.ios.launcher.assembly;

import android.view.View;

import com.amz.ios.ioslite.common.launcher.LeftCustomContentCallbacks;

import java.lang.reflect.Method;

/**
 * 负一屏回调借口，兼容卓易头条
 */
public class CusCallbackCompactDroi implements LeftCustomContentCallbacks {
    private View mDroiNewsPage;

    public CusCallbackCompactDroi(View view) {
        mDroiNewsPage = view;
    }

    @Override
    public boolean isScrollingAllowed(float downX, float downY, float lastX, float lastY, float rawX, float rawY) {
        try {
            Method method = mDroiNewsPage.getClass().getDeclaredMethod("isScrollingAllowed", new Class[]{float.class, float.class, float.class, float.class});
            method.setAccessible(true);
            Object object = method.invoke(mDroiNewsPage, new Object[]{Float.valueOf(downX), Float.valueOf(downY), Float.valueOf(lastX), Float.valueOf(lastY)});
            return ((Boolean) object).booleanValue();
        } catch (Exception e) {
            //
        }
        return false;
    }

    @Override
    public void onShow(boolean fromResume) {
        try {
            Method method = mDroiNewsPage.getClass().getDeclaredMethod("onShow", new Class[]{boolean.class});
            method.setAccessible(true);
            method.invoke(mDroiNewsPage, new Object[]{Boolean.valueOf(fromResume)});
        } catch (Exception e) {
            //
        }
    }

    @Override
    public void onHide() {
        try {
            Method method = mDroiNewsPage.getClass().getDeclaredMethod("onHide", new Class[]{});
            method.setAccessible(true);
            method.invoke(mDroiNewsPage, new Object[]{});
        } catch (Exception e) {
            //
        }
    }

    @Override
    public void onScrollProgressChanged(float progress) {
        try {
            Method method = mDroiNewsPage.getClass().getDeclaredMethod("onScrollProgressChanged", new Class[]{float.class});
            method.setAccessible(true);
            method.invoke(mDroiNewsPage, new Object[]{Float.valueOf(progress)});
        } catch (Exception e) {
            //
        }
    }

    @Override
    public boolean onBackPressed() {
        try {
            Method method = mDroiNewsPage.getClass().getDeclaredMethod("onBackPressed", new Class[]{});
            method.setAccessible(true);
            Object object = method.invoke(mDroiNewsPage, new Object[]{});
            return ((Boolean) object).booleanValue();
        } catch (Exception e) {
            //
        }
        return false;
    }
}
