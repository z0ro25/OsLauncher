package com.amz.ios.http.Internal;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-19 上午11:02
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public abstract class BaseProvider {

    private static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = 2;

    private static final String TAG = BaseProvider.class.getSimpleName();

    private static final boolean DEBUG = com.amz.ios.ioslite.common.BuildConfig.DEBUG;

    private long realexcuteTime = 0;

    private AtomicInteger mSequenceGenerator;

    private BlockingQueue<Action> mWaitActions = new PriorityBlockingQueue();
    private MerlinDispatcher[] mRunBus;

    private Context mContext;

    public BaseProvider(Context context) {
        mSequenceGenerator = new AtomicInteger();
        mContext = context.getApplicationContext();
        mRunBus = new MerlinDispatcher[DEFAULT_NETWORK_THREAD_POOL_SIZE];
        for (int i = 0; i < DEFAULT_NETWORK_THREAD_POOL_SIZE; i++) {
            mRunBus[i] = new MerlinDispatcher(mWaitActions, TAG.concat("Merlin-WorkTask" + i));
            mRunBus[i].start();
        }
    }

    public long getRealexcuteTime() {
        return realexcuteTime;
    }

    public int getSequenceNumber() {
        return this.mSequenceGenerator.incrementAndGet();
    }

    protected Action mLastAction;

    protected final List<Action> mActions = new ArrayList<>(10);

    public Context getContext() {
        return mContext;
    }

    public BaseProvider cancelAllTask() {
        realexcuteTime = System.currentTimeMillis();
        final int length = mActions.size();
        Action action = null;
        for (int i = 0; i < length; i++) {
            action = mActions.get(i);
            action.cancel();
        }
        mWaitActions.clear();
        mActions.clear();
        return this;
    }

    protected final void holdAction(Action action) {
        if (action == null) return;
        mActions.add(action);
        mLastAction = action;
    }

    public BaseProvider cancelTask(CancelableCallBack callBack) {
        callBack.setCanceled(true);
        return this;
    }

    protected abstract void onDestroy();

    public void destroy() {
        onDestroy();
        for (int i = 0; i < DEFAULT_NETWORK_THREAD_POOL_SIZE; i++) {
            mRunBus[i].quit();
            mRunBus[i] = null;
        }
        cancelAllTask();
        mLastAction.cancel();
        mLastAction = null;
        mContext = null;
    }

    public final void addAction(Action action) {
        if (action == null) return;
        action.setSequence(getSequenceNumber());
        holdAction(action);
        try {
            mWaitActions.put(action);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class MerlinDispatcher extends Thread {

        private boolean mQuit;

        private String name;

        public void quit() {
            mQuit = true;
            interrupt();
        }

        private BlockingQueue<Action> mWaitActions = null;

        public MerlinDispatcher(BlockingQueue<Action> waitActions, String name) {
            mWaitActions = waitActions;
            this.name = name;
        }

        @Override
        public void run() {
            Log.e(TAG, ">>>>>>MerlinDispatcher#run : " + name + " start run! ");
            Action action;
            setPriority(Thread.MAX_PRIORITY);
            while (true) {
                if (mQuit) {
                    Log.e(TAG, ">>>>>>MerlinDispatcher#run : " + name + " end!");
                    break;
                }
                try {
                    action = mWaitActions.take();
                    if (action.isCanceled()) {
                        Log.e(TAG, ">>>>>>MerlinDispatcher#run : " + action.getName() + " is canceled");
                        continue;
                    }
                    Log.d(TAG, ">>>>>>MerlinDispatcher#run : " + action.getName() + " started");
                    action.run();

                } catch (InterruptedException e) {
                    Log.e(TAG, ">>>>>>MerlinDispatcher#run : " + e.getMessage());
                    continue;
                }
            }
        }
    }
}
