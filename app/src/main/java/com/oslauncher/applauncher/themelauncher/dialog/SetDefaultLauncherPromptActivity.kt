package com.oslauncher.applauncher.themelauncher.dialog

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity TRONG SUỐT hiển thị dialog "Set as default launcher" NGAY TRÊN desktop launcher,
 * khi user bấm "Go to launcher" ở màn Home nhưng app CHƯA là launcher mặc định.
 * SearchLauncher (engine) mở activity này qua ComponentName (không phụ thuộc ngược :app).
 * "Set default" -> bung dialog ROLE_HOME của hệ thống; đóng dialog -> finish, trở lại desktop.
 */
class SetDefaultLauncherPromptActivity : AppCompatActivity() {

    // Sau khi user thao tác với dialog ROLE_HOME của hệ thống thì đóng activity, trả về desktop.
    private val roleRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SetDefaultLauncherDialog(this).apply {
            onSetDefault = { requestDefaultLauncher() }
            setOnDismissListener { finish() }
        }.show()
    }

    /** Bung dialog "đặt làm Màn hình chính mặc định" của hệ thống; fallback màn cài đặt Home. */
    private fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !rm.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                try {
                    roleRequest.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        // Máy cũ / không hỗ trợ role: mở màn cài đặt Home của hệ thống.
        // Settings.ACTION_HOME_SETTINGS có từ API 24 -> dùng cho MỌI máy (kể cả Android 9). Trước
        // đây máy < API 32 rơi vào ACTION_MAIN+CATEGORY_HOME nên "không hiện gì".
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            } catch (ignored: Exception) {
            }
        }
    }
}
