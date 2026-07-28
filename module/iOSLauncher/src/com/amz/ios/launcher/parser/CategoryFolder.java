package com.amz.ios.launcher.parser;

import android.content.ComponentName;
import android.content.Context;

import com.amz.ios.ioslite.common.CommonSdk;
import com.amz.ios.launcher.R;
import com.amz.ios.launcher.ShortcutInfo;

public class CategoryFolder {

    public static final int CATEGORY_FOLDER_GAME = 1;
    public static final int CATEGORY_FOLDER_ENTERTAINMENT = 2;
    public static final int CATEGORY_FOLDER_MEDIA = 3;
    public static final int CATEGORY_FOLDER_PHOTO = 4;
    public static final int CATEGORY_FOLDER_SOCIAL = 5;
    public static final int CATEGORY_FOLDER_NEWS_BOOKS = 6;
    public static final int CATEGORY_FOLDER_FINANCE_SHOPPING = 7;
    public static final int CATEGORY_FOLDER_TRAFFIC = 8;
    public static final int CATEGORY_FOLDER_LIFESTYPE = 9;
    public static final int CATEGORY_FOLDER_WORK_STUDY = 10;
    public static final int CATEGORY_FOLDER_TOOLS = 11;

    public static final int CATEGORY_FOLDER_SYSTEM = 0;
    public static final int CATEGORY_FOLDER_OTHER = 99;

    public static final int CATEGORY_FOLDER_DEFAULT = 100;


    private int folderType;
    private String folderName;

    public CategoryFolder(Context context, int type) {
        folderType = type;
        folderName = getFolerNameForType(context, folderType);
    }


    public String getFolderName() {
        return folderName;
    }

    public int getFolderType() {
        return folderType;
    }

    public static String getFolerNameForType(Context context, int type) {
        int resId;
        switch (type) {
            case CATEGORY_FOLDER_MEDIA:
                resId = R.string.category_folder_media;
                break;
            case CATEGORY_FOLDER_ENTERTAINMENT:
                resId = R.string.category_folder_entertainment;
                break;
            case CATEGORY_FOLDER_SOCIAL:
                resId = R.string.category_folder_social;
                break;
            case CATEGORY_FOLDER_NEWS_BOOKS:
                resId = R.string.category_folder_news_books;
                break;
            case CATEGORY_FOLDER_FINANCE_SHOPPING:
                resId = R.string.category_folder_finance_shopping;
                break;
            case CATEGORY_FOLDER_TRAFFIC:
                resId = R.string.category_folder_traffic;
                break;
            case CATEGORY_FOLDER_LIFESTYPE:
                resId = R.string.category_folder_lifestyle;
                break;
            case CATEGORY_FOLDER_GAME:
                resId = R.string.category_folder_game;
                break;
            case CATEGORY_FOLDER_TOOLS:
                resId = R.string.category_folder_tools;
                break;
            case CATEGORY_FOLDER_PHOTO:
                resId = R.string.category_folder_photo;
                break;
            case CATEGORY_FOLDER_WORK_STUDY:
                resId = R.string.category_folder_work_study;
                break;
            case CATEGORY_FOLDER_SYSTEM:
                resId = R.string.category_folder_system;
                break;
            case CATEGORY_FOLDER_OTHER:
                resId = R.string.category_folder_others;
                break;
            default:
                resId = R.string.category_folder_default;
                break;
        }
        return context.getString(resId);
    }

    public String toString() {
        return "folderName : " + folderName
                + ", folderType : " + folderType;
    }


    public static String getPredictFolderName(ShortcutInfo info) {
        Context context = CommonSdk.getApplicationContext();
        ComponentName cn = info.getTargetComponent();
        AppCategoryProvider provider = AppCategoryProvider.getInstance();
        int type = provider.getCatTypeForComp(cn);
        return getFolerNameForType(context, type);
    }
}
