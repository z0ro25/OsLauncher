package com.oslauncher.applauncher.themelauncher.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

object PermissionManager {

    var permission = ""
    var launcherSingle: ActivityResultLauncher<String>? = null
    var launcherMulti: ActivityResultLauncher<Array<String>>? = null
    var callBack: PermissionResultListener? = null
    var externalPermisison: Array<String> = arrayOf()

    //gọi hàm này để sử dụng quyền
    fun initLauncher(activity: AppCompatActivity) {
        activity.apply {
            launcherSingle = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    if (!it) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!shouldShowRequestPermissionRationale(permission)) {
                                callBack?.goSetting()
                            } else callBack?.onDenied()
                        } else callBack?.onDenied()
                    } else {
                        callBack?.onAllow()
                    }
                }

            launcherMulti =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                    if (result.all { it.value }.not()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (result.all { shouldShowRequestPermissionRationale(it.key) }.not()) {
                                callBack?.goSetting()
                            } else callBack?.onDenied()
                        } else callBack?.onDenied()
                    } else {
                        callBack?.onAllow()
                    }

                }
        }

    }

    //xin 1 quyền
    fun requestPermission(per: String, listener: PermissionResultListener) {
        permission = per
        launcherSingle?.launch(per)
        callBack = listener
    }

    //xin nhiều quền
    fun requestPermission(per: Array<String>, listener: PermissionResultListener) {
        launcherMulti?.launch(per)
        callBack = listener
    }

    fun isPermissionGranted(context: Context, per: Array<String>): Boolean {
        return per.all {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            } else true
        }
    }

    fun isPermissionGranted(context: Context, per: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(per) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun isExternalPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(
                context,
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isPermissionGranted(
                context,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        } else true
    }


    fun isExternalPermission(context: Context): Boolean {
        externalPermisison = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) else arrayOf()

        return isPermissionGranted(context, externalPermisison)
    }

    fun requestExternalPermission(context: Context, callback: PermissionResultListener) {
        if (!isExternalPermission(context)) {
            requestPermission(externalPermisison, callback)
        }
    }

    fun openAppSetting(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val packageName = context.packageName
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        launcher.launch(intent)
    }

    fun checkKeyboardEnable(context: Context?): Boolean {
        val enabledInputMethodList =
            (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).enabledInputMethodList
        var isEnabledKB = false
        val size = enabledInputMethodList.size
        for (i in 0 until size) {
            val inputMethodInfo = enabledInputMethodList[i]
            if (inputMethodInfo.packageName == context.getPackageName()) {
                isEnabledKB = true
            }
        }
        return isEnabledKB
    }

    fun checkIfDefaultKB(context: Context?) : Boolean {
        val enabledInputMethodList =
            (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).enabledInputMethodList
        var isDefaultKB = false
        val size = enabledInputMethodList.size
        for (i in 0 until size) {
            val inputMethodInfo = enabledInputMethodList[i]

            if (inputMethodInfo.id == Settings.Secure.getString(context.getContentResolver(), "default_input_method")) {
                isDefaultKB = inputMethodInfo.packageName == context.getPackageName()
            }
        }
        return isDefaultKB
    }

    interface PermissionResultListener {
        //người dùng từ chối quyền
        fun onDenied() {}

        //người dùng cấp quyền
        fun onAllow() {}

        //người dùng tắt quyền
        fun goSetting() {}
    }

}