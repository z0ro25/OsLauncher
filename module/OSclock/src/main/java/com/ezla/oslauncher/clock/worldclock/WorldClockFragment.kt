package com.ezla.oslauncher.clock.worldclock

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.appbase.base.BaseFragment
import com.ezla.oslauncher.clock.ClockActivity
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.core.ClockMath
import com.ezla.oslauncher.clock.data.ClockStore
import com.ezla.oslauncher.clock.databinding.OsclockFragmentListBinding
import com.ezla.oslauncher.clock.databinding.OsclockItemCityBinding
import java.util.Calendar
import java.util.TimeZone

// Figma "World Clock" / "World Clock Edit" / "City Added". Edit: xoá + kéo sắp xếp.
class WorldClockFragment : BaseFragment<OsclockFragmentListBinding>(), CityPickerSheet.Host {

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        OsclockFragmentListBinding.inflate(inflater, container, false)

    private lateinit var store: ClockStore
    private val adapter = CityAdapter()
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            adapter.notifyItemRangeChanged(0, adapter.itemCount, PAYLOAD_TIME)
            val now = System.currentTimeMillis()
            handler.postDelayed(this, 60_000 - now % 60_000 + 50)
        }
    }
    private val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun isLongPressDragEnabled() = false
        override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            adapter.move(vh.bindingAdapterPosition, target.bindingAdapterPosition)
            return true
        }
        override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) = Unit
        override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
            super.clearView(rv, vh)
            store.saveCities(adapter.zones)
        }
    })

    override fun initView() {
        store = ClockStore(requireContext())
        binding.osclockHeader.osclockHeaderTitle.setText(R.string.osclock_tab_world)
        binding.osclockListEmpty.setText(R.string.osclock_no_world_clock)
        binding.osclockList.layoutManager = LinearLayoutManager(requireContext())
        binding.osclockList.adapter = adapter
        touchHelper.attachToRecyclerView(binding.osclockList)
        adapter.submit(store.cities())
        renderHeader()
    }

    override fun viewListener() {
        binding.osclockHeader.osclockHeaderLeft.setOnClickListener {
            adapter.editMode = !adapter.editMode
            renderHeader()
        }
        binding.osclockHeader.osclockHeaderRight.setOnClickListener {
            if (childFragmentManager.findFragmentByTag(CityPickerSheet.TAG) == null) {
                CityPickerSheet().show(childFragmentManager, CityPickerSheet.TAG)
            }
        }
    }

    override fun dataObservable() {}

    override fun onStart() {
        super.onStart()
        handler.post(tick)
    }

    override fun onStop() {
        handler.removeCallbacks(tick)
        super.onStop()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden && adapter.editMode) {
            adapter.editMode = false
            renderHeader()
        }
    }

    private fun renderHeader() {
        binding.osclockHeader.osclockHeaderLeft.setText(if (adapter.editMode) R.string.osclock_done else R.string.osclock_edit)
        binding.osclockHeader.osclockHeaderLeft.visibility = if (adapter.itemCount == 0 && !adapter.editMode) View.INVISIBLE else View.VISIBLE
        binding.osclockListEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun onCityPicked(city: City) {
        if (city.zoneId !in adapter.zones) {
            adapter.submit(adapter.zones + city.zoneId)
            store.saveCities(adapter.zones)
            binding.osclockList.scrollToPosition(adapter.itemCount - 1)
        }
        renderHeader()
        (activity as? ClockActivity)?.showToast(R.drawable.osclock_ic_check_white, getString(R.string.osclock_city_added, city.name))
    }

    private inner class CityAdapter : RecyclerView.Adapter<CityAdapter.VH>() {
        val zones = mutableListOf<String>()
        var editMode = false
            @SuppressLint("NotifyDataSetChanged")
            set(v) { field = v; notifyDataSetChanged() }

        @SuppressLint("NotifyDataSetChanged")
        fun submit(list: List<String>) {
            zones.clear()
            zones.addAll(list)
            notifyDataSetChanged()
        }

        fun move(from: Int, to: Int) {
            if (from < 0 || to < 0) return
            zones.add(to, zones.removeAt(from))
            notifyItemMoved(from, to)
        }

        override fun getItemCount() = zones.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(OsclockItemCityBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
            if (payloads.contains(PAYLOAD_TIME)) holder.bindTime(zones[position]) else super.onBindViewHolder(holder, position, payloads)
        }

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(zones[position])

        inner class VH(val v: OsclockItemCityBinding) : RecyclerView.ViewHolder(v.root) {

            @SuppressLint("ClickableViewAccessibility")
            fun bind(zone: String) {
                v.osclockCityName.text = Cities.nameOf(zone)
                v.osclockCityDelete.visibility = if (editMode) View.VISIBLE else View.GONE
                v.osclockCityDrag.visibility = if (editMode) View.VISIBLE else View.GONE
                v.osclockCityTime.visibility = if (editMode) View.GONE else View.VISIBLE
                v.osclockCityDelete.setOnClickListener {
                    val p = bindingAdapterPosition
                    if (p == RecyclerView.NO_POSITION) return@setOnClickListener
                    zones.removeAt(p)
                    notifyItemRemoved(p)
                    store.saveCities(zones)
                    renderHeader()
                }
                v.osclockCityDrag.setOnTouchListener { _, e ->
                    if (e.actionMasked == MotionEvent.ACTION_DOWN) touchHelper.startDrag(this)
                    false
                }
                bindTime(zone)
            }

            fun bindTime(zone: String) {
                val now = System.currentTimeMillis()
                val tz = TimeZone.getTimeZone(Cities.zoneOf(zone))
                val local = TimeZone.getDefault()
                val c = Calendar.getInstance(tz).apply { timeInMillis = now }
                v.osclockCityTime.text = ClockMath.hhmm(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
                val day = when (ClockMath.dayDelta(now, tz, local)) {
                    -1 -> R.string.osclock_yesterday
                    1 -> R.string.osclock_tomorrow
                    else -> R.string.osclock_today
                }
                v.osclockCityOffset.text = getString(
                    R.string.osclock_day_offset, getString(day), ClockMath.offsetLabel(ClockMath.offsetMinutes(now, tz, local))
                )
            }
        }
    }

    companion object {
        private const val PAYLOAD_TIME = "time"
    }
}
