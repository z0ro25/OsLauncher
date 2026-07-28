package com.amz.ios.http.Internal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.amz.newspage.newssource.utils.WorkFlowScheduler;

/**
 * Author       : yizhihao
 * Create time  : 2016-12-08 下午8:09
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public abstract class Action<T> implements Runnable, Comparable<Action> {

    public static final boolean DEBUG = com.amz.ios.ioslite.common.BuildConfig.DEBUG;

    private static final String TAG = Action.class.getSimpleName();

    private static final int THREAD_MAIN = 0x1;

    private static final int THREAD_ASYN = 0x2;

    private static Handler mHandlerLooper = new Handler(WorkFlowScheduler.MainLoop());

    private int FlAG_MASK = 0x00000000;

    private int FLAG_CANCELED = 0x10000000;

    private Integer mSequence;

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        IMMEDIATE;

        private Priority() {
        }
    }

    public boolean isCanceled() {
        return (FlAG_MASK & FLAG_CANCELED) == FLAG_CANCELED;
    }

    public boolean isValid() {
        return mHandler != null ? (realtime >= mHandler.getRealexcuteTime()) : false;
    }

    public final Integer getSequence() {
        if (this.mSequence == null) {
            throw new IllegalStateException("getSequence called before setSequence");
        } else {
            return this.mSequence.intValue();
        }
    }

    public final Action<T> setSequence(Integer sequence) {
        this.mSequence = Integer.valueOf(sequence);
        return this;
    }

    public Priority getPriority() {
        return Priority.NORMAL;
    }

    /**
     * time start running;
     */
    private long begin;

    private String name;

    private BaseProvider mHandler;

    private CancelableCallBack mCallBack;

    private int threadType = THREAD_ASYN;

    private long delay = 0;

    private long realtime = 0;

    protected boolean mQuit = false;

    public final Action cancel() {
        FlAG_MASK |= FLAG_CANCELED;
        if (mCallBack != null) {
            mCallBack.setCanceled(true);
        }
        mQuit = true;
        mHandler = null;
        return this;
    }

    public String getName() {
        return name;
    }

    public Action(BaseProvider handler, String name) {
        mHandler = handler;
        realtime = System.currentTimeMillis();
        this.name = name;
        mHandler.holdAction(this);
    }

    public Action(BaseProvider handler) {
        mHandler = handler;
    }

    public Action from(Looper looper) {
        if (looper != null) {
            mHandlerLooper = new Handler(looper);
        }
        return this;
    }

    /**
     * subclass should do this for work start
     * it will work on thread by specific by {@link #asynLoop()} (Looper)}
     * default on MainLooper
     *
     * @param callBack
     */
    protected abstract void work(CancelableCallBack<T> callBack);

    @Override
    public void run() {
        try {
            if (DEBUG) Log.e(TAG, ">>>>>>Action#run : " + name + " start running!");
            if (mQuit) {
                Log.e(TAG, ">>>>>>DataFlowProvider : " + getName() + " is canceled");
                if (mCallBack != null) mCallBack.onFalure("", 0);
                return;
            }
            begin = System.currentTimeMillis();
            work(mCallBack);
        } catch (Exception e) {
            Log.e(TAG, ">>>>>>Action#run : " + e.getMessage());
        } finally {
            final Thread thread = Thread.currentThread();
            if (DEBUG)
                Log.d(TAG, ">>>>>>Action " + name + " work on : [" + thread.getName() + " : " + thread.getId() + "]");
            if (DEBUG) Log.d(TAG, ">>>>>>tasks " + (System.currentTimeMillis() - begin) + "ms");
            //auto releas out ref;
            mHandlerLooper = null;
        }
    }

    /***
     * control if a error occur ,how to should we show tip.
     */
    private boolean mShouldShowErrorTips = false;

    public void setShouldShowErrorTips(boolean shouldShowErrorTips) {
        this.mShouldShowErrorTips = shouldShowErrorTips;
        if (mCallBack != null) mCallBack.setShowTips(shouldShowErrorTips);
    }

    public Action delay(long time) {
        delay = time;
        return this;
    }

    public final BaseProvider observer(CancelableCallBack<T> result) {
        if (result == null) return mHandler;
        if (result.isCanceled()) {
            //in case a situation , we are checking permission,
            //at the same time cancel task has called, some cse will happen
            //two or more task.So we just throw it before it add in last control action pool.
            //and we provide a method to solve it.
            result.onFalure("", 0);
            return mHandler;
        }
        mCallBack = result.setName(name).setHandler(this);
        mCallBack.setShowTips(mShouldShowErrorTips);
        if (mHandlerLooper == null) {
            mHandlerLooper = new Handler(WorkFlowScheduler.MainLoop());
        }
        switch (threadType) {
            case THREAD_MAIN:
                mHandlerLooper.postDelayed(this, delay);
                break;
            case THREAD_ASYN:
            default:
                if (delay > 0) {
                    mHandlerLooper.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mHandler.addAction(Action.this);
                        }
                    }, delay);
                } else {
                    mHandler.addAction(this);
                }
                break;
        }
        return mHandler;
    }

    public Action asynLoop() {
        threadType = THREAD_ASYN;
        return this;
    }

    public Action MainLoop() {
        threadType = THREAD_MAIN;
        return this;
    }

    /**
     * in case a call back way  to stat  a cancelable task.
     *
     * @return
     */
    public Action addInPool() {
        mHandler.holdAction(this);
        return this;
    }

    @Override
    public int compareTo(Action other) {
        Priority left = this.getPriority();
        Priority right = other.getPriority();
        return left == right ? this.mSequence.intValue() - other.mSequence.intValue() : right.ordinal() - left.ordinal();
    }
}