package com.ezla.oslauncher.calculator

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.ezla.oslauncher.calculator.CalculatorEngine.Op
import com.ezla.oslauncher.calculator.databinding.CalcActivityCalculatorBinding
import com.ezla.oslauncher.appbase.base.BaseActivity
import kotlin.math.abs

/** Máy tính kiểu iOS (Figma "18 · Calculator"). Kế thừa BaseActivity dùng chung (:library:OSAppBase). */
class CalculatorActivity : BaseActivity<CalcActivityCalculatorBinding>(), HistorySheet.Host {

    override val setViewBinding get() = CalcActivityCalculatorBinding.inflate(layoutInflater)

    // Figma tràn viền nền đen (status bar đè lên): layout tự canh top, không cần base pad.
    override val padStatusBar = false

    private val b get() = binding
    private val engine = CalculatorEngine()
    private lateinit var history: HistoryStore

    // Mọi đường vào (icon desktop, CalculatorEntry) đều mở thẳng màn này: lần đầu chuyển sang onboarding.
    override fun redirectBeforeCreate(): Boolean {
        if (CalculatorEntry.isOnboardingDone(this)) return false
        startActivity(android.content.Intent(this, CalculatorOnboardingActivity::class.java))
        return true
    }

    override fun initView() {
        history = HistoryStore(this)
        // Thay listener inset của base: bàn phím số phải nằm trên thanh điều hướng (khi user bật nav).
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            v.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            insets
        }
        render()
    }

    override fun viewListener() {
        bindKeys()
        bindDisplay()
    }

    override fun dataObservable() {}

    private fun bindKeys() {
        val digits = listOf(b.calcKey0, b.calcKey1, b.calcKey2, b.calcKey3, b.calcKey4,
            b.calcKey5, b.calcKey6, b.calcKey7, b.calcKey8, b.calcKey9)
        digits.forEachIndexed { i, v -> v.setOnClickListener { tap(it) { engine.inputDigit('0' + i) } } }
        b.calcKeyDot.setOnClickListener { tap(it) { engine.inputDot() } }
        b.calcKeyClear.setOnClickListener { tap(it) { engine.clear() } }
        b.calcKeySign.setOnClickListener { tap(it) { engine.toggleSign() } }
        b.calcKeyPercent.setOnClickListener { tap(it) { engine.percent() } }
        b.calcKeyDivide.setOnClickListener { tap(it) { engine.inputOperator(Op.DIVIDE) } }
        b.calcKeyMultiply.setOnClickListener { tap(it) { engine.inputOperator(Op.MULTIPLY) } }
        b.calcKeyMinus.setOnClickListener { tap(it) { engine.inputOperator(Op.SUBTRACT) } }
        b.calcKeyPlus.setOnClickListener { tap(it) { engine.inputOperator(Op.ADD) } }
        b.calcKeyEqual.setOnClickListener {
            tap(it) { engine.equal()?.let { r -> history.add(r.expression, r.result) } }
        }
        b.calcBtnHistory.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(HistorySheet.TAG) == null) {
                HistorySheet().show(supportFragmentManager, HistorySheet.TAG)
            }
        }
    }

    // Vuốt ngang trên số = xoá 1 chữ số (như iOS); giữ lâu = copy kết quả.
    @SuppressLint("ClickableViewAccessibility")
    private fun bindDisplay() {
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 != null && abs(e2.x - e1.x) > SWIPE_MIN && abs(vx) > abs(vy)) {
                    engine.backspace()
                    render()
                    return true
                }
                return false
            }
            override fun onLongPress(e: MotionEvent) = copyResult()
        })
        b.calcTvResult.setOnTouchListener { _, ev -> detector.onTouchEvent(ev) }
    }

    private fun copyResult() {
        val text = b.calcTvResult.text.toString()
        if (engine.hasError) return
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(getString(R.string.calc_app_name), text.replace(",", "")))
        b.calcTvResult.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        Toast.makeText(this, R.string.calc_copied, Toast.LENGTH_SHORT).show()
    }

    private inline fun tap(v: View, action: () -> Unit) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        action()
        render()
    }

    private fun render() {
        b.calcTvResult.text = if (engine.hasError) getString(R.string.calc_error) else engine.display()
        b.calcTvExpression.text = engine.expressionLine()
        // Toán tử đang chờ số thứ hai: nút sáng trắng, icon cam (calc_bg_key_operator selected).
        val pending = engine.pendingOperator
        b.calcKeyDivide.isSelected = pending == Op.DIVIDE
        b.calcKeyMultiply.isSelected = pending == Op.MULTIPLY
        b.calcKeyMinus.isSelected = pending == Op.SUBTRACT
        b.calcKeyPlus.isSelected = pending == Op.ADD
    }

    override fun onHistoryPicked(result: String) {
        engine.loadValue(result)
        render()
    }

    companion object {
        private const val SWIPE_MIN = 60f
    }
}
