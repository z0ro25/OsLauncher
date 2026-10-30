package com.ezla.oslauncher.clock.alarm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.core.ClockMath
import com.ezla.oslauncher.clock.data.Alarm
import com.ezla.oslauncher.clock.data.ClockStore
import com.ezla.oslauncher.clock.databinding.OsclockSheetSleepBinding
import com.ezla.oslauncher.clock.ring.AlarmScheduler
import com.ezla.oslauncher.clock.ui.ClockSheet

// Figma "Sleep Schedule": Done = lưu báo thức dậy (hằng ngày) + giờ ngủ; giờ ngủ chỉ để hiển thị.
class SleepScheduleSheet : ClockSheet<OsclockSheetSleepBinding>() {

    interface Host {
        fun onSleepSaved(alarm: Alarm?)
    }

    private lateinit var store: ClockStore

    override fun inflate(inflater: LayoutInflater, container: ViewGroup?) =
        OsclockSheetSleepBinding.inflate(inflater, container, false)

    override fun bind() {
        store = ClockStore(requireContext())
        val existing = store.sleepAlarm()
        b.osclockSleepBar.osclockBarTitle.setText(R.string.osclock_full_schedule)
        b.osclockSleepBar.osclockBarRight.setText(R.string.osclock_done)
        b.osclockSleepBar.osclockBarLeft.setOnClickListener { dismiss() }
        b.osclockSleepBar.osclockBarRight.setOnClickListener { save() }
        b.osclockSleepOff.visibility = if (existing?.enabled == true) View.VISIBLE else View.GONE
        b.osclockSleepOff.setOnClickListener { turnOff() }
        b.osclockSleepDial.set(store.sleepBedMinutes, existing?.let { it.hour * 60 + it.minute } ?: DEFAULT_WAKE)
        b.osclockSleepDial.onChanged = { _, _ -> render() }
        render()
    }

    private fun render() {
        val bed = b.osclockSleepDial.bedMinutes
        val wake = b.osclockSleepDial.wakeMinutes
        b.osclockSleepBed.text = ClockMath.hhmm(bed / 60, bed % 60)
        b.osclockSleepWake.text = ClockMath.hhmm(wake / 60, wake % 60)
        val total = ClockMath.sleepMinutes(bed, wake)
        b.osclockSleepTotal.text = getString(R.string.osclock_hr_min, total / 60, total % 60)
        b.osclockSleepGoal.setText(if (total >= GOAL_MINUTES) R.string.osclock_sleep_goal_met else R.string.osclock_sleep_goal_short)
    }

    private fun save() {
        val wake = b.osclockSleepDial.wakeMinutes
        store.sleepBedMinutes = b.osclockSleepDial.bedMinutes
        val old = store.sleepAlarm()
        val alarm = (old ?: Alarm(id = store.nextId(), hour = 0, minute = 0, isSleep = true,
            label = getString(R.string.osclock_wake_up_label)))
            .copy(hour = wake / 60, minute = wake % 60, days = ClockMath.EVERY_DAY, enabled = true, snoozeUntil = 0)
        store.saveAlarm(alarm)
        AlarmScheduler.schedule(requireContext(), alarm)
        (parentFragment as? Host)?.onSleepSaved(alarm)
        dismiss()
    }

    private fun turnOff() {
        store.sleepAlarm()?.let {
            val off = it.copy(enabled = false, snoozeUntil = 0)
            store.saveAlarm(off)
            AlarmScheduler.cancelAlarm(requireContext(), off.id)
        }
        (parentFragment as? Host)?.onSleepSaved(null)
        dismiss()
    }

    companion object {
        const val TAG = "sleep_schedule"
        private const val DEFAULT_WAKE = 6 * 60 + 30
        private const val GOAL_MINUTES = 7 * 60
    }
}
