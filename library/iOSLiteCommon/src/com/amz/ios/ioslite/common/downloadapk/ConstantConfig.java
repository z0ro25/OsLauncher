package com.amz.ios.ioslite.common.downloadapk;

import java.util.HashMap;

/**
 * Created by server on 17-11-21.
 */

public class ConstantConfig {
    public static HashMap<String,DownLoadState> downLoadState = new HashMap<>();

    public enum  DownLoadState{
        LOADING,STOP
    }
}
