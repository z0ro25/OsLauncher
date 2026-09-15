package com.amz.ios.http.Internal;

import com.android.volley.Cache;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-17 上午10:57
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class DroiCache implements Cache {

    public Cache mCache;

    public DroiCache(Cache cache) {
        mCache = cache;
    }

    @Override
    public Entry get(String key) {
        return mCache.get(key);
    }

    @Override
    public void put(String key, Entry entry) {
        mCache.put(key, entry);
    }

    @Override
    public void initialize() {
        mCache.initialize();
    }

    @Override
    public void invalidate(String key, boolean fullExpire) {
        mCache.invalidate(key, fullExpire);
    }

    @Override
    public void remove(String key) {
        mCache.remove(key);
    }

    @Override
    public void clear() {
        mCache.clear();
    }
}