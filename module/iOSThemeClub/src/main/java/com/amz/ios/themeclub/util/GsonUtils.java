package com.amz.ios.themeclub.util;/**
 * Author       : yizhihao
 * Create time  : 2016-07-14 下午3:16
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * manager gson instance
 * Author       : yizhihao
 * Create time  : 2016-07-14 下午3:16
 */
public class GsonUtils {

    private static Map<String,Gson> gsonPool;

    public static Gson getGsonInstance(){
        final String threadName = Thread.currentThread().getName();
        Gson instance = null;
        if(gsonPool == null){
            gsonPool = new HashMap<String,Gson>();
        }
        instance = gsonPool.get(threadName);
        if(instance == null){
            instance = createGsonInstance();
            gsonPool.put(threadName,instance);
        }
        return instance;
    }

    private static Gson createGsonInstance(){
        //注意这里的Gson的构建方式为GsonBuilder,区别于test1中的Gson gson = new Gson();
        Gson gson = new GsonBuilder()
//                .excludeFieldsWithoutExposeAnnotation() //不导出实体中没有用@Expose注解的属性
                .enableComplexMapKeySerialization() //支持Map的key为复杂对象的形式
                .serializeNulls().setDateFormat("yyyy-MM-dd HH:mm:ss:SSS")//时间转化为特定格式
//                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)//会把字段首字母大写,注:对于实体上使用了@SerializedName注解的不会生效.
//                .setPrettyPrinting() //对json结果格式化.
//                .setVersion(1.0)    //有的字段不是一开始就有的,会随着版本的升级添加进来,那么在进行序列化和返序列化的时候就会根据版本号来选择是否要序列化.
                //@Since(版本号)能完美地实现这个功能.还的字段可能,随着版本的升级而删除,那么
                //@Until(版本号)也能实现这个功能,GsonBuilder.setVersion(double)方法需要调用.
                .create();
        return gson;
    }

    /**
     <p>call this in doinbackgroud before return.</p>

     <code>

         new AsyncTask<Void,RspOrderHistoryInfo,RspOrderHistoryInfo>(){
             protected RspOrderHistoryInfo doInBackground(Void... params) {
                 RqstBuyHistoryInfo info = new RqstBuyHistoryInfo();
                 info.from = 0;
                 info.to   = 999;
                 try {
                     Gson gson = GsonUtils.getGsonInstance();
                     String result = NetworkUtil.accessNetwork(Url.getBaseUrl(),gson.toJson(info.buildSign()),"POST");
                     RspOrderHistoryInfo rspInfo = gson.fromJson(result,RspOrderHistoryInfo.class);
                     GsonUtils.releaseAsnycTaskGsonInstance();
                     return rspInfo;
                 } catch (IOException e) {
                    e.printStackTrace();
                 }
                 return null;
             }

            @Override
            protected void onPostExecute(RspOrderHistoryInfo aVoid) {
                super.onPostExecute(aVoid);
                HideProgressBar();
                initAdapter();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

     </code>

     */
    public static void releaseAsnycTaskGsonInstance(){
        final String threadName = Thread.currentThread().getName();
        if(gsonPool != null){
            gsonPool.remove(threadName);
        }
    }
}
