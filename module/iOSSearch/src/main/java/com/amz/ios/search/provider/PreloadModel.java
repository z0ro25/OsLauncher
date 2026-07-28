package com.amz.ios.search.provider;

import android.content.Context;
import android.os.HandlerThread;

import com.amz.ios.search.config.Urls;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-19 下午4:35
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class PreloadModel {

    private static final String TAG = PreloadModel.class.getSimpleName();

    private Context mContext;

    private static final AtomicReference<PreloadModel> INSTANCE = new AtomicReference<PreloadModel>();

    private HandlerThread mTaskBus;

    private String searchEngine = "";

    private String mkeyWordSeg = "";

    private PreloadModel(Context context) {
        this.mContext = context.getApplicationContext();
        searchEngine = Urls.DEFAULT_SEARCH_ENGINE;
        mkeyWordSeg = Urls.KEY_WORD_SEG;
        mTaskBus = new HandlerThread(TAG + "-thread");
    }

    public static PreloadModel getInstance(Context context) {
        for (; ; ) {
            PreloadModel current = INSTANCE.get();
            if (current != null) {
                return current;
            }
            current = new PreloadModel(context);
            if (INSTANCE.compareAndSet(null, current)) {
                return current;
            }
        }
    }

    /**
     * do some preload
     */
    public void preload() {
        preLoadFromNet();
        preLoadFromLocal();
    }

    private void preLoadFromLocal() {
    }

    private void preLoadFromNet() {
    }

    public String getDefaultSearchEngine() {
        return searchEngine;
    }

    public String getKeyWordSeg() {
        return mkeyWordSeg;
    }
}
