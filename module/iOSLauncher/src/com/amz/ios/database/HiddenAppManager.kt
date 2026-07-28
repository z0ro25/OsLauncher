package com.amz.ios.database

import android.content.Context
import android.content.Intent
import com.amz.ios.launcher.AppInfo
import com.amz.ios.launcher.ItemInfo
import com.amz.ios.launcher.Launcher
import java.util.HashSet

object HiddenAppManager {
    private var database: HiddenDatabase? = null

    fun initDataBase(context: Context) {
        database = HiddenDatabase.createDataBase(context)
    }

    fun getAllHiddenApp() : List<ItemInfo>?{
        return database?.databaseDao()?.getAllHiddenApp()
    }

    fun notiFyUnHiddenApp(context: Context){
        val intent = Intent()
        intent.setAction("UPDATE_UNHIDDENAPP")
        context.sendBroadcast(intent)
    }

    fun hideApp(data: ItemInfo) {
        database?.databaseDao()?.hideApp(data)
    }

    fun unHideApp(data: ItemInfo) {
        database?.databaseDao()?.unHideApp(data)
    }

    fun isHidden(value : ItemInfo) : Boolean {
        val alldata = getAllHiddenApp()
        return alldata?.lastOrNull { it.packageName.trim() == value.targetComponent.packageName.trim() } != null
    }
}