package com.ezla.oslauncher.clock.alarm

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezla.oslauncher.clock.R
import com.ezla.oslauncher.clock.core.ClockMath
import com.ezla.oslauncher.clock.data.Alarm
import com.ezla.oslauncher.clock.data.ClockStore
import com.ezla.oslauncher.clock.databinding.OsclockSheetAlarmBinding
import com.ezla.oslauncher.clock.databinding.OsclockViewOptionRowBinding
import com.ezla.oslauncher.clock.ring.AlarmScheduler
import com.ezla.oslauncher.clock.ring.ClockSounds
import com.ezla.oslauncher.clock.ui.ClockSheet
import com.ezla.oslauncher.clock.ui.SoundPicker
import com.ezla.oslauncher.clock.ui.TextRowAdapter
import java.util.Calendar
import java.text.DateFormatSymbols

// Figma "Add Alarm" / "Edit Alarm" / "Repeat" / "Label" / "Sound": 1 sheet, đổi trang bên trong
// để giữ nguyên bản nháp. Chỉ ghi ClockStore khi bấm Save / Delete Alarm.
class AlarmEditSheet : ClockSheet<OsclockSheetAlarmBinding>() {

    interface Host {
        fun onAlarmSaved(alarm: Alarm)
        fun onAlarmDeleted()
    }

    private lateinit var store: ClockStore
    private lateinit var draft: Alarm
    private var isNew = true
    private var soundPicker: SoundPicker? = null
    private val repeatAdapter = TextRowAdapter(checkAtStart = false) { i ->
        draft = draft.copy(days = draft.days xor ClockMath.dayBit(AlarmText.REPEAT_ORDER[i]))
        renderRepeat()
    }

    override fun inflate(inflater: LayoutInflater, container: ViewGroup?) =
        OsclockSheetAlarmBinding.inflate(inflater, container, false)

    override fun bind() {
        store = ClockStore(requireContext())
        val id = requireArguments().getInt(ARG_ID, -1)
        val existing = store.alarm(id)
        isNew = existing == null
        draft = existing ?: Calendar.getInstance().let {
            Alarm(id = -1, hour = it.get(Calendar.HOUR_OF_DAY), minute = it.get(Calendar.MINUTE))
        }

        b.osclockMainBar.osclockBarTitle.setText(if (isNew) R.string.osclock_add_alarm else R.string.osclock_edit_alarm)
        b.osclockMainBar.osclockBarLeft.setOnClickListener { dismiss() }
        b.osclockMainBar.osclockBarRight.setOnClickListener { save() }
        b.osclockBtnDeleteAlarm.visibility = if (isNew) View.GONE else View.VISIBLE
        b.osclockBtnDeleteAlarm.setOnClickListener { deleteAlarm() }

        b.osclockWheelHour.items = (0..23).map { ClockMath.two(it) }
        b.osclockWheelMinute.items = (0..59).map { ClockMath.two(it) }
        b.osclockWheelHour.setSelected(draft.hour)
        b.osclockWheelMinute.setSelected(draft.minute)

        option(b.osclockOptRepeat, R.string.osclock_repeat_title) { showPage(b.osclockPageRepeat) }
        option(b.osclockOptLabel, R.string.osclock_label) { showPage(b.osclockPageLabel) }
        option(b.osclockOptSound, R.string.osclock_sound) { showPage(b.osclockPageSound) }
        option(b.osclockOptSnooze, R.string.osclock_snooze, last = true, toggle = true) {}
        b.osclockOptSnooze.osclockOptToggle.setOn(draft.snooze)
        b.osclockOptSnooze.osclockOptToggle.onChanged = { draft = draft.copy(snooze = it) }

        setupSubBar(b.osclockRepeatBar, R.string.osclock_repeat_title)
        setupSubBar(b.osclockLabelBar, R.string.osclock_label)
        setupSubBar(b.osclockSoundBar, R.string.osclock_sound)
        b.osclockRepeatList.layoutManager = LinearLayoutManager(requireContext())
        b.osclockRepeatList.adapter = repeatAdapter

        b.osclockLabelInput.hint = getString(R.string.osclock_alarm)
        b.osclockLabelInput.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) { showPage(b.osclockPageMain); true } else false
        }

        b.osclockOptHaptics.root.background = ContextCompat.getDrawable(requireContext(), R.drawable.osclock_bg_card)
        option(b.osclockOptHaptics, R.string.osclock_haptics, last = true) {
            draft = draft.copy(vibrate = !draft.vibrate)
            renderSummary()
        }
        soundPicker = SoundPicker(requireContext(), b.osclockSoundList, withStop = false) {
            draft = draft.copy(sound = it)
        }.also { it.load(draft.sound) }

        // Back ở trang con = về trang chính (không đóng sheet, không mất nháp).
        dialog?.setOnKeyListener { _, keyCode, e ->
            val onSubPage = b.osclockPageMain.visibility != View.VISIBLE
            if (keyCode != KeyEvent.KEYCODE_BACK || !onSubPage) return@setOnKeyListener false
            if (e.action == KeyEvent.ACTION_UP) showPage(b.osclockPageMain)
            true
        }

        renderRepeat()
        renderSummary()
    }

    private fun option(row: OsclockViewOptionRowBinding, name: Int, last: Boolean = false, toggle: Boolean = false, click: () -> Unit) {
        row.osclockOptName.setText(name)
        row.osclockOptSeparator.visibility = if (last) View.INVISIBLE else View.VISIBLE
        row.osclockOptToggle.visibility = if (toggle) View.VISIBLE else View.GONE
        row.osclockOptChevron.visibility = if (toggle) View.GONE else View.VISIBLE
        row.osclockOptValue.visibility = if (toggle) View.GONE else View.VISIBLE
        if (!toggle) row.root.setOnClickListener { click() } else row.root.isClickable = false
    }

    private fun setupSubBar(bar: com.ezla.oslauncher.clock.databinding.OsclockViewSheetBarBinding, title: Int) {
        bar.osclockBarTitle.setText(title)
        // INVISIBLE, không GONE: tiêu đề bám dọc theo nút trái, GONE thì nút co về (0,0) làm tiêu đề bị cắt.
        bar.osclockBarLeft.visibility = View.INVISIBLE
        bar.osclockBarRight.visibility = View.GONE
        bar.osclockBarBack.visibility = View.VISIBLE
        bar.osclockBarBack.setOnClickListener { showPage(b.osclockPageMain) }
    }

    private fun showPage(page: View) {
        if (b.osclockPageLabel.visibility == View.VISIBLE && page != b.osclockPageLabel) {
            draft = draft.copy(label = b.osclockLabelInput.text?.toString()?.trim().orEmpty())
            hideKeyboard()
        }
        if (page != b.osclockPageSound) soundPicker?.stopPreview()
        listOf(b.osclockPageMain, b.osclockPageRepeat, b.osclockPageLabel, b.osclockPageSound).forEach {
            it.visibility = if (it == page) View.VISIBLE else View.GONE
        }
        if (page == b.osclockPageLabel) {
            b.osclockLabelInput.setText(draft.label)
            b.osclockLabelInput.setSelection(b.osclockLabelInput.length())
            b.osclockLabelInput.requestFocus()
            b.osclockLabelInput.post {
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(b.osclockLabelInput, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        renderSummary()
    }

    private fun renderRepeat() {
        val names = DateFormatSymbols.getInstance().weekdays
        repeatAdapter.submit(AlarmText.REPEAT_ORDER.map { getString(R.string.osclock_every_day_of, names[it]) }) {
            draft.days and ClockMath.dayBit(AlarmText.REPEAT_ORDER[it]) != 0
        }
        renderSummary()
    }

    private fun renderSummary() {
        b.osclockOptRepeat.osclockOptValue.text = AlarmText.repeatOrNever(requireContext(), draft.days)
        b.osclockOptLabel.osclockOptValue.text = draft.label.ifBlank { getString(R.string.osclock_alarm) }
        b.osclockOptSound.osclockOptValue.text = ClockSounds.shortTitle(requireContext(), draft.sound)
        b.osclockOptHaptics.osclockOptValue.setText(if (draft.vibrate) R.string.osclock_haptics_on else R.string.osclock_haptics_off)
    }

    private fun save() {
        if (b.osclockPageLabel.visibility == View.VISIBLE) {
            draft = draft.copy(label = b.osclockLabelInput.text?.toString()?.trim().orEmpty())
        }
        val saved = draft.copy(
            id = if (isNew) store.nextId() else draft.id,
            hour = b.osclockWheelHour.selected,
            minute = b.osclockWheelMinute.selected,
            enabled = true,
            snoozeUntil = 0
        )
        store.saveAlarm(saved)
        AlarmScheduler.schedule(requireContext(), saved)
        (parentFragment as? Host)?.onAlarmSaved(saved)
        dismiss()
    }

    private fun deleteAlarm() {
        AlarmScheduler.cancelAlarm(requireContext(), draft.id)
        store.deleteAlarm(draft.id)
        (parentFragment as? Host)?.onAlarmDeleted()
        dismiss()
    }

    private fun hideKeyboard() {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(b.osclockLabelInput.windowToken, 0)
    }

    override fun onDismiss(dialog: DialogInterface) {
        soundPicker?.stopPreview()
        super.onDismiss(dialog)
    }

    companion object {
        const val TAG = "alarm_edit"
        private const val ARG_ID = "id"

        fun newInstance(id: Int) = AlarmEditSheet().apply { arguments = Bundle().apply { putInt(ARG_ID, id) } }
    }
}
