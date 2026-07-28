package com.amz.ios.search.utils;

import android.content.Context;
import android.text.TextUtils;

import com.amz.ios.ioslite.common.util.AlphabeticIndexCompat;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Author       : yizhihao
 * Create time  : 2016-11-23 上午11:43
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class PinyinUtils {

    private static final AtomicReference<PinyinUtils> INSTANCE = new AtomicReference<PinyinUtils>();

    private Context mContext;

    private AlphabeticIndexCompat mIndexCompat;

    private PinyinUtils(Context context) {
        this.mContext = context.getApplicationContext();
        mIndexCompat = new AlphabeticIndexCompat(context);
    }

    public static PinyinUtils getInstance(Context context) {
        for (; ; ) {
            PinyinUtils current = INSTANCE.get();
            if (current != null) {
                return current;
            }
            current = new PinyinUtils(context);
            if (INSTANCE.compareAndSet(null, current)) {
                return current;
            }
        }
    }


    public String getAlpha(String word) {
        return mIndexCompat.computeSectionName(word);
    }

    /**
     * @param chinese
     * @return
     */
    public String getAlphasForChinese(String chinese) {
        final StringBuilder sb = new StringBuilder();
        final int size = chinese.length();
        String indexStr;
        for (int i = 0; i < size; i++) {
            indexStr = mIndexCompat.computeSectionName(String.valueOf(chinese.charAt((i))));
            sb.append(indexStr);
        }
        return sb.toString();
    }

    /**
     * whether first letter matched
     *
     * @param name
     * @param search
     * @return
     */
    public boolean contains(String name, String search) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(search)) {
            return false;
        }
        boolean flag = false;
        if (isAllPinYin(search)) {
            String firstLetters = getAlphasForChinese(name);
            try {
                Pattern firstLetterMatcher = Pattern.compile(search, Pattern.CASE_INSENSITIVE);
                flag = firstLetterMatcher.matcher(firstLetters).find();
            } catch (PatternSyntaxException e) {
                e.printStackTrace();
            }
        } else {
            flag = name.contains(search);
        }
        return flag;
    }

    public boolean isAllPinYin(String chinese) {
        char[] array = chinese.toCharArray();
        for (char c : array) {
            if (IsCharChinese(c)) {
                return false;
            }
        }
        return true;
    }

    public boolean IsCharChinese(char c) {
        if (0x4e00 < c && c < 0x9fa5) {
            return true;
        }
        return false;
    }
}
