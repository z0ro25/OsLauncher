package com.oslauncher.applauncher.themelauncher.Features.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.Features.home.HomeActivity
import com.oslauncher.applauncher.themelauncher.databinding.ActivityPermissionBinding
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils
import com.oslauncher.applauncher.themelauncher.utils.PermissionManager

class PermissionActivity : BaseActivity<ActivityPermissionBinding>() {
    override val setViewBinding: ActivityPermissionBinding
        get() = ActivityPermissionBinding.inflate(layoutInflater)

    // Kết quả xin quyền thông báo (Android 13+). Đăng ký ở field (trước RESUMED) theo yêu cầu API.
    private val notificationPermRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            binding.swNotification.isChecked = granted
            binding.swNotification.isEnabled = !granted
        }

    override fun initView() {
        PermissionManager.initLauncher(this)

        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()

        binding.apply {
            // External Storage: đã cấp -> ON + khóa; chưa -> OFF + cho bấm.
            val hasExternal = PermissionManager.isExternalPermission(this@PermissionActivity)
            swExternal.isChecked = hasExternal
            swExternal.isEnabled = !hasExternal

            // Notification: máy <13 không có quyền runtime -> coi như đã bật (ON + khóa).
            // Máy 13+ đọc trạng thái thật; đã cấp -> khóa, chưa -> cho bấm để xin.
            val hasNotification = isNotificationGranted()
            swNotification.isChecked = hasNotification
            swNotification.isEnabled =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotification
        }
    }

    override fun viewListener() {
        binding.apply {
            swExternal.setOnClickListener {
                if (!PermissionManager.isExternalPermission(this@PermissionActivity)) {
                    PermissionManager.requestExternalPermission(
                        this@PermissionActivity,
                        object : PermissionManager.PermissionResultListener {
                            override fun onDenied() {
                                super.onDenied()
                                swExternal.isChecked = false
                            }

                            override fun onAllow() {
                                super.onAllow()
                                swExternal.isChecked = true
                            }

                            override fun goSetting() {
                                super.goSetting()
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.parse("package:$packageName")
                                startActivity(intent)
                            }
                        })
                }
            }

            swNotification.setOnClickListener {
                // Chỉ máy 13+ mới tới đây (máy cũ toggle đã khóa). Bấm -> bung dialog xin quyền hệ thống.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationGranted()) {
                    notificationPermRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            btnComplete.setOnClickListener {
                SharePrefUtils.putBoolean(this@PermissionActivity, "PERMISSION_SHOWED", true)
                launchActivity<HomeActivity> { }
            }
        }
    }

    override fun dataObservable() {}

    /** Quyền hiển thị thông báo đã được cấp chưa. Máy <13 không có quyền runtime -> coi như đã có. */
    private fun isNotificationGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
