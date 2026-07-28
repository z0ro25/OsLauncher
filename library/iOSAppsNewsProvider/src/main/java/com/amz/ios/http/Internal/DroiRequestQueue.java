package com.amz.ios.http.Internal;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.amz.ios.ioslite.common.BuildConfig;
import com.amz.ios.ioslite.common.util.FileUtil;
import com.amz.ios.ioslite.common.util.NetworkStateUtil;
import com.amz.ios.http.BaseDroiRequest;


import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Author       : yizhihao
 * Create time  : 2016-11-11 下午2:12
 */
public class DroiRequestQueue {

    private static final String TAG = DroiRequestQueue.class.getSimpleName();

    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static final String UNIQUE_CACHE_FILE_NAME = "thumb";

    private static final AtomicReference<DroiRequestQueue> INSTANCE = new AtomicReference<DroiRequestQueue>();

    private RequestQueue mRequestQueue;

    private MerlinImageLoader mImageLoader;

    private Context mContext;

    public static DroiRequestQueue getInstance(Context context) {
        for (; ; ) {
            DroiRequestQueue current = INSTANCE.get();
            if (current != null) {
                return current;
            }
            current = new DroiRequestQueue(context);
            if (INSTANCE.compareAndSet(null, current)) {
                return current;
            }
        }
    }

    private DroiRequestQueue(Context context) {
        // 获取图片缓存路径
        File cacheDir = FileUtil.getDiskCacheDir(context, UNIQUE_CACHE_FILE_NAME);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        // 创建DiskLruCache实例，初始化缓存数据
        this.mContext = context.getApplicationContext();
//        OkHttpClient okHttpClient = new OkHttpClient();
//        okHttpClient.interceptors().add(CACH_INTERCEPTER);
//        OkHttpStack okHttpstack = new OkHttpStack(okHttpClient);
//        mRequestQueue = Volley.newRequestQueue(mContext, okHttpstack);
//        mRequestQueue.addRequestFinishedListener(new GlobleRequstFinishedListener());
//        mImageLoader = new MerlinImageLoader(mRequestQueue, new BitmapLruImageCache((int) (Runtime.getRuntime().maxMemory() / 10)));
    }

    private Interceptor CACH_INTERCEPTER = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>> ");
            Log.e(TAG, ">>>>>>DroiRequestQueue#intercept :  new Request");
            Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>> ");
            Request request = chain.request();

            Response response = chain.proceed(request); //===========
            Response responseBuild;
            if (!TextUtils.isEmpty(response.header("Cache-Control"))) return response;
            // 有网络的时候从缓存1天后失效
            if (NetworkStateUtil.isNetworkConnected(mContext)) {
                int maxAge = 60 * 60 * 24;
                responseBuild = response.newBuilder()
                        .removeHeader("Pragma")
                        .removeHeader("Cache-Control")
                        .header("Cache-Control", "public, max-age=" + maxAge)
                        .build();
            } else {
                // 无网络缓存保存四周
                int maxStale = 60 * 60 * 24 * 7;
                responseBuild = response.newBuilder()
                        .removeHeader("Pragma")
                        .removeHeader("Cache-Control")
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .build();
            }
            return responseBuild;
        }
    };

    /**
     * 定义OkHttpStack
     *
     * @author yizhihao
     */
//    private static class OkHttpStack extends HurlStack {
//        private final OkUrlFactory okUrlFactory;
//
//        public OkHttpStack(OkHttpClient okHttpClient) {
//            this(new OkUrlFactory(okHttpClient));
//        }
//
//        public OkHttpStack() {
//            this(new OkUrlFactory(new OkHttpClient()));
//        }
//
//        public OkHttpStack(OkUrlFactory okUrlFactory) {
//            if (okUrlFactory == null) {
//                throw new NullPointerException("Client must not be null.");
//            }
//            this.okUrlFactory = okUrlFactory;
//        }
//
//        @Override
//        protected HttpURLConnection createConnection(URL url)
//                throws IOException {
//            return okUrlFactory.open(url);
//        }
//    }
//
//    //+++++++++++++++++++++++++++++++++++++++++
//    // delegate for imageloader start
//    //+++++++++++++++++++++++++++++++++++++++++
//    public boolean isCached(String requestUrl, int maxWidth, int maxHeight) {
//        return mImageLoader.isCached(requestUrl, maxWidth, maxHeight);
//    }
//
//    public boolean isCached(String requestUrl, int maxWidth, int maxHeight, ImageView.ScaleType scaleType) {
//        return mImageLoader.isCached(requestUrl, maxWidth, maxHeight, scaleType);
//    }
//
//    public ImageListener get(String requestUrl) {
//        ImageListener l = new ImageListener();
//        mImageLoader.get(requestUrl, l);
//        return l;
//    }
//
//    public void get(String requestUrl, DroiImageListener listener) {
//        mImageLoader.get(requestUrl, listener);
//    }
//
//    public class ImageListener extends DroiImageListener {
//
//        private WeakReference<ImageView> mImageView;
//
//        private int defaultResId;
//
//        public ImageListener defaultRes(int resId) {
//            this.defaultResId = resId;
//            return this;
//        }
//
//        @Override
//        public void onPreNetResponce(DroiImageContainer response) {
//            if (mImageView != null && mImageView.get() != null) {
//                mImageView.get().setImageResource(defaultResId);
//            }
//        }
//
//        public void into(ImageView imageView) {
//            this.mImageView = new WeakReference<ImageView>(imageView);
//        }
//
//        @Override
//        public void onResponse(DroiImageContainer response) {
//            if (response.getBitmap() != null && mImageView != null && mImageView.get() != null) {
//                mImageView.get().setImageBitmap(response.getBitmap());
//            }
//        }
//    }

    public void setBatchedResponseDelay(int newBatchedResponseDelayMs) {
        mImageLoader.setBatchedResponseDelay(newBatchedResponseDelayMs);
    }
    //+++++++++++++++++++++++++++++++++++++++++
    // delegate for imageloader end
    //+++++++++++++++++++++++++++++++++++++++++


    //+++++++++++++++++++++++++++++++++++++++++
    // delegate for RequestQuen start
    //+++++++++++++++++++++++++++++++++++++++++
    public void stop() {
        mRequestQueue.stop();
    }

    public int getSequenceNumber() {
        return mRequestQueue.getSequenceNumber();
    }

    public DroiCache getCache() {
        return new DroiCache(mRequestQueue.getCache());
    }

    public void cancelAll(RequestQueue.RequestFilter filter) {
        mRequestQueue.cancelAll(filter);
    }

    public void cancelAll(Object tag) {
        mRequestQueue.cancelAll(tag);
    }

    public void add(BaseDroiRequest request) {
        mRequestQueue.add(request);
    }

    public void add(BaseDroiRequest request, Object tag) {
        if (tag != null) {
            request.setTag(tag);
        }
        mRequestQueue.add(request);
    }
    //+++++++++++++++++++++++++++++++++++++++++
    // delegate for RequestQuen end
    //+++++++++++++++++++++++++++++++++++++++++

    /**
     * wrap listener
     */
    public static abstract class DroiImageListener implements ImageLoader.ImageListener {

        @Override
        public final void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
            final DroiImageContainer container = new DroiImageContainer(response);
            if (isImmediate && response.getBitmap() == null) onPreNetResponce(container);
            onResponse(new DroiImageContainer(response));
        }

        @Override
        public final void onErrorResponse(VolleyError error) {
            Log.e(TAG, ">>>>>>DroiImageListener#onErrorResponse : " + error);
            onErrorResponse(error.getClass().getSimpleName());
        }

        public void onPreNetResponce(DroiImageContainer response) {
        }

        public void onErrorResponse(String message) {
        }

        public abstract void onResponse(DroiImageContainer response);
    }

    public static class DroiImageContainer {

        private ImageLoader.ImageContainer mImageContainer;

        public DroiImageContainer(ImageLoader.ImageContainer imageContainer) {
            mImageContainer = imageContainer;
        }

        public void cancelRequest() {
            mImageContainer.cancelRequest();
        }

        public String getRequestUrl() {
            return mImageContainer.getRequestUrl();
        }

        public Bitmap getBitmap() {
            return mImageContainer.getBitmap();
        }
    }

    public interface CallBack<T> {

        void onSucess(T result);

        /**
         * @param message
         * @param type    for now no usage
         */
        void onFalure(String message, int type);

    }
}
