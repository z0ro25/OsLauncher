package com.amz.ios.themeclub.bean;

import java.io.Serializable;

/**
 * Created by server on 16-12-3.
 */

public class NativeWallpaperBean implements Serializable,Comparable<NativeWallpaperBean> {

    public String path;
    public Long lastModified;

    @Override
    public int compareTo(NativeWallpaperBean another) {
        if(another.lastModified > this.lastModified){
            return 1;
        }
        return -1;
    }
}
