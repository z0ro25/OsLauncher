package com.oslauncher.applauncher.themelauncher.Features.home

import android.app.role.RoleManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.amz.ios.launcher.searchlauncher.SearchLauncher
import com.oslauncher.applauncher.themelauncher.Base.BaseActivity
import com.oslauncher.applauncher.themelauncher.R
import com.oslauncher.applauncher.themelauncher.Features.general.GeneralActivity
import com.oslauncher.applauncher.themelauncher.Features.general.applibrary.AppLibraryActivity
import com.oslauncher.applauncher.themelauncher.Features.general.changeicon.ChangeAppIconActivity
import com.oslauncher.applauncher.themelauncher.Features.general.hiddenapp.HiddenAppActivity
import com.oslauncher.applauncher.themelauncher.Features.general.renameapp.ChangeAppNameActivity
import com.oslauncher.applauncher.themelauncher.Features.general.screengrid.ScreenGridActivity
import com.oslauncher.applauncher.themelauncher.Features.appearance.AppearanceActivity
import com.oslauncher.applauncher.themelauncher.Features.general.transitionpage.PageTransitionActivity
import com.oslauncher.applauncher.themelauncher.Features.lang.LanguageSettingActivity
import com.oslauncher.applauncher.themelauncher.databinding.ActivityHomeBinding
import com.oslauncher.applauncher.themelauncher.dialog.SetDefaultLauncherDialog
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

        applyDefaultLauncherState()

        maybeShowSetDefaultDialog()
    }

    override fun onResume() {
        super.onResume()
        // State default có thể đổi sau khi user quay lại từ màn chọn launcher hệ thống -> cập nhật lại.
        applyDefaultLauncherState()
    }

    /**
     * Card "Set as default launcher" và dòng "Select Default Launcher" là CÙNG chức năng nhưng hiển thị NGƯỢC nhau:
     * chưa đặt app làm default -> hiện card (mời đặt); đã đặt xong -> ẩn card, hiện dòng trong list.
     */
    private fun applyDefaultLauncherState() {
        val isDefault = isDefaultLauncher()
        binding.llSetDefaultCard.visibility = if (isDefault) android.view.View.GONE else android.view.View.VISIBLE
        binding.llSelectDefault.visibility = if (isDefault) android.view.View.VISIBLE else android.view.View.GONE
//        binding.dividerSelectDefault.visibility = if (isDefault) android.view.View.VISIBLE else android.view.View.GONE
    }

    /**
     * Lần ĐẦU vào màn Home: hiện dialog gợi ý đặt app làm launcher mặc định (Figma node 217:2459).
     * Chỉ hiện 1 lần (cờ persist) và bỏ qua nếu app đã là default (khi đó gợi ý không còn ý nghĩa).
     */
    private fun maybeShowSetDefaultDialog() {
        if (SharePrefUtils.getBoolean(this, PREF_SET_DEFAULT_DIALOG_SHOWN)) return
        SharePrefUtils.putBoolean(this, PREF_SET_DEFAULT_DIALOG_SHOWN, true)
        if (isDefaultLauncher()) return
        SetDefaultLauncherDialog(this).apply {
            onSetDefault = { selectDefaultLauncher() }
        }.show()
    }

    /**
     * App hiện có đang là launcher mặc định không. Resolve HOME intent rồi so package -> đúng ở MỌI API
     * (kể cả < Q, nơi RoleManager không tồn tại nên cách cũ luôn trả false dù app ĐÃ là default -> vẫn nhắc dialog).
     */
    private fun isDefaultLauncher(): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res: ResolveInfo? = packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return res?.activityInfo?.packageName == packageName
    }

    override fun viewListener() {
        // ===== 2 card đầu màn =====
        // Go to launcher: đã là default -> vào thẳng desktop + thoát app; chưa default -> xem trải
        // nghiệm bình thường (qua màn Hello) và nhắc lại dialog Set default ngay khi tới desktop.
        binding.llGoToLauncher.tap { goToLauncher() }
        // Set as default launcher: bấm cả card hoặc nút "Set default" đều mở chọn launcher mặc định.
        binding.llSetDefaultCard.tap { selectDefaultLauncher() }
        binding.btnSetDefault.tap { selectDefaultLauncher() }

        // ===== Card chính (12 mục) =====
        binding.llGeneral.tap { launchActivity<GeneralActivity>() }
        binding.llChangeAppIcon.tap { launchActivity<ChangeAppIconActivity>() }
        binding.llHomescreenStyle.tap { /* TODO: chưa có màn Homescreen Style */ }
        binding.llScreenGrid.tap { launchActivity<ScreenGridActivity>() }
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

    /**
     * Mở desktop launcher từ card "Go to launcher".
     * - Đã là default: vào thẳng desktop và thoát app (chỉ finish app khi đã là default).
     * - Chưa default: đi qua màn Hello (trải nghiệm bình thường), KHÔNG finish app; đặt cờ để
     *   khi tới desktop hiện lại dialog "Set as default launcher". [SearchLauncher] đọc 2 cờ này.
     */
    private fun goToLauncher() {
        if (isDefaultLauncher()) {
            launchActivity<SearchLauncher>()
            finishAffinity()
        } else {
            SharePrefUtils.putBoolean(this, "hello_pending", true)
            SharePrefUtils.putBoolean(this, PREF_PROMPT_SET_DEFAULT_ON_DESKTOP, true)
            launchActivity<SearchLauncher>()
        }
    }

    /** Nhận kết quả dialog ROLE_HOME của hệ thống. Hệ thống tự áp default nên không cần xử lý thêm. */
    private val defaultLauncherRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* no-op */ }

    private fun selectDefaultLauncher() {
        // Đánh dấu: user vừa đi chọn default launcher -> lần đầu SearchLauncher khởi động
        // (sau khi đã set default) sẽ hiện màn Hello 1 lần rồi mới vào desktop. Xem Launcher.onCreate.
        SharePrefUtils.putBoolean(this, "hello_pending", true)
        SharePrefUtils.putBoolean(this, "is_login", false)
        // Android 10+ : bung DIALOG chọn launcher mặc định của hệ thống (RoleManager ROLE_HOME).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && requestHomeRoleDialog()) return
        // Máy cũ / không hỗ trợ role / đã là default: mở màn cài đặt Home của hệ thống.
        selectDefault()
    }

    /**
     * Hiện dialog "đặt làm app Màn hình chính mặc định" của hệ thống qua [RoleManager.ROLE_HOME].
     * @return true nếu đã bung được dialog; false khi thiết bị không hỗ trợ hoặc app đã là default
     *         (khi đó caller tự fallback sang màn cài đặt Home).
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestHomeRoleDialog(): Boolean {
        val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager ?: return false
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) return false
        if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) return false
        return try {
            defaultLauncherRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun selectDefault() {
        // Settings.ACTION_HOME_SETTINGS có từ API 24 -> mở thẳng trang chọn "App Màn hình chính
        // mặc định" của hệ thống trên MỌI máy (kể cả Android 9). Trước đây máy < API 32 rơi vào
        // ACTION_MAIN+CATEGORY_HOME: gọi từ trong chính launcher chỉ đưa app lên lại -> "không hiện gì".
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            // Máy hiếm không có trang Home settings: fallback về HOME intent để hệ thống tự xử lý.
            try {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            } catch (ignored: Exception) {
            }
        }
        SharePrefUtils.putBoolean(this, "is_login", false)
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

    companion object {
        /** Cờ persist: dialog "Set as default launcher" chỉ hiện 1 lần ở lần đầu vào màn Home. */
        private const val PREF_SET_DEFAULT_DIALOG_SHOWN = "set_default_dialog_shown"

        /** Cờ dùng-1-lần: bấm "Go to launcher" khi chưa default -> desktop hiện lại dialog Set default. */
        private const val PREF_PROMPT_SET_DEFAULT_ON_DESKTOP = "prompt_set_default_on_desktop"
    }
}
