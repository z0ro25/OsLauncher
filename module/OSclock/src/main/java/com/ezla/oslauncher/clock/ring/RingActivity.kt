package com.ezla.oslauncher.clock.ring

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.core.ClockMath
import com.ezla.oslauncher.clock.databinding.OsclockActivityRingBinding
import com.truongnt.ios.ioslite.common.IosLocale
import java.lang.ref.WeakReference
import java.util.Calendar

// Màn chuông full-screen (Figma "Alarm Ringing" / "Timer Done"): hiện cả trên màn khoá.
// Không kế thừa BaseActivity: không được có banner và không được ẩn khi locale đổi.
class RingActivity : AppCompatActivity() {

    private lateinit var b: OsclockActivityRingBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(IosLocale.wrapLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        showOverLockScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        b = OsclockActivityRingBinding.inflate(layoutInflater)
        setContentView(b.root)
        instance = WeakReference(this)
        // Back không tắt chuông (người dùng phải chọn Snooze / Stop).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
        bind(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        bind(intent)
    }

    override fun onDestroy() {
        if (instance?.get() === this) instance = null
        super.onDestroy()
    }

    private fun bind(intent: Intent) {
        val kind = intent.getIntExtra(RingService.EXTRA_KIND, RingService.KIND_ALARM)
        val id = intent.getIntExtra(RingService.EXTRA_ID, -1)
        val info = RingService.current ?: RingInfo.load(this, kind, id)
        if (info == null) {
            finish()
            return
        }
        if (intent.getBooleanExtra(EXTRA_START_SERVICE, false)) RingService.startFromActivity(this, info)
        if (info.kind == RingService.KIND_TIMER) {
            b.osclockRingTimerIcon.visibility = View.VISIBLE
            b.osclockRingTitle.text = info.title(this)
            b.osclockRingTitle.setTextAppearance(R.style.OsclockRingTimerTitle)
            b.osclockRingTime.text = info.subtitle(this)
            b.osclockRingTime.setTextAppearance(R.style.OsclockRingTimerSubtitle)
            b.osclockRingPrimary.setText(R.string.osclock_repeat)
            b.osclockRingPrimary.setOnClickListener { RingService.repeatTimer(this) }
        } else {
            b.osclockRingTimerIcon.visibility = View.GONE
            b.osclockRingTitle.text = info.title(this)
            val c = Calendar.getInstance()
            b.osclockRingTime.text = ClockMath.hhmm(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
            b.osclockRingPrimary.setText(R.string.osclock_snooze)
            b.osclockRingPrimary.visibility = if (info.snooze) View.VISIBLE else View.GONE
            b.osclockRingPrimary.setOnClickListener { RingService.snooze(this) }
        }
        b.osclockRingStop.setOnClickListener { RingService.dismiss(this) }
    }

    @Suppress("DEPRECATION")
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        private const val EXTRA_START_SERVICE = "start_service"
        private var instance: WeakReference<Activity>? = null

        fun intent(context: Context, info: RingInfo, startService: Boolean = false): Intent =
            Intent(context, RingActivity::class.java)
                .putExtra(RingService.EXTRA_KIND, info.kind)
                .putExtra(RingService.EXTRA_ID, info.id)
                .putExtra(EXTRA_START_SERVICE, startService)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION)

        fun finishAll() {
            instance?.get()?.let { if (!it.isFinishing) it.finish() }
            instance = null
        }
    }
}
