package com.amz.ios.themeclub.model;


import com.amz.ios.themeclub.R;

import java.io.Serializable;

/**
 * Created by TUDOL on 10/18/2019.
 */

public class FontModel implements Serializable {
    public static int FONT_COUNT = 4;
    private Integer[] fontThumbResIds = {
            R.drawable.themeclub_login_icon,
            R.drawable.themeclub_login_icon,
            R.drawable.themeclub_login_icon,
            R.drawable.themeclub_login_icon,

    };

    private Integer[] fontDetailResIds = {
            R.drawable.themeclub_login_icon,
            R.drawable.themeclub_login_icon,
            R.drawable.themeclub_login_icon,
            R.drawable.themeclub_login_icon,
    };

    private int[] fontNames = {};

    private boolean mChecked;
    private int mType;

    private Integer thumbResId;
    private Integer detailResId;
    private int fontName;
    private int fontFile;

    public FontModel(int type){
        mType = type;
        thumbResId = fontThumbResIds[type];
        detailResId = fontDetailResIds[type];
        fontName = fontNames[type];
    }

    public boolean getChecked(){
        return mChecked;
    }

    public void setChecked(boolean checked){
        mChecked = checked;
    }
    public int getType(){
        return mType;
    }

    public Integer getThumbResId(){
        return thumbResId;
    }

    public Integer getDetailResId(){return  detailResId;}
    public int getFontName(){return  fontName;}

}
