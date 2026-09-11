package com.ezla.oslauncher.Features.home

import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.ezla.oslauncher.Base.BaseActivity
import com.ezla.oslauncher.Features.appearance.AppearanceActivity
import com.ezla.oslauncher.Features.general.GeneralActivity
import com.ezla.oslauncher.Features.general.applibrary.AppLibraryActivity
import com.ezla.oslauncher.Features.general.changeicon.ChangeAppIconActivity
import com.ezla.oslauncher.Features.general.hiddenapp.HiddenAppActivity
import com.ezla.oslauncher.Features.general.renameapp.ChangeAppNameActivity
import com.ezla.oslauncher.Features.general.screengrid.ScreenGridActivity
import com.ezla.oslauncher.Features.general.transitionpage.PageTransitionActivity
import com.ezla.oslauncher.Features.lang.LanguageSettingActivity
import com.ezla.oslauncher.R
import com.ezla.oslauncher.databinding.ActivityHomeBinding
import com.ezla.oslauncher.dialog.SetDefaultLauncherDialog
import com.ezla.oslauncher.extensions.launchActivity
import com.ezla.oslauncher.extensions.tap
import com.ezla.oslauncher.tool.sharePreferenceTool.SharePrefUtils
import com.ezla.oslauncher.utils.PermissionManager
import com.truongnt.ios.launcher.searchlauncher.SearchLauncher


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
        binding.llSetDefaultCard.visibility =
            if (isDefault) android.view.View.GONE else android.view.View.VISIBLE
        binding.llSelectDefault.visibility =
            if (isDefault) android.view.View.VISIBLE else android.view.View.GONE
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
        val res: ResolveInfo? =
            packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
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
        binding.llRate.tap {
            showRateDialog(false) {

            }
        }
        binding.llMail.tap { sendFeedbackMail() }
        binding.llPrivacy.tap {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://docs.google.com/document/d/1MQhESaXwlgu5Gx9JWXSfXQMGBaaWCJBs-ochf5Cng3Y/edit?tab=t.0")
            )
            startActivity(browserIntent)
        }

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

    /**
     * Mở ứng dụng mail để người dùng gửi góp ý.
     *
     * Dùng ACTION_SENDTO + data "mailto:" thay cho ACTION_SEND: chỉ những app THẬT SỰ gửi mail mới
     * nhận intent này, nên danh sách chọn không lẫn Bluetooth/Drive/Zalo... như ACTION_SEND.
     *
     * Địa chỉ nhận + tiền tố tiêu đề lấy từ [SharePrefUtils] (nơi cấu hình chung của app).
     * Phần thân thư điền sẵn thông tin máy để đội hỗ trợ đỡ phải hỏi lại.
     */
    private fun sendFeedbackMail() {
        val recipients = listOf(SharePrefUtils.email, SharePrefUtils.email1)
            .filter { it.isNotBlank() }
            .toTypedArray()

        val subject = "${SharePrefUtils.subject}${getString(R.string.app_name)}"
        val body = buildString {
            append("\n\n---\n")
            append("Device ID: ").append(readAndroidId()).append('\n')
            append("Model: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
            append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
            append("App version: ").append(readAppVersion())
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            // Để trống phần sau "mailto:" và truyền người nhận qua EXTRA_EMAIL: cách này giữ được
            // dấu tiếng Việt / ký tự đặc biệt ở subject-body mà không phải tự encode URI.
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, recipients)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.Send_Email)))
        } catch (e: Exception) {
            // Máy không có app mail nào -> createChooser vẫn ném ActivityNotFoundException.
            Toast.makeText(this, getString(R.string.There_is_no), Toast.LENGTH_SHORT).show()
        }
    }

    /** Tên phiên bản app; trả chuỗi rỗng nếu không đọc được (không để crash vì việc phụ này). */
    private fun readAppVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
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
