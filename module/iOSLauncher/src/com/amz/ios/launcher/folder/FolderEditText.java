package com.amz.ios.launcher.folder;

import android.content.Context;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;

/**
 * The edit text that reports back when the back key has been pressed.
 */
public class FolderEditText extends AppCompatEditText {

    private Folder mFolder;
    /**
     * Implemented by listeners of the back key.
     */
    public interface OnBackKeyListener {
        boolean onBackKey();
    }

    private OnBackKeyListener mBackKeyListener;

    public FolderEditText(Context context) {
        super(context);
        init();
    }

    public FolderEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FolderEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        updateFont();
    }

    public void updateFont() {
//        Themes.setTextViewTypeFace(getContext(), this);
    }
    public void setSpecialFont(int font_type){
//        Themes.setSpacialTypeFace(getContext(), font_type, this);
    }

    public void setOnBackKeyListener(OnBackKeyListener listener) {
        mBackKeyListener = listener;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // If this is a back key, propagate the key back to the listener
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (mBackKeyListener != null) {
                return mBackKeyListener.onBackKey();
            }
            return false;
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void setFolder(Folder baseFolder) {
        this.mFolder = baseFolder;
    }
}
