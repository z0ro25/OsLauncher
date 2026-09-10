//package com.ios.launcher;
//
//import android.content.Context;
//import android.util.AttributeSet;
//import android.view.KeyEvent;
//import android.widget.EditText;
//
//public class FolderEditText extends EditText
//{
//    private static final boolean DEBUG_DRAW = true;
//    private static final boolean DEBUG_ENABLE = false;
//    private static final String TAG = "FolderEditText";
//    public static long sBackPressCurTime = 0L;
//    private BaseFolder mFolder;
//
//    public FolderEditText(Context context)
//    {
//        super(context);
//    }
//
//    public FolderEditText(Context context, AttributeSet paramAttributeSet)
//    {
//        super(context, paramAttributeSet);
//    }
//
//    public FolderEditText(Context context, AttributeSet paramAttributeSet, int paramInt)
//    {
//        super(context, paramAttributeSet, paramInt);
//    }
//
//    public boolean onKeyPreIme(int paramInt, KeyEvent paramKeyEvent)
//    {
//        if (this.mFolder.isRecommendVisiable())
//            sBackPressCurTime = System.currentTimeMillis();
//        if (paramKeyEvent.getKeyCode() == 4)
//            this.mFolder.dismissEditingName();
//        for (boolean bool = true; ; bool = false)
//            return bool;
//    }
//
//    public void setEditFlag(int paramInt)
//    {
//    }
//
//    public void setFolder(BaseFolder paramBaseFolder)
//    {
//        this.mFolder = paramBaseFolder;
//    }
//}