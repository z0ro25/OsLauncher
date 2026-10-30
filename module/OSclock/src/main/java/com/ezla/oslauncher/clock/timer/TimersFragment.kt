package com.ezla.oslauncher.clock.timer

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.appbase.base.BaseFragment
import com.ezla.oslauncher.clock.ClockActivity
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.core.ClockMath
import com.ezla.oslauncher.clock.data.ClockStore
import com.ezla.oslauncher.clock.data.ClockTimer
import com.ezla.oslauncher.clock.databinding.OsclockFragmentTimersBinding
import com.ezla.oslauncher.clock.databinding.OsclockItemRecentBinding
import com.ezla.oslauncher.clock.databinding.OsclockItemTimerBinding
import com.ezla.oslauncher.clock.databinding.OsclockItemTimerNoteBinding
import com.ezla.oslauncher.clock.ring.AlarmScheduler
import com.ezla.oslauncher.clock.ring.ClockSounds
import com.ezla.oslauncher.clock.ring.TimerNotifier

// Figma "Timers" / "Timer Running" / "Timer Paused". Không có timer nào -> màn chọn thời lượng;
// có -> danh sách thẻ, nút "+" quay lại màn chọn. Hết giờ do AlarmManager + RingService lo.
class TimersFragment : BaseFragment<OsclockFragmentTimersBinding>(), TimerOptionSheet.Host {

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        OsclockFragmentTimersBinding.inflate(inflater, container, false)

    private lateinit var store: ClockStore
    private val timerAdapter = TimerAdapter()
    private val recentAdapter = RecentAdapter()
    private var adding = false
    private var editMode = false
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            timerAdapter.tick()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun initView() {
        store = ClockStore(requireContext())
        binding.osclockHeader.osclockHeaderTitle.setText(R.string.osclock_tab_timers)
        binding.osclockWheelH.items = (0..23).map { it.toString() }
        binding.osclockWheelM.items = (0..59).map { it.toString() }
        binding.osclockWheelS.items = (0..59).map { it.toString() }
        listOf(binding.osclockWheelH, binding.osclockWheelM, binding.osclockWheelS).forEach {
            it.align = 1
            it.onSelected = { renderStart() }
        }
        val d = store.pickerDurationMs / 1000
        binding.osclockWheelH.setSelected((d / 3600).toInt())
        binding.osclockWheelM.setSelected(((d / 60) % 60).toInt())
        binding.osclockWheelS.setSelected((d % 60).toInt())

        binding.osclockOptTimerLabel.osclockOptName.setText(R.string.osclock_label)
        binding.osclockOptTimerSound.osclockOptName.setText(R.string.osclock_when_timer_ends)
        binding.osclockOptTimerSound.osclockOptSeparator.visibility = View.INVISIBLE

        binding.osclockTimerList.layoutManager = LinearLayoutManager(requireContext())
        binding.osclockTimerList.adapter = timerAdapter
        binding.osclockRecents.layoutManager = LinearLayoutManager(requireContext())
        binding.osclockRecents.adapter = recentAdapter
        reload()
    }

    override fun viewListener() {
        binding.osclockTimerStart.setOnClickListener {
            val ms = pickerMs()
            if (ms > 0) start(ms)
        }
        binding.osclockTimerCancel.setOnClickListener {
            if (adding && store.timers().isNotEmpty()) {
                adding = false
                reload()
            }
        }
        binding.osclockOptTimerLabel.root.setOnClickListener { openOption(true) }
        binding.osclockOptTimerSound.root.setOnClickListener { openOption(false) }
        binding.osclockHeader.osclockHeaderRight.setOnClickListener {
            adding = true
            editMode = false
            reload()
        }
        binding.osclockHeader.osclockHeaderLeft.setOnClickListener {
            editMode = !editMode
            reload()
        }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        reload()
        handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        handler.removeCallbacks(tick)
        if (!hidden) {
            reload()
            handler.post(tick)
        }
    }

    override fun onTimerOptionsChanged() = renderOptions()

    private fun pickerMs(): Long =
        (binding.osclockWheelH.selected * 3600L + binding.osclockWheelM.selected * 60L + binding.osclockWheelS.selected) * 1000L

    private fun openOption(label: Boolean) {
        if (childFragmentManager.findFragmentByTag(TimerOptionSheet.TAG) == null) {
            TimerOptionSheet.newInstance(label).show(childFragmentManager, TimerOptionSheet.TAG)
        }
    }

    private fun start(durationMs: Long) {
        val end = System.currentTimeMillis() + durationMs
        val t = ClockTimer(
            id = store.nextId(), durationMs = durationMs, label = store.timerLabel,
            sound = store.timerSound, endAt = end, remainingMs = durationMs
        )
        store.saveTimer(t)
        store.addRecent(durationMs)
        store.pickerDurationMs = durationMs
        AlarmScheduler.scheduleTimer(requireContext(), t.id, end)
        TimerNotifier.refresh(requireContext())
        (activity as? ClockActivity)?.ensureAlarmPermissions()
        adding = false
        reload()
    }

    private fun pauseOrResume(t: ClockTimer) {
        val now = System.currentTimeMillis()
        val updated = if (t.running) {
            AlarmScheduler.cancelTimer(requireContext(), t.id)
            t.copy(endAt = 0L, remainingMs = t.remaining(now))
        } else {
            val end = now + t.remainingMs
            AlarmScheduler.scheduleTimer(requireContext(), t.id, end)
            t.copy(endAt = end)
        }
        store.saveTimer(updated)
        TimerNotifier.refresh(requireContext())
        reload()
    }

    private fun delete(t: ClockTimer) {
        AlarmScheduler.cancelTimer(requireContext(), t.id)
        store.deleteTimer(t.id)
        TimerNotifier.refresh(requireContext())
        reload()
    }

    private fun reload() {
        val now = System.currentTimeMillis()
        // Timer đã kêu xong (remaining 0, không chạy) mà RingService chưa xoá: dọn khỏi danh sách.
        val timers = store.timers().filter { it.running || it.remaining(now) > 0 }
        if (timers.isEmpty()) {
            adding = false
            editMode = false
        }
        val showList = timers.isNotEmpty() && !adding
        binding.osclockTimerList.visibility = if (showList) View.VISIBLE else View.GONE
        binding.osclockTimerSetup.visibility = if (showList) View.GONE else View.VISIBLE
        val header = binding.osclockHeader
        header.osclockHeaderLeft.visibility = if (showList) View.VISIBLE else View.INVISIBLE
        header.osclockHeaderLeft.setText(if (editMode) R.string.osclock_done else R.string.osclock_edit)
        header.osclockHeaderRight.visibility = if (showList) View.VISIBLE else View.GONE
        timerAdapter.submit(timers)
        recentAdapter.submit(store.recents())
        binding.osclockRecentsTitle.visibility = if (store.recents().isEmpty()) View.GONE else View.VISIBLE
        renderStart()
        renderOptions()
    }

    private fun renderStart() {
        val ok = pickerMs() > 0
        binding.osclockTimerStart.isEnabled = ok
        binding.osclockTimerStart.alpha = if (ok) 1f else 0.5f
        val cancelActive = adding && store.timers().isNotEmpty()
        binding.osclockTimerCancel.setTextColor(requireContext().getColor(if (cancelActive) R.color.osclock_text else R.color.osclock_gray))
        binding.osclockTimerCancel.setBackgroundResource(if (cancelActive) R.drawable.osclock_bg_circle_active else R.drawable.osclock_bg_circle_idle)
    }

    private fun renderOptions() {
        binding.osclockOptTimerLabel.osclockOptValue.text = store.timerLabel.ifBlank { getString(R.string.osclock_timer) }
        binding.osclockOptTimerSound.osclockOptValue.text = ClockSounds.shortTitle(requireContext(), store.timerSound)
    }

    private fun durationText(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total / 60) % 60
        val s = total % 60
        return listOfNotNull(
            if (h > 0) getString(R.string.osclock_n_hr, h) else null,
            if (m > 0) getString(R.string.osclock_n_min, m) else null,
            if (s > 0) getString(R.string.osclock_n_sec, s) else null
        ).joinToString(" ")
    }

    // Hàng cuối = chú thích "Timer also shows…" (Figma "Timer Running"), chỉ có khi đã có timer.
    private inner class TimerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var items: List<ClockTimer> = emptyList()

        @SuppressLint("NotifyDataSetChanged")
        fun submit(list: List<ClockTimer>) {
            items = list
            notifyDataSetChanged()
        }

        fun tick() {
            val now = System.currentTimeMillis()
            if (items.any { it.running && it.endAt <= now }) {
                reload() // vừa hết giờ
                return
            }
            if (items.any { it.running }) notifyItemRangeChanged(0, items.size, PAYLOAD)
        }

        override fun getItemCount() = if (items.isEmpty()) 0 else items.size + 1
        override fun getItemViewType(position: Int) = if (position < items.size) TYPE_TIMER else TYPE_NOTE

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inf = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_NOTE) NoteVH(OsclockItemTimerNoteBinding.inflate(inf, parent, false))
            else VH(OsclockItemTimerBinding.inflate(inf, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
            if (holder is VH && payloads.contains(PAYLOAD)) holder.bindTime(items[position]) else super.onBindViewHolder(holder, position, payloads)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder !is VH) return
            val t = items[position]
            val v = holder.v
            v.osclockTimerLabel.text = getString(
                R.string.osclock_timer_label_duration, t.label.ifBlank { getString(R.string.osclock_timer) }, durationText(t.durationMs)
            )
            v.osclockTimerToggle.setImageResource(if (t.running) R.drawable.osclock_ic_pause else R.drawable.osclock_ic_play)
            v.osclockTimerToggle.setBackgroundResource(if (t.running) R.drawable.osclock_bg_circle_orange else R.drawable.osclock_bg_circle_green_soft)
            v.osclockTimerToggle.contentDescription = getString(if (t.running) R.string.osclock_pause else R.string.osclock_resume)
            v.osclockTimerToggle.setOnClickListener { pauseOrResume(t) }
            v.osclockTimerDelete.visibility = if (editMode) View.VISIBLE else View.GONE
            v.osclockTimerDelete.setOnClickListener { delete(t) }
            holder.bindTime(t)
        }

        inner class NoteVH(v: OsclockItemTimerNoteBinding) : RecyclerView.ViewHolder(v.root)

        inner class VH(val v: OsclockItemTimerBinding) : RecyclerView.ViewHolder(v.root) {
            fun bindTime(t: ClockTimer) {
                val rem = t.remaining(System.currentTimeMillis())
                v.osclockTimerRemaining.text = ClockMath.countdown(rem)
                v.osclockTimerRing.fraction = if (t.durationMs > 0) rem.toFloat() / t.durationMs else 0f
            }
        }
    }

    private inner class RecentAdapter : RecyclerView.Adapter<RecentAdapter.VH>() {
        private var items: List<Long> = emptyList()

        @SuppressLint("NotifyDataSetChanged")
        fun submit(list: List<Long>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(OsclockItemRecentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val ms = items[position]
            holder.v.osclockRecentTime.text = ClockMath.countdown(ms)
            holder.v.osclockRecentDesc.text = durationText(ms)
            holder.v.osclockRecentPlay.setOnClickListener { start(ms) }
            holder.v.root.setOnLongClickListener {
                store.deleteRecent(ms)
                reload()
                true
            }
        }

        inner class VH(val v: OsclockItemRecentBinding) : RecyclerView.ViewHolder(v.root)
    }

    companion object {
        private const val PAYLOAD = "tick"
        private const val TICK_MS = 250L
        private const val TYPE_TIMER = 0
        private const val TYPE_NOTE = 1
    }
}
