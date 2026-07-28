package com.amz.ios.search.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.amz.ios.search.drawable.FlashDrawable;
import com.amz.ios.search.http.HotwordResponseBean;

import java.util.List;

/**
 * Created by liaozhongjun on 2017/2/15.
 */

public class FlashTextView extends TextView implements FlashDrawable.FlashEndListener {
    List<HotwordResponseBean.DataBean.WordsBean> words;
    private int mCurrentPos;

    public FlashTextView(Context context) {
        this(context, null);
    }

    public FlashTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlashTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setHotword(List<HotwordResponseBean.DataBean.WordsBean> words) {
        if (words == null || words.size() <= 0) {
            return;
        }
        this.words = words;
        mCurrentPos = 0;

    }

    public HotwordResponseBean.DataBean.WordsBean getCurrentHotword() {
        return words.get(mCurrentPos);
    }


    private HotwordResponseBean.DataBean.WordsBean nextWord() {
        if (words == null || words.size() <= 0) {
            return null;
        }
        mCurrentPos = (++mCurrentPos) % words.size();
        return words.get(mCurrentPos);
    }

    @Override
    public void onFlashEnd() {
        setText(nextWord().getTitle());

    }


}
