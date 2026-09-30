package com.ezla.oslauncher.beautylauncher.Features.home

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.ezla.oslauncher.beautylauncher.Base.BaseActivity
import com.ezla.oslauncher.beautylauncher.Features.appearance.AppearanceActivity
import com.ezla.oslauncher.beautylauncher.Features.general.GeneralActivity
import com.ezla.oslauncher.beautylauncher.Features.general.applibrary.AppLibraryActivity
import com.ezla.oslauncher.beautylauncher.Features.general.changeicon.ChangeAppIconActivity
import com.ezla.oslauncher.beautylauncher.Features.general.hiddenapp.HiddenAppActivity
import com.ezla.oslauncher.beautylauncher.Features.general.renameapp.ChangeAppNameActivity
import com.ezla.oslauncher.beautylauncher.Features.general.screengrid.ScreenGridActivity
import com.ezla.oslauncher.beautylauncher.Features.general.transitionpage.PageTransitionActivity
import com.ezla.oslauncher.beautylauncher.Features.lang.LanguageSettingActivity
import com.ezla.oslauncher.beautylauncher.Features.subs.SubsAct
import com.ezla.oslauncher.beautylauncher.R
import com.ezla.oslauncher.beautylauncher.databinding.ActivityHomeBinding
import com.ezla.oslauncher.beautylauncher.dialog.SetDefaultLauncherDialog
import com.ezla.oslauncher.beautylauncher.extensions.launchActivity
import com.ezla.oslauncher.beautylauncher.extensions.tap
import com.ezla.oslauncher.beautylauncher.utils.BillingUtils
import com.ezla.oslauncher.beautylauncher.utils.PermissionManager
import com.ezt.v2.ezt.admobdemo.ads.InterAds
import com.ezt.v2.ezt.admobdemo.ads.NativeAds
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.truongnt.ios.ioslite.common.config.AppAds
import com.truongnt.ios.ioslite.common.config.SharePrefUtils
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

        applyDefaultLauncherState()

        maybeShowSetDefaultDialog()

        binding.apply {
            tvPro.isVisible = BillingUtils.isSubsCached(this@HomeActivity)
            tvPro.setTypeface(
                ResourcesCompat.getFont(
                    this@HomeActivity,
                    if (BillingUtils.isSubsCached(this@HomeActivity)) R.font.sf_pro_display_bold_italic else R.font.sf_pro_display_bold
                )
            )

            ivPro.isVisible = !BillingUtils.isSubsCached(this@HomeActivity)
            llSubs.isVisible = !BillingUtils.isSubsCached(this@HomeActivity)
        }

        if (!AdsSdk.isAdFree) {
            AppAds.kit.showNativeInline(
                this,
                "native_home",
                binding.frNativeHome,
                R.layout.layout_native_large
            )


        } else {
            binding.frNativeHome.isVisible = false
        }
    }

    override fun onResume() {
        super.onResume()
        // State default có thể đổi sau khi user quay lại từ màn chọn launcher hệ thống -> cập nhật lại.
        applyDefaultLauncherState()
        if (!AdsSdk.isAdFree) {
            AppAds.kit.showBanner(this, "main_banner", binding.frBanner, true)
        }
    }

    /**
     * Card "Set as default launcher" và dòng "Select Default Launcher" là CÙNG chức năng nhưng hiển thị NGƯỢC nhau:
     * chưa đặt app làm default -> hiện card (mời đặt); đã đặt xong -> ẩn card, hiện dòng trong list.
     */
    private fun applyDefaultLauncherState() {
        val isDefault = isDefaultLauncher()
        binding.llSetDefaultCard.visibility =
            if (isDefault) View.GONE else View.VISIBLE
        binding.llSelectDefault.visibility =
            if (isDefault) View.VISIBLE else View.GONE
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
        binding.llGoToLauncher.tap {
            tapWithInterInApp { goToLauncher() }
        }
        // Set as default launcher: bấm cả card hoặc nút "Set default" đều mở chọn launcher mặc định.
        binding.llSetDefaultCard.tap {
            tapWithInterInApp { selectDefaultLauncher() }
        }
        binding.btnSetDefault.tap {
            tapWithInterInApp { selectDefaultLauncher() }
        }

        // ===== Card chính (12 mục) =====
        binding.llGeneral.tap {
            tapWithInterInApp { launchActivity<GeneralActivity>() }
        }
        binding.llChangeAppIcon.tap {
            tapWithInterInApp { launchActivity<ChangeAppIconActivity>() }
        }
        binding.llHomescreenStyle.tap { /* TODO: chưa có màn Homescreen Style */ }
        binding.llScreenGrid.tap {
            tapWithInterInApp { launchActivity<ScreenGridActivity>() }
        }
        binding.llHiddenApps.tap {
            tapWithInterInApp { launchActivity<HiddenAppActivity>() }
        }
        binding.llPageTransition.tap {
            tapWithInterInApp { launchActivity<PageTransitionActivity>() }
        }
        binding.llAppLibrary.tap {
            tapWithInterInApp { launchActivity<AppLibraryActivity>() }
        }
        binding.llChangeAppName.tap {
            tapWithInterInApp { launchActivity<ChangeAppNameActivity>() }
        }
        binding.llBadgeNotifications.tap { /* TODO: chưa có màn Badge Notifications */ }
        binding.llLanguage.tap {
            tapWithInterInApp { launchActivity<LanguageSettingActivity>() }
        }
        binding.llAppearance.tap {
            tapWithInterInApp { launchActivity<AppearanceActivity>() }
        }
        binding.llSelectDefault.tap {
            selectDefaultLauncher()
        }

        binding.btnUpgrade.setOnClickListener {
            launchActivity<SubsAct>()
        }

        binding.ivPro.setOnClickListener {
            launchActivity<SubsAct>()
        }
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
    }

    override fun dataObservable() {}

    // Bọc [action] sau interstitial in-app. Bất biến: [action] luôn chạy đúng 1 lần — đã mua
    // bản không qc hoặc chưa có ad thì chạy ngay, có ad thì chạy khi ad đóng.
    private fun tapWithInterInApp(action: () -> Unit) {
        if (AdsSdk.isAdFree) {
            action()
            return
        }

        if (!InterAds.isCanShowAds()) {
            InterAds.initInterAds(this) {}
            action()
            return
        }

        InterAds.showAds(this, { action() }, true)
    }

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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestHomeRoleDialog(): Boolean {
        val roleManager = getSystemService(ROLE_SERVICE) as? RoleManager ?: return false
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


    private fun sendFeedbackMail() {
        val recipients = listOf(SharePrefUtils.email, SharePrefUtils.email1)
            .filter { it.isNotBlank() }
            .toTypedArray()

        val subject = "${SharePrefUtils.subject}${getString(R.string.app_name)}"
        val body = buildString {
            append("\n\n---\n")
            append("Model: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append('\n')
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

    companion object {
        /** Cờ persist: dialog "Set as default launcher" chỉ hiện 1 lần ở lần đầu vào màn Home. */
        private const val PREF_SET_DEFAULT_DIALOG_SHOWN = "set_default_dialog_shown"

        /** Cờ dùng-1-lần: bấm "Go to launcher" khi chưa default -> desktop hiện lại dialog Set default. */
        private const val PREF_PROMPT_SET_DEFAULT_ON_DESKTOP = "prompt_set_default_on_desktop"
    }
}
