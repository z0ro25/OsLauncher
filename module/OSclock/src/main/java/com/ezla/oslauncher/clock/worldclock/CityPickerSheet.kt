package com.ezla.oslauncher.clock.worldclock

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.clock.databinding.OsclockSheetCityBinding
import com.ezla.oslauncher.clock.ui.ClockSheet
import com.ezla.oslauncher.clock.ui.TextRowAdapter
import kotlin.concurrent.thread

/** Figma "Choose a City": gõ để lọc, bấm 1 dòng = thêm thành phố và đóng sheet. */
class CityPickerSheet : ClockSheet<OsclockSheetCityBinding>() {

    interface Host {
        fun onCityPicked(city: City)
    }

    private var all: List<City> = emptyList()
    private var shown: List<City> = emptyList()
    private val adapter = TextRowAdapter(checkAtStart = false) { i ->
        shown.getOrNull(i)?.let { (parentFragment as? Host)?.onCityPicked(it) }
        dismiss()
    }

    override fun inflate(inflater: LayoutInflater, container: ViewGroup?) =
        OsclockSheetCityBinding.inflate(inflater, container, false)

    override fun bind() {
        b.osclockCityResults.layoutManager = LinearLayoutManager(requireContext())
        b.osclockCityResults.adapter = adapter
        b.osclockCityCancel.setOnClickListener { dismiss() }
        b.osclockCitySearch.doAfterTextChanged { filter() }
        // ~400 múi giờ + ICU getRegion: dựng nền để sheet mở không giật.
        val root = b.root
        thread(name = "osclock-cities") {
            val list = Cities.all()
            root.post {
                if (view == null) return@post
                all = list
                filter()
            }
        }
        b.osclockCitySearch.requestFocus()
        b.osclockCitySearch.postDelayed({
            if (view == null) return@postDelayed
            (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(b.osclockCitySearch, InputMethodManager.SHOW_IMPLICIT)
        }, 250)
    }

    private fun filter() {
        shown = Cities.search(all, b.osclockCitySearch.text?.toString().orEmpty())
        adapter.submit(shown.map { it.display })
    }

    companion object {
        const val TAG = "city_picker"
    }
}
