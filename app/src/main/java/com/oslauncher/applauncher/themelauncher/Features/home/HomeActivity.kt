package com.oslauncher.applauncher.themelauncher.Features.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.addCallback
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.Features.general.GeneralActivity
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.AppLibraryActivity
import com.oslauncher.applauncher.themelauncher.Features.general.changeicon.ChangeAppIconActivity
import com.oslauncher.applauncher.themelauncher.Features.general.hiddenapp.HiddenAppActivity
import com.oslauncher.applauncher.themelauncher.Features.general.renameapp.ChangeAppNameActivity
import com.oslauncher.applauncher.themelauncher.Features.appearance.AppearanceActivity
import com.oslauncher.applauncher.themelauncher.Features.general.transitionpage.PageTransitionActivity
import com.oslauncher.applauncher.themelauncher.Features.lang.LanguageSettingActivity
import com.oslauncher.applauncher.themelauncher.databinding.ActivityHomeBinding
import com.oslauncher.applauncher.themelauncher.extensions.launchActivity
import com.oslauncher.applauncher.themelauncher.extensions.tap
import com.oslauncher.applauncher.themelauncher.tool.sharePreferenceTool.SharePrefUtils
import com.oslauncher.applauncher.themelauncher.utils.PermissionManager


class HomeActivity : BaseActivity<ActivityHomeBinding>() {
    override val setViewBinding: ActivityHomeBinding
        get() = ActivityHomeBinding.inflate(layoutInflater)

    override fun initView() {
        SharePrefUtils.increaseCountOpenApp(this)

        onBackPressedDispatcher.addCallback {
            finishAffinity()
        }

        PermissionManager.initLauncher(this)

        binding.tvDeviceId.text = readAndroidId()
    }

    override fun viewListener() {
        // ===== Card chính (12 mục) =====
        binding.llGeneral.tap { launchActivity<GeneralActivity>() }
        binding.llChangeAppIcon.tap { launchActivity<ChangeAppIconActivity>() }
        binding.llHomescreenStyle.tap { /* TODO: chưa có màn Homescreen Style */ }
        binding.llScreenGrid.tap { /* TODO: chưa có màn Screen Grid */ }
        binding.llHiddenApps.tap { launchActivity<HiddenAppActivity>() }
        binding.llPageTransition.tap { launchActivity<PageTransitionActivity>() }
        binding.llAppLibrary.tap { launchActivity<AppLibraryActivity>() }
        binding.llChangeAppName.tap { launchActivity<ChangeAppNameActivity>() }
        binding.llBadgeNotifications.tap { /* TODO: chưa có màn Badge Notifications */ }
        binding.llLanguage.tap { launchActivity<LanguageSettingActivity>() }
        binding.llAppearance.tap { launchActivity<AppearanceActivity>() }
        binding.llSelectDefault.tap { selectDefaultLauncher() }

        // ===== App Function Settings =====
        binding.llLauncherAi.tap { /* TODO: chưa có màn Launcher AI */ }
        binding.llWeather.tap { /* TODO: chưa có màn Weather */ }

        // ===== Other =====
        binding.llRate.tap { /* TODO: chưa gắn Rate Our App */ }
        binding.llMail.tap { /* TODO: chưa gắn Mail To Us */ }
        binding.llPrivacy.tap { /* TODO: chưa gắn Privacy Policy */ }

        // ===== Device ID =====
        binding.ivCopyDeviceId.tap { copyDeviceId() }
    }

    override fun dataObservable() {}

    private fun selectDefaultLauncher() {
        // Đánh dấu: user vừa đi chọn default launcher -> lần đầu SearchLauncher khởi động
        // (sau khi đã set default) sẽ hiện màn Hello 1 lần rồi mới vào desktop. Xem Launcher.onCreate.
        SharePrefUtils.putBoolean(this, "hello_pending", true)
        if (getDefaultLauncherPackage().equals("android")) {
            selectDefault()
        } else {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
            SharePrefUtils.putBoolean(this, "is_login", false)
        }
    }

    fun selectDefault() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
        } else {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
        }
        SharePrefUtils.putBoolean(this, "is_login", false)
    }

    private fun getDefaultLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val packageManager = packageManager
        val resolveInfo: ResolveInfo? =
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

        return resolveInfo?.activityInfo?.packageName
    }

    @Suppress("HardwareIds")
    private fun readAndroidId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    }

    private fun copyDeviceId() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("device_id", readAndroidId()))
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }
}
