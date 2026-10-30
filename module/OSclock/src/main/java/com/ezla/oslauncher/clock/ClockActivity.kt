package com.ezla.oslauncher.clock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.ezla.oslauncher.appbase.base.BaseActivity
import com.ezla.oslauncher.clock.alarm.AlarmsFragment
import com.ezla.oslauncher.clock.databinding.OsclockActivityClockBinding
import com.ezla.oslauncher.clock.data.ClockStore
import com.ezla.oslauncher.clock.ring.AlarmScheduler
import com.ezla.oslauncher.clock.ring.TimerNotifier
import com.ezla.oslauncher.clock.stopwatch.StopwatchFragment
import com.ezla.oslauncher.clock.timer.TimersFragment
import com.ezla.oslauncher.clock.worldclock.WorldClockFragment

/** Đồng hồ kiểu iOS (Figma "15 · Clock"): World Clock / Alarms / Stopwatch / Timers. */
class ClockActivity : BaseActivity<OsclockActivityClockBinding>() {

    override val setViewBinding get() = OsclockActivityClockBinding.inflate(layoutInflater)

    private val b get() = binding
    private lateinit var store: ClockStore
    private val tabs by lazy { listOf(b.osclockTabWorld, b.osclockTabAlarms, b.osclockTabStopwatch, b.osclockTabTimers) }
    private var current = -1
    private val hideToast = Runnable {
        b.osclockToast.animate().alpha(0f).setDuration(200).withEndAction { b.osclockToast.visibility = View.GONE }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    // Mọi đường vào (icon desktop, ClockEntry) đều mở thẳng màn này: lần đầu chuyển sang onboarding.
    override fun redirectBeforeCreate(): Boolean {
        if (ClockEntry.isOnboardingDone(this)) return false
        startActivity(Intent(this, ClockOnboardingActivity::class.java))
        return true
    }

    override fun initView() {
        store = ClockStore(this)
        // Tab bar nổi trên thanh điều hướng (base chỉ pad top + bàn phím).
        ViewCompat.setOnApplyWindowInsetsListener(b.osclockTabbar) { v, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updateLayoutParams<android.widget.FrameLayout.LayoutParams> {
                bottomMargin = nav + resources.getDimensionPixelSize(R.dimen.osclock_tab_margin)
            }
            insets
        }
        select(intent.getIntExtra(EXTRA_TAB, store.lastTab))
    }

    override fun viewListener() {
        tabs.forEachIndexed { i, v -> v.setOnClickListener { select(i) } }
    }

    override fun dataObservable() {}

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra(EXTRA_TAB)) select(intent.getIntExtra(EXTRA_TAB, 0))
    }

    override fun onResume() {
        super.onResume()
        // Lịch có thể đã mất (cập nhật app khi chưa có receiver, force-stop...): đặt lại mỗi lần mở.
        AlarmScheduler.rescheduleAll(this)
        TimerNotifier.refresh(this)
    }

    private fun select(index: Int) {
        val i = index.coerceIn(0, 3)
        if (i == current) return
        current = i
        store.lastTab = i
        tabs.forEachIndexed { k, v -> v.isSelected = k == i }
        val tag = "tab$i"
        val fm = supportFragmentManager
        val tx = fm.beginTransaction()
        fm.fragments.filter { it.tag?.startsWith("tab") == true }.forEach { tx.hide(it) }
        val existing = fm.findFragmentByTag(tag)
        if (existing != null) tx.show(existing) else tx.add(R.id.osclock_container, newTab(i), tag)
        tx.commitAllowingStateLoss()
    }

    private fun newTab(i: Int): Fragment = when (i) {
        TAB_WORLD -> WorldClockFragment()
        TAB_ALARMS -> AlarmsFragment()
        TAB_STOPWATCH -> StopwatchFragment()
        else -> TimersFragment()
    }

    /** Toast kính giữa-đáy (Figma "Paris Added", "Alarm in 18 hr 4 min"). */
    fun showToast(iconRes: Int, text: CharSequence) {
        b.osclockToast.removeCallbacks(hideToast)
        b.osclockToastIcon.setImageResource(iconRes)
        b.osclockToastText.text = text
        b.osclockToast.visibility = View.VISIBLE
        b.osclockToast.animate().cancel()
        b.osclockToast.animate().alpha(1f).setDuration(180).start()
        b.osclockToast.postDelayed(hideToast, TOAST_MS)
    }

    /** Lần đầu bật báo thức / hẹn giờ: xin quyền thông báo (13+) và exact alarm (12+). */
    fun ensureAlarmPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        if (!AlarmScheduler.canExact(this) && !store.askedExact) {
            store.askedExact = true
            AlarmScheduler.exactAlarmSettingsIntent(this)?.let { runCatching { startActivity(it) } }
        }
    }

    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_WORLD = 0
        const val TAB_ALARMS = 1
        const val TAB_STOPWATCH = 2
        const val TAB_TIMERS = 3
        private const val TOAST_MS = 2200L
    }
}
