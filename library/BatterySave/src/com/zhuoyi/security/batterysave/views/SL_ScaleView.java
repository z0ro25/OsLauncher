/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhuoyi.security.batterysave.views;

import android.content.Context;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

/**
 * Helper class for performing atomic operations on a file, by creating a backup
 * file until a write has successfully completed.
 * <p>
 * Atomic file guarantees file integrity by ensuring that a file has been
 * completely written and sync'd to disk before removing its backup. As long as
 * the backup file exists, the original file is considered to be invalid (left
 * over from a previous attempt to write the file).
 * </p>
 * <p>
 * Atomic file does not confer any file locking semantics. Do not use this class
 * when the file may be accessed or modified concurrently by multiple threads or
 * processes. The caller is responsible for ensuring appropriate mutual
 * exclusion invariants whenever it accesses the file.
 * </p>
 * 
 * @param <SaAnimationListener>
 */
public class SL_ScaleView extends ImageView implements OnTouchListener {
    Context mContext;
    Handler handler;
    ImageView v;
    int viewId;
    int mLastEven;
    boolean mPressed,mReleased;
    TestViewAnimationListener listener = new TestViewAnimationListener();
    final ScaleAnimation animation =new ScaleAnimation(1.0f, 0.9f, 1.0f, 0.9f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

    final ScaleAnimation animation2 =new ScaleAnimation(0.95f, 1.0f, 0.95f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    public SL_ScaleView(Context context) {
        super(context);
        mContext = context;
        this.setOnTouchListener(this);
        v = this;
    }

    public SL_ScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        this.setOnTouchListener(this);
        v = this;
        animation.setDuration(200);//设置动画持续时间
//        animation.setFillAfter(true);
        animation2.setDuration(200);//设置动画持续时间
//        animation2.setFillAfter(true);
    }

    public void setCallback(int id, Callback callback) {
        handler = new Handler(callback);
        viewId = id;
    }

    class TestViewAnimationListener implements AnimationListener {

        @Override
        public void onAnimationEnd(Animation animation) {
            // TODO Auto-generated method stub

            if (handler != null) {
                Message msg = new Message();
                msg.what = 999;
                msg.arg1 = viewId;
                handler.sendMessage(msg);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onAnimationStart(Animation animation) {
            // TODO Auto-generated method stub
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN: {
//            v.setFocusable(true);
//            v.requestFocus();
        /*    AnimationSet mAnimationSet = new AnimationSet(true);
            mAnimationSet.addAnimation(animation);
            mAnimationSet.setFillAfter(true);
            v.startAnimation(mAnimationSet);*/
            mReleased = false;
            mPressed = true;
            playAnimationZoomOut(v);
        }
            return false;
        case MotionEvent.ACTION_CANCEL:
        /*case MotionEvent.ACTION_MOVE:*/ {
            /*v.setFocusable(true);
            v.requestFocus();
            AnimationSet mAnimationSet = new AnimationSet(true);
            mAnimationSet.addAnimation(animation2);
            mAnimationSet.setFillAfter(true);
            v.startAnimation(mAnimationSet);
            */
            if(mPressed){
                mReleased = true;
            }
            else{
                playAnimationZoomIn(v,false);
            }

            mLastEven = MotionEvent.ACTION_CANCEL;
            return false;
        }
        case MotionEvent.ACTION_MOVE:
            return false;

        case MotionEvent.ACTION_UP: {/*
            v.setFocusable(true);
            v.requestFocus();
            AnimationSet mAnimationSet = new AnimationSet(true);
            mAnimationSet.addAnimation(animation2);
            mAnimationSet.setFillAfter(true);
            mAnimationSet.setAnimationListener(listener);
            v.startAnimation(mAnimationSet);*/
            if(mPressed){
                mReleased = true;
            }
            else{
                playAnimationZoomIn(v,true);
            }
            mLastEven = MotionEvent.ACTION_UP;

            return false;
        }

        }
        return true;
    }
    AnimationListener mZoomOutListener = new AnimationListener() {

        @Override
        public void onAnimationStart(Animation arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onAnimationRepeat(Animation arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onAnimationEnd(Animation arg0) {
            // TODO Auto-generated method stub
            if(mReleased){
                if(mLastEven == MotionEvent.ACTION_UP)
                    playAnimationZoomIn(v,true);
                else
                    playAnimationZoomIn(v,false);

            }
            mPressed = false;

        }
    };
    private void playAnimationZoomOut(View v){
        ScaleAnimation sa;
//        if(viewId == R.id.kill_button){
//              sa = new ScaleAnimation(1.0f, 0.95f, 1.0f, 0.95f,
//                    Animation.RELATIVE_TO_SELF, 0.5f,
//                    Animation.RELATIVE_TO_SELF, 0.5f);
//
//        }else{

              sa = new ScaleAnimation(1.0f, 0.8f, 1.0f, 0.8f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
//        }
        sa.setDuration(150);
        sa.setFillAfter(true);
        sa.setInterpolator(new DecelerateInterpolator());
        AlphaAnimation aa = new AlphaAnimation(1.0f, 0.5f);// (1.0f, 0.3f,
                                                            // 1.0f, 0.3f);
        aa.setDuration(150);
        aa.setFillAfter(true);

        aa.setInterpolator(new DecelerateInterpolator());
        AnimationSet as = new AnimationSet(false);
        as.addAnimation(sa);
        as.addAnimation(aa);

        as.setFillAfter(true);
        as.setAnimationListener(mZoomOutListener);
        v.startAnimation(as);
    }

    private void playAnimationZoomIn(View v,boolean canClick){
        ScaleAnimation sa ;
//        if(viewId == R.id.kill_button){
//              sa = new ScaleAnimation(0.95f, 1.0f, 0.95f, 1.0f,
//                    Animation.RELATIVE_TO_SELF, 0.5f,
//                    Animation.RELATIVE_TO_SELF, 0.5f);
//
//        }else{
              sa = new ScaleAnimation(0.8f, 1.0f, 0.8f, 1.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);

//        }
        sa.setDuration(150);
        sa.setFillAfter(true);
        sa.setInterpolator(new DecelerateInterpolator());
        AlphaAnimation aa = new AlphaAnimation(0.5f, 1.0f);
        aa.setDuration(150);
        aa.setFillAfter(true);

        aa.setInterpolator(new DecelerateInterpolator());
        AnimationSet as = new AnimationSet(false);
        as.addAnimation(sa);
        as.addAnimation(aa);

        as.setFillAfter(true);
        if(canClick)
            as.setAnimationListener(listener);
        v.startAnimation(as);
        }


}