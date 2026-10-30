package com.ezla.oslauncher.clock.alarm

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.appbase.base.BaseFragment
import com.ezla.oslauncher.clock.ClockActivity
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.core.ClockMath
import com.ezla.oslauncher.clock.data.Alarm
import com.ezla.oslauncher.clock.data.ClockStore
import com.ezla.oslauncher.clock.databinding.OsclockFragmentListBinding
import com.ezla.oslauncher.clock.databinding.OsclockItemAlarmBinding
import com.ezla.oslauncher.clock.databinding.OsclockItemAlarmHeaderBinding
import com.ezla.oslauncher.clock.ring.AlarmScheduler

// Figma "Alarms" / "Alarms Edit" / "Alarm Saved". Hàng 0 = khối Sleep | Wake Up.
class AlarmsFragment : BaseFragment<OsclockFragmentListBinding>(), AlarmEditSheet.Host, SleepScheduleSheet.Host {

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        OsclockFragmentListBinding.inflate(inflater, container, false)

    private lateinit var store: ClockStore
    private val adapter = AlarmAdapter()

    override fun initView() {
        store = ClockStore(requireContext())
        binding.osclockHeader.osclockHeaderTitle.setText(R.string.osclock_tab_alarms)
        binding.osclockList.layoutManager = LinearLayoutManager(requireContext())
        binding.osclockList.adapter = adapter
        reload()
    }

    override fun viewListener() {
        binding.osclockHeader.osclockHeaderLeft.setOnClickListener {
            adapter.editMode = !adapter.editMode
            renderHeader()
        }
        binding.osclockHeader.osclockHeaderRight.setOnClickListener { openEditor(null) }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        reload() // báo thức một lần có thể vừa tự tắt sau khi kêu
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) reload() else if (adapter.editMode) {
            adapter.editMode = false
            renderHeader()
        }
    }

    private fun reload() {
        val list = store.alarms().filter { !it.isSleep }.sortedWith(compareBy({ it.hour }, { it.minute }, { it.id }))
        adapter.submit(list, store.sleepAlarm())
        renderHeader()
    }

    private fun renderHeader() {
        binding.osclockHeader.osclockHeaderLeft.setText(if (adapter.editMode) R.string.osclock_done else R.string.osclock_edit)
    }

    private fun openEditor(alarm: Alarm?) {
        if (childFragmentManager.findFragmentByTag(AlarmEditSheet.TAG) != null) return
        AlarmEditSheet.newInstance(alarm?.id ?: -1).show(childFragmentManager, AlarmEditSheet.TAG)
    }

    override fun onAlarmSaved(alarm: Alarm) {
        if (adapter.editMode) adapter.editMode = false
        reload()
        toastUntil(alarm)
    }

    override fun onAlarmDeleted() = reload()

    override fun onSleepSaved(alarm: Alarm?) {
        reload()
        alarm?.let { toastUntil(it) }
    }

    private fun toastUntil(alarm: Alarm) {
        val host = activity as? ClockActivity ?: return
        host.ensureAlarmPermissions()
        AlarmScheduler.nextTriggerOf(alarm)?.let {
            host.showToast(R.drawable.osclock_ic_tab_alarm, AlarmText.until(requireContext(), it))
        }
    }

    private fun setEnabled(a: Alarm, on: Boolean) {
        val updated = a.copy(enabled = on, snoozeUntil = 0)
        store.saveAlarm(updated)
        AlarmScheduler.schedule(requireContext(), updated)
        reload()
        if (on) toastUntil(updated)
    }

    private fun delete(a: Alarm) {
        AlarmScheduler.cancelAlarm(requireContext(), a.id)
        store.deleteAlarm(a.id)
        reload()
    }

    private inner class AlarmAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var items: List<Alarm> = emptyList()
        private var sleep: Alarm? = null
        var editMode = false
            @SuppressLint("NotifyDataSetChanged")
            set(v) { field = v; notifyDataSetChanged() }

        @SuppressLint("NotifyDataSetChanged")
        fun submit(list: List<Alarm>, sleepAlarm: Alarm?) {
            items = list
            sleep = sleepAlarm
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size + 1
        override fun getItemViewType(position: Int) = if (position == 0) TYPE_HEADER else TYPE_ALARM

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inf = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER) HeaderVH(OsclockItemAlarmHeaderBinding.inflate(inf, parent, false))
            else AlarmVH(OsclockItemAlarmBinding.inflate(inf, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is HeaderVH -> holder.bind()
                is AlarmVH -> holder.bind(items[position - 1])
            }
        }

        inner class HeaderVH(val v: OsclockItemAlarmHeaderBinding) : RecyclerView.ViewHolder(v.root) {
            fun bind() {
                val s = sleep
                v.osclockSleepSummary.text = if (s == null || !s.enabled) getString(R.string.osclock_no_alarm)
                else getString(
                    R.string.osclock_sleep_summary,
                    ClockMath.hhmm(store.sleepBedMinutes / 60, store.sleepBedMinutes % 60), ClockMath.hhmm(s.hour, s.minute)
                )
                v.osclockSleepChange.setOnClickListener {
                    if (childFragmentManager.findFragmentByTag(SleepScheduleSheet.TAG) == null) {
                        SleepScheduleSheet().show(childFragmentManager, SleepScheduleSheet.TAG)
                    }
                }
                v.osclockOtherTitle.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        inner class AlarmVH(val v: OsclockItemAlarmBinding) : RecyclerView.ViewHolder(v.root) {
            fun bind(a: Alarm) {
                val color = requireContext().getColor(if (a.enabled) R.color.osclock_text else R.color.osclock_gray)
                v.osclockAlarmTime.text = ClockMath.hhmm(a.hour, a.minute)
                v.osclockAlarmTime.setTextColor(color)
                v.osclockAlarmDesc.text = AlarmText.description(requireContext(), a)
                v.osclockAlarmDesc.setTextColor(color)
                v.osclockAlarmDelete.visibility = if (editMode) View.VISIBLE else View.GONE
                v.osclockAlarmChevron.visibility = if (editMode) View.VISIBLE else View.GONE
                v.osclockAlarmToggle.visibility = if (editMode) View.GONE else View.VISIBLE
                v.osclockAlarmToggle.setOn(a.enabled)
                v.osclockAlarmToggle.onChanged = { on -> setEnabled(a, on) }
                v.osclockAlarmDelete.setOnClickListener { delete(a) }
                // iOS: chỉ mở sửa khi đang Edit; ngoài Edit chạm hàng cũng mở (tiện hơn trên Android).
                v.root.setOnClickListener { openEditor(a) }
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ALARM = 1
    }
}
