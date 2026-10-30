package com.ezla.oslauncher.clock.timer

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.data.ClockStore
import com.ezla.oslauncher.clock.databinding.OsclockSheetTimerOptionBinding
import com.ezla.oslauncher.clock.ui.ClockSheet
import com.ezla.oslauncher.clock.ui.SoundPicker

/** Sửa Label hoặc chuông "When Timer Ends" cho hẹn giờ kế tiếp; Set mới ghi ClockStore. */
class TimerOptionSheet : ClockSheet<OsclockSheetTimerOptionBinding>() {

    interface Host {
        fun onTimerOptionsChanged()
    }

    private lateinit var store: ClockStore
    private var picker: SoundPicker? = null
    private val isLabel get() = requireArguments().getBoolean(ARG_LABEL)

    override fun inflate(inflater: LayoutInflater, container: ViewGroup?) =
        OsclockSheetTimerOptionBinding.inflate(inflater, container, false)

    override fun bind() {
        store = ClockStore(requireContext())
        val bar = b.osclockToptBar
        bar.osclockBarTitle.setText(if (isLabel) R.string.osclock_label else R.string.osclock_when_timer_ends)
        bar.osclockBarRight.setText(R.string.osclock_set)
        bar.osclockBarLeft.setOnClickListener { dismiss() }
        bar.osclockBarRight.setOnClickListener { apply() }
        if (isLabel) {
            b.osclockToptLabel.visibility = View.VISIBLE
            b.osclockToptLabel.hint = getString(R.string.osclock_timer)
            b.osclockToptLabel.setText(store.timerLabel)
            b.osclockToptLabel.setSelection(b.osclockToptLabel.length())
            b.osclockToptLabel.setOnEditorActionListener { _, a, _ -> if (a == EditorInfo.IME_ACTION_DONE) { apply(); true } else false }
            b.osclockToptLabel.requestFocus()
            b.osclockToptLabel.postDelayed({
                if (view == null) return@postDelayed
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(b.osclockToptLabel, InputMethodManager.SHOW_IMPLICIT)
            }, 250)
        } else {
            b.osclockToptSounds.visibility = View.VISIBLE
            picker = SoundPicker(requireContext(), b.osclockToptSounds, withStop = true) {}.also { it.load(store.timerSound) }
        }
    }

    private fun apply() {
        if (isLabel) store.timerLabel = b.osclockToptLabel.text?.toString()?.trim().orEmpty()
        else picker?.let { store.timerSound = it.selected }
        (parentFragment as? Host)?.onTimerOptionsChanged()
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        picker?.stopPreview()
        super.onDismiss(dialog)
    }

    companion object {
        const val TAG = "timer_option"
        private const val ARG_LABEL = "label"

        fun newInstance(label: Boolean) = TimerOptionSheet().apply { arguments = Bundle().apply { putBoolean(ARG_LABEL, label) } }
    }
}
