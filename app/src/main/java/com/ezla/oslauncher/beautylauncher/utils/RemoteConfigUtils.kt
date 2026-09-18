package com.ezla.oslauncher.beautylauncher.utils

import android.content.Context
import com.ezla.oslauncher.beautylauncher.R
import com.ezla.oslauncher.beautylauncher.tool.sharePreferenceTool.SharePrefUtils

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object RemoteConfigUtils {


    fun initRemoteConfig(listener: OnCompleteListener<Boolean>) {
        val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        mFirebaseRemoteConfig.reset()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(listener)
    }

    private fun getRemoteConfigBoolean(adUnitId: String?): Boolean {
        val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        return mFirebaseRemoteConfig.getBoolean(adUnitId!!)
    }

    private fun getRemoteConfigLong(adUnitId: String?): Long {
        val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        return mFirebaseRemoteConfig.getLong(adUnitId!!)
    }

    private fun getRemoteConfigInt(adUnitId: String?): Int {
        val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        return mFirebaseRemoteConfig.getLong(adUnitId!!).toInt()
    }

    public fun getRemoteConfigString(adUnitId: String?): String {
        val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val `object` = mFirebaseRemoteConfig.getString(adUnitId!!)
        return `object`
    }

    private fun getRemoteConfigFloat(adUnitId: String?): Float {
        val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        return mFirebaseRemoteConfig.getDouble(adUnitId!!).toFloat()
    }

    fun saveRemoteConfigInt(context: Context, adUnitId: String?) {
        SharePrefUtils.putInteger(
            context,
            adUnitId,
            getRemoteConfigInt(adUnitId)
        )
    }

    fun saveRemoteConfigBoolean(context: Context, adUnitId: String?) {
        SharePrefUtils.putBoolean(
            context,
            adUnitId,
            getRemoteConfigBoolean(adUnitId)
        )
    }

    fun saveRemoteConfigLong(context: Context, adUnitId: String?) {
        SharePrefUtils.putlong(
            context,
            adUnitId,
            getRemoteConfigLong(adUnitId)
        )
    }

    fun saveRemoteConfigString(context: Context, adUnitId: String?) {
        SharePrefUtils.putString(
            context,
            adUnitId,
            getRemoteConfigString(adUnitId)
        )
    }

    fun saveRemoteConfigFloat(context: Context, adUnitId: String?) {
        SharePrefUtils.putFloat(
            context,
            adUnitId,
            getRemoteConfigFloat(adUnitId)
        )
    }

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
            getRemoteConfigLong(adUnitId)
        )
    }

    fun getRemoteConfigFloat(context: Context, adUnitId: String?): Float {
        return SharePrefUtils.getFloat(
            context,
            adUnitId,
            getRemoteConfigFloat(adUnitId)
        )
    }

    fun getRemoteConfigInt(context: Context, adUnitId: String?): Int {
        return SharePrefUtils.getInteger(
            context,
            adUnitId,
            getRemoteConfigInt(adUnitId)
        )
    }

    fun getRemoteConfigString(context: Context, adUnitId: String?): String {
        return SharePrefUtils.getString(
            context,
            adUnitId,
            getRemoteConfigString(adUnitId)
        )
    }

}
