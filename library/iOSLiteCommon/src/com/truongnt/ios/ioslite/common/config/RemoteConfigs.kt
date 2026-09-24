package com.truongnt.ios.ioslite.common.config

import android.content.Context
import com.truongnt.ios.ioslite.common.CommonSdk
import com.truongnt.ios.ioslite.common.ads.Ads

object RemoteConfigs {

    private const val TAG = "RemoteConfigs"

    const val daily_noti_time = "daily_noti_time"
    const val ADS_ENABLE = "ads_enable"
    const val INTER_SPLASH = "Inter_Splash"
    const val INTER_INAPP = "Inter_InApp"
    const val INTER_OPEN_APP = "Inter_Open_App"
    const val INTER_NATIVE_FULL = "Inter_Native_full"
    const val INTER_INTERVAL = "interval_between_interstitial"

    const val NATIVE_LANG1 = "Native_Lang1"
    const val NATIVE_LANG2 = "Native_Lang2"
    const val NATIVE_ONB1 = "Native_Onb1"
    const val NATIVE_ONB2 = "Native_Onb2"
    const val NATIVE_ONB3 = "Native_Onb3"
    const val NATIVE_ONB4 = "Native_Onb4"
    const val NATIVE_ONB_FULL = "Native_Onb_Full"
    const val NATIVE_CLP = "Native_CLP"
    const val NATIVE_PERMISSION = "Native_Permission"
    const val NATIVE_APP_LIBRARY = "Native_AppLibrary"
    const val NATIVE_APP_SEARCH = "Native_AppSearch"
    const val NATIVE_LEFT_PAGE = "Native_LeftPage"
    const val NATIVE_FULL = "Native_Dialog_Full"
    const val NATIVE_INAPP = "Native_In_App"

    const val REWARD_INAPP = "Reward_Inapp"

    const val BANNER_CLP = "Banner_Clp"
    const val BANNER_INAPP = "Banner_InApp"

    /** Native ở 3 màn ThemeClub (theme detail, wallpaper detail, selection wallpaper). */
    const val NATIVE_THEME_CLUB = "Native_ThemeClub"

    const val RESUME = "RESUME"



    fun getRemoteConfigBoolean(context: Context, adUnitId: String?): Boolean {
        return SharePrefUtils.getBoolean(
            context,
            adUnitId,
            true
        )
    }

    fun getRemoteConfigLong(context: Context, adUnitId: String?): Long {
        return SharePrefUtils.getLong(
            context,
            adUnitId,
            0L
        )
    }

    fun getRemoteConfigFloat(context: Context, adUnitId: String?): Float {
        return SharePrefUtils.getFloat(
            context,
            adUnitId,
            0F
        )
    }

    fun getRemoteConfigInt(context: Context, adUnitId: String?): Int {
        return SharePrefUtils.getInteger(
            context,
            adUnitId,
            0
        )
    }

    fun getRemoteConfigString(context: Context, adUnitId: String?): String {
        return SharePrefUtils.getString(
            context,
            adUnitId,
            ""
        )
    }

    @JvmStatic
    fun isAdsMasterOn(context: Context): Boolean =
        getRemoteConfigBoolean(context, ADS_ENABLE)


    @JvmStatic
    fun isAdsEnabled(context: Context, key: String?): Boolean =
        !Ads.isPremium() && isAdsMasterOn(context) && getRemoteConfigBoolean(context, key)

    @JvmStatic
    fun isAdsEnabled(key: String?): Boolean {
        val context = CommonSdk.getApplicationContext() ?: return true
        return isAdsEnabled(context, key)
    }
}
