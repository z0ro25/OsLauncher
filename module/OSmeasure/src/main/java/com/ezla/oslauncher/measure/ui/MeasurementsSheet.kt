package com.ezla.oslauncher.measure.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.measure.R
import com.ezla.oslauncher.measure.data.MeasureFormat
import com.ezla.oslauncher.measure.data.MeasureStore
import com.ezla.oslauncher.measure.data.Measurement
import com.ezla.oslauncher.measure.databinding.MeasureItemMeasurementBinding
import com.ezla.oslauncher.measure.databinding.MeasureSheetListBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Sheet lịch sử đo (Figma "Measurements"): copy từng dòng, Clear All. */
class MeasurementsSheet : BottomSheetDialogFragment() {

    private var _b: MeasureSheetListBinding? = null
    private val b get() = _b!!

    override fun getTheme() = R.style.MeasureBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = MeasureSheetListBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Nền sheet phủ kín tới đáy màn (Figma): chữ / nút chỉ né thanh điều hướng bằng padding.
        val basePad = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            v.updatePadding(bottom = basePad + insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }
        val store = MeasureStore(requireContext())
        val adapter = Adapter { (activity as? com.ezla.oslauncher.measure.MeasureActivity)?.copyText(MeasureFormat.cm(it.meters)) }
        b.measureSheetList.layoutManager = LinearLayoutManager(requireContext())
        b.measureSheetList.adapter = adapter
        render(adapter, store.all())
        b.measureSheetDone.setOnClickListener { dismiss() }
        b.measureSheetClearAll.setOnClickListener {
            store.clear()
            render(adapter, emptyList())
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    private fun render(adapter: Adapter, items: List<Measurement>) {
        adapter.submit(items)
        b.measureSheetEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        b.measureSheetClearAll.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    private class Adapter(private val onCopy: (Measurement) -> Unit) : RecyclerView.Adapter<Adapter.Holder>() {
        private var items: List<Measurement> = emptyList()

        fun submit(list: List<Measurement>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            MeasureItemMeasurementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val m = items[position]
            holder.b.measureItemValue.text = MeasureFormat.cm(m.meters)
            holder.b.measureItemCopy.setOnClickListener { onCopy(m) }
        }

        class Holder(val b: MeasureItemMeasurementBinding) : RecyclerView.ViewHolder(b.root)
    }

    companion object {
        const val TAG = "measurements"
    }
}
