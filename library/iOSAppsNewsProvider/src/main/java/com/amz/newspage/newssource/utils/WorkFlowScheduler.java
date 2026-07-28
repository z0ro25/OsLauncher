package com.amz.newspage.newssource.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-09 下午9:16
 */
public abstract class WorkFlowScheduler {

    private static final String TAG = WorkFlowScheduler.class.getSimpleName();

    private final static AtomicInteger COUNT = new AtomicInteger();

    private Handler mBackhandler;

    private Handler mReturnHandler;

    private ResultProvider mHeadPosDispater;

    protected SourceProvider mSourceProvider;

    private ResultProvider mLastResultProvider = null;

    public static Looper MainLoop() {
        return Looper.getMainLooper();
    }

    public WorkFlowScheduler() {
        COUNT.incrementAndGet();
    }

    /**
     * default handler is a asyn thread
     *
     * @param looper
     * @return
     */
    public final WorkFlowScheduler workOn(Looper looper) {
        if (looper == null) {
            throw new IllegalArgumentException("Work Loop can't be null");
        }
        mBackhandler = new Handler(looper);
        return this;
    }

    public static boolean currentMainThread() {
        return Thread.currentThread().getId() == MainLoop().getThread().getId();
    }

    /**
     * default on main thread
     *
     * @param looper
     * @return
     */
    public final WorkFlowScheduler firstOn(Looper looper) {
        if (looper == null) {
            throw new IllegalArgumentException("Return Loop can't be null");
        }
        mReturnHandler = new Handler(looper);
        return this;
    }

    public final WorkFlowScheduler firstReturn(ResultProvider resultProvider) {
        mHeadPosDispater = resultProvider;
        mLastResultProvider = resultProvider;
        return this;
    }

    public final void start() {
        checkBackHandler();
        mSourceProvider = provider();
        mBackhandler.post(mSourceProvider);
    }

    private void checkBackHandler() {
        if (mBackhandler == null) {
            HandlerThread workThread = new HandlerThread(TAG + "-BackScheduler-" + COUNT.get());
            mBackhandler = new Handler(workThread.getLooper());
        }
    }

    private void checkResultProvider() {
        if (mReturnHandler == null && mHeadPosDispater != null) {
            mReturnHandler = new Handler(MainLoop());
        }
    }

    public abstract <T> SourceProvider<T> provider();

    /**
     * make sure this excuate after {@link #doAfter(ResultProvider)}
     *
     * @param looper
     */
    public final WorkFlowScheduler doAfterOn(Looper looper) {
        if (looper == null)
            throw new IllegalArgumentException("Looper-After can't be null!");
        mLastResultProvider.setHandler(new Handler(looper));
        return this;
    }

    public final WorkFlowScheduler doAfter(ResultProvider resultProvider) {
        if (resultProvider == null) {
            throw new IllegalArgumentException("resultProvider can't be null");
        }
        mLastResultProvider.setNextPos(resultProvider);
        mLastResultProvider = resultProvider;
        return this;
    }

    /**
     * provider source
     *
     * @param <T>
     */
    public abstract class SourceProvider<T> implements Runnable {

        private Handler mResultHandler = null;

        public void setResultHandler(Handler resultHandler) {
            mResultHandler = resultHandler;
        }

        abstract T providerSource();

        @Override
        public void run() {
            T result = providerSource();
            checkResultProvider();
            mResultHandler.post(mHeadPosDispater.setResult(result));
        }
    }


    public abstract class ResultProvider<IN, OUT> implements Runnable {

        private Handler mHandler;

        private IN mResult;

        private ResultProvider nextPos;

        public ResultProvider setHandler(Handler handler) {
            mHandler = handler;
            return this;
        }

        public Handler getHandler() {
            return mHandler;
        }

        public ResultProvider setResult(IN result) {
            this.mResult = result;
            return this;
        }

        public ResultProvider setNextPos(ResultProvider nextPos) {
            this.nextPos = nextPos;
            return this;
        }

        abstract OUT postResult(IN result);

        private ResultProvider getNext() {
            return nextPos;
        }

        private boolean shouldSheduler() {
            return nextPos != null;
        }

        public void post() {
            getHandler().post(this);
        }

        @Override
        public void run() {

            final OUT OUT = postResult(mResult);

            //if has next shedule next task;
            if (shouldSheduler()) {

                //set next value then handler it
                getNext().setResult(OUT).post();

            }
        }
    }
}
