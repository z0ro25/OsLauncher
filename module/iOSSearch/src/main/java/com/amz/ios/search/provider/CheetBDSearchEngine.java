package com.amz.ios.search.provider;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseArray;


/**
 * Created by Administrator on 2017/2/8.
 */

public class CheetBDSearchEngine implements ISearchEngine {
    public static final String YAHOO_SEARCH = "yahoo";

    private SparseArray<String> mMccMap = new SparseArray<String>();

    {
        /*mMccMap.put(310,"search.yahoo.com");
        mMccMap.put(311,"search.yahoo.com");
        mMccMap.put(312,"search.yahoo.com");
        mMccMap.put(313,"search.yahoo.com");
        mMccMap.put(314,"search.yahoo.com");
        mMccMap.put(315,"search.yahoo.com");
        mMccMap.put(316,"search.yahoo.com");
        mMccMap.put(330,"search.yahoo.com");
        mMccMap.put(332,"search.yahoo.com");
        mMccMap.put(334,"search.yahoo.com");
        mMccMap.put(335,"search.yahoo.com");
        mMccMap.put(344,"search.yahoo.com");*/
        mMccMap.put(722, "ar");
        mMccMap.put(232, "ar");
        mMccMap.put(724, "br");
        mMccMap.put(302, "ca");
        mMccMap.put(228, "ch");
        mMccMap.put(730, "cl");
        mMccMap.put(262, "de");
        mMccMap.put(238, "dk");
        mMccMap.put(288, "dk");

        mMccMap.put(290, "dk");
        mMccMap.put(214, "es");
        mMccMap.put(244, "fi");
        mMccMap.put(208, "fr");
        mMccMap.put(308, "fr");
        mMccMap.put(340, "fr");
        mMccMap.put(543, "fr");
        mMccMap.put(546, "fr");
        mMccMap.put(547, "fr");
        mMccMap.put(647, "fr");
        mMccMap.put(742, "fr");
        mMccMap.put(454, "hk");
        mMccMap.put(510, "id");
        mMccMap.put(404, "in");
        mMccMap.put(405, "in");
        mMccMap.put(406, "in");
        mMccMap.put(222, "it");
    }

    private Context mContext;

    public CheetBDSearchEngine(Context mContext) {
        this.mContext = mContext;
    }

    private StringBuilder mHostBuilder = null;

    public static final String HTTPS = "https://";
    public static final String CHEET_SEARCH_BODY = "search.yahoo.com";
    public static final String CHEET_SEARCH_URL = HTTPS + "%s/yhs/mobile/search?hspart=cheetah&hsimp=yhs-cheetah_055&type=%s&p=%s";

    @Override
    public String getSearchUrl(int action, String keyWord) {
        if (mHostBuilder == null) {
            mHostBuilder = new StringBuilder();
        }
        mHostBuilder.delete(0, mHostBuilder.length());

        //build host
        final String mcc = mMccMap.get(getMcc(mContext));
        if (!TextUtils.isEmpty(mcc)) {
            mHostBuilder.append(mcc).append(".");
        }
        mHostBuilder.append(CHEET_SEARCH_BODY);

        final String typeId = "2025100";

        return String.format(CHEET_SEARCH_URL, mHostBuilder.toString(), typeId, keyWord);
    }

    public int getMcc(Context context) {
        if (context != null) {
            try {
                final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                String mmc = tm.getSimOperator();
                if (!TextUtils.isEmpty(mmc)) {
                    if (mmc.length() >= 3) {
                        return Integer.valueOf(mmc.substring(0, 3));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }
}
