package com.ezla.oslauncher.clock.stopwatch

import com.ezla.oslauncher.appbase.R as AppBaseR
import android.graphics.Color
import android.util.TypedValue
import android.view.Choreographer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ezla.oslauncher.appbase.base.BaseFragment
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.core.ClockMath
import com.ezla.oslauncher.clock.data.ClockStore
import com.ezla.oslauncher.clock.databinding.OsclockFragmentStopwatchBinding
import com.ezla.oslauncher.clock.databinding.OsclockItemLapBinding
import com.ezla.oslauncher.clock.ui.StopwatchDialView

// Figma "Stopwatch" / "Running" / "Stopped" / "Analog". Lưu mốc wall-clock vào ClockStore nên
// đóng app rồi mở lại vẫn chạy tiếp (giống iOS).
class StopwatchFragment : BaseFragment<OsclockFragmentStopwatchBinding>() {

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        OsclockFragmentStopwatchBinding.inflate(inflater, container, false)

    private lateinit var store: ClockStore
    private var digital: TextView? = null
    private var analog: StopwatchDialView? = null
    private val lapAdapter = LapAdapter()
    private var ticking = false
    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!ticking) return
            renderTime()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private val running get() = store.swStartedAt > 0L

    private fun elapsed(): Long =
        store.swAccumulated + if (running) System.currentTimeMillis() - store.swStartedAt else 0L

    override fun initView() {
        store = ClockStore(requireContext())
        binding.osclockSwPager.adapter = PageAdapter()
        binding.osclockSwPager.setCurrentItem(store.swPage, false)
        binding.osclockSwPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                store.swPage = position
                renderDots(position)
            }
        })
        binding.osclockSwLaps.layoutManager = LinearLayoutManager(requireContext())
        binding.osclockSwLaps.adapter = lapAdapter
        renderDots(store.swPage)
        render()
    }

    override fun viewListener() {
        binding.osclockSwRight.setOnClickListener {
            if (running) {
                store.swAccumulated = elapsed()
                store.swStartedAt = 0L
            } else {
                store.swStartedAt = System.currentTimeMillis()
            }
            render()
        }
        binding.osclockSwLeft.setOnClickListener {
            if (running) {
                store.swLapMarks = store.swLapMarks + elapsed()
            } else if (elapsed() > 0) {
                store.swAccumulated = 0L
                store.swLapMarks = emptyList()
            }
            render()
        }
    }

    override fun dataObservable() {}

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onPause() {
        stopTicking()
        super.onPause()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) stopTicking() else render()
    }

    private fun render() {
        val r = running
        val e = elapsed()
        val ctx = requireContext()
        binding.osclockSwRight.setText(if (r) R.string.osclock_stop else R.string.osclock_start)
        binding.osclockSwRight.setBackgroundResource(if (r) R.drawable.osclock_bg_circle_red else R.drawable.osclock_bg_circle_green)
        binding.osclockSwRight.setTextColor(ctx.getColor(if (r) R.color.osclock_red else R.color.osclock_green))
        // Đang chạy = Lap; dừng = Reset (chưa chạy lần nào thì Reset xám, không bấm được như Figma).
        binding.osclockSwLeft.setText(if (r) R.string.osclock_lap else R.string.osclock_reset)
        val leftActive = r || e > 0L
        binding.osclockSwLeft.isEnabled = leftActive
        binding.osclockSwLeft.setBackgroundResource(if (leftActive) R.drawable.osclock_bg_circle_active else R.drawable.osclock_bg_circle_idle)
        binding.osclockSwLeft.setTextColor(ctx.getColor(if (leftActive) R.color.osclock_text else R.color.osclock_gray))
        renderTime()
        if (r && isResumed && !isHidden) startTicking() else stopTicking()
    }

    private fun renderTime() {
        val e = elapsed()
        digital?.text = ClockMath.stopwatch(e)
        analog?.elapsedMs = e
        lapAdapter.update(store.swLapMarks, e, running || e > 0)
    }

    private fun renderDots(page: Int) {
        binding.osclockSwDot0.alpha = if (page == 0) 1f else 0.4f
        binding.osclockSwDot1.alpha = if (page == 1) 1f else 0.4f
    }

    private fun startTicking() {
        if (ticking) return
        ticking = true
        Choreographer.getInstance().postFrameCallback(frame)
    }

    private fun stopTicking() {
        ticking = false
        Choreographer.getInstance().removeFrameCallback(frame)
    }

    private inner class PageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = 2
        override fun getItemViewType(position: Int) = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val ctx = parent.context
            val d = ctx.resources.displayMetrics.density
            val page = FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            if (viewType == 0) {
                val tv = TextView(ctx).apply {
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 72f)
                    typeface = ResourcesCompat.getFont(ctx, AppBaseR.font.appbase_sf_pro_light)
                    fontFeatureSettings = "tnum"
                    includeFontPadding = false
                    gravity = Gravity.CENTER
                    maxLines = 1
                }
                page.addView(tv, FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply { topMargin = (100 * d).toInt() })
                digital = tv
            } else {
                val dial = StopwatchDialView(ctx)
                page.addView(dial, FrameLayout.LayoutParams((280 * d).toInt(), (280 * d).toInt(), Gravity.CENTER_HORIZONTAL or Gravity.TOP)
                    .apply { topMargin = (22 * d).toInt() })
                analog = dial
            }
            return object : RecyclerView.ViewHolder(page) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = renderTime()
    }

    /** Hàng đầu = lap đang chạy, rồi các lap đã xong (mới nhất trước). */
    private inner class LapAdapter : RecyclerView.Adapter<LapAdapter.VH>() {
        private var laps: List<Long> = emptyList() // độ dài từng lap đã xong, cũ nhất trước
        private var current = 0L
        private var show = false
        private var best = -1
        private var worst = -1

        fun update(marks: List<Long>, elapsed: Long, visible: Boolean) {
            val newLaps = marks.mapIndexed { i, m -> m - (if (i == 0) 0L else marks[i - 1]) }
            val cur = elapsed - (marks.lastOrNull() ?: 0L)
            val structural = newLaps != laps || visible != show
            laps = newLaps
            current = cur
            show = visible
            val bw = ClockMath.bestWorst(laps)
            best = bw.first
            worst = bw.second
            @Suppress("NotifyDataSetChanged")
            if (structural) notifyDataSetChanged() else if (itemCount > 0) notifyItemChanged(0, PAYLOAD)
        }

        override fun getItemCount() = if (show) laps.size + 1 else 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(OsclockItemLapBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val ctx = holder.itemView.context
            val lapIndex = if (position == 0) laps.size else laps.size - position // 0-based
            val value = if (position == 0) current else laps[lapIndex]
            holder.v.osclockLapName.text = ctx.getString(R.string.osclock_lap_n, lapIndex + 1)
            holder.v.osclockLapTime.text = ClockMath.stopwatch(value)
            val color = when {
                position == 0 -> R.color.osclock_text
                lapIndex == best -> R.color.osclock_green
                lapIndex == worst -> R.color.osclock_red
                else -> R.color.osclock_text
            }
            holder.v.osclockLapName.setTextColor(ctx.getColor(color))
            holder.v.osclockLapTime.setTextColor(ctx.getColor(color))
        }

        inner class VH(val v: OsclockItemLapBinding) : RecyclerView.ViewHolder(v.root)
    }

    companion object {
        private const val PAYLOAD = "tick"
    }
}
