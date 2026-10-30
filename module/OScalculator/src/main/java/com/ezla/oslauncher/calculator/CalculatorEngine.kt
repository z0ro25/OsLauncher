package com.ezla.oslauncher.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

// Máy tính kiểu iOS: × ÷ trước + −, "=" lặp phép cuối, % / ± tác động số đang nhập.
// Thuần Kotlin để unit test được; BigDecimal tránh sai số 0.1+0.2.
class CalculatorEngine {

    enum class Op(val symbol: String) {
        ADD("+"), SUBTRACT("−"), MULTIPLY("×"), DIVIDE("÷");

        val highPriority get() = this == MULTIPLY || this == DIVIDE
    }

    /** Một phép đã hoàn tất bằng "=", để lưu lịch sử. */
    data class Result(val expression: String, val result: String)

    // Biểu thức đang dựng: các số đã chốt + toán tử giữa chúng (operands.size == ops.size + 1 khi đủ).
    private val operands = ArrayList<BigDecimal>()
    private val ops = ArrayList<Op>()

    // Số đang gõ dạng chuỗi (giữ "0." / "-0" như user gõ); null = chưa gõ số mới.
    private var entry: String? = null

    // Số hiển thị khi không gõ (kết quả vừa tính / số vừa chốt trước toán tử).
    private var shown: BigDecimal = BigDecimal.ZERO

    // "=" lặp: phép + toán hạng cuối của lần "=" trước.
    private var lastOp: Op? = null
    private var lastOperand: BigDecimal? = null

    // Vừa bấm "=" -> gõ số mới sẽ bắt đầu phép mới.
    private var justEvaluated = false

    // Biểu thức của lần "=" gần nhất: vẫn hiện phía trên kết quả (Figma: "30x4" trên "120").
    private var lastExpression = ""

    var hasError = false
        private set

    /** Toán tử đang chờ số thứ hai (để sáng nút như iOS); null nếu đang gõ số. */
    val pendingOperator: Op?
        get() = if (entry == null && ops.size == operands.size && ops.isNotEmpty()) ops.last() else null

    /** Nút C trái = "C" khi có số đang gõ, "AC" khi xoá hết. */
    val showsClearEntry: Boolean get() = entry != null && !hasError

    fun inputDigit(d: Char) {
        require(d in '0'..'9')
        if (hasError) allClear()
        if (justEvaluated) resetExpression()
        val cur = entry
        entry = when {
            cur == null -> d.toString()
            cur == "0" -> d.toString()
            cur == "-0" -> "-$d"
            digitCount(cur) >= MAX_DIGITS -> cur
            else -> cur + d
        }
    }

    fun inputDot() {
        if (hasError) allClear()
        if (justEvaluated) resetExpression()
        val cur = entry
        entry = when {
            cur == null -> "0."
            cur.contains('.') -> cur
            digitCount(cur) >= MAX_DIGITS -> cur
            else -> "$cur."
        }
    }

    fun inputOperator(op: Op) {
        if (hasError) return
        justEvaluated = false
        val cur = entry
        if (cur == null) {
            if (ops.size == operands.size && ops.isNotEmpty()) {
                // Đổi toán tử khi chưa gõ số thứ hai (iOS cho đổi ý).
                ops[ops.size - 1] = op
                return
            }
            if (operands.isEmpty()) operands.add(shown)
        } else {
            operands.add(cur.toBigDecimal())
            entry = null
        }
        // Gộp trước những phép có độ ưu tiên >= op để hiển thị kết quả tạm như iOS.
        collapse(op.highPriority)
        if (hasError) return
        shown = operands.last()
        ops.add(op)
    }

    fun equal(): Result? {
        if (hasError) return null
        val cur = entry
        val expression: String
        if (justEvaluated) {
            val op = lastOp ?: return null
            val b = lastOperand ?: return null
            expression = format(shown) + op.symbol + format(b)
            val r = apply(shown, op, b) ?: return error()
            shown = r
        } else {
            if (operands.isEmpty() && cur == null) return null
            if (cur != null) {
                operands.add(cur.toBigDecimal())
            } else if (ops.size == operands.size && ops.isNotEmpty()) {
                // "5 + =" -> dùng chính số trước làm toán hạng (5 + 5) như iOS.
                operands.add(operands.last())
            }
            entry = null
            if (ops.isEmpty()) {
                shown = operands.last()
                resetExpressionKeepShown()
                return null
            }
            expression = buildExpression()
            lastOp = ops.last()
            lastOperand = operands.last()
            collapse(false)
            if (hasError) return null
            shown = operands.last()
        }
        operands.clear()
        ops.clear()
        justEvaluated = true
        lastExpression = expression
        return Result(expression, format(shown))
    }

    /** % : trong phép ± thì là % của số trước (50+10% = 55), còn lại chia 100. */
    fun percent() {
        if (hasError) return
        val value = currentValue()
        // Đang nhập số thứ hai của + / − (operands.size == ops.size) thì lấy số trước làm gốc.
        val base = if (ops.isNotEmpty() && operands.size == ops.size && !ops.last().highPriority)
            operands.last() else null
        val r = if (base != null) base.multiply(value).divide(HUNDRED, MC) else value.divide(HUNDRED, MC)
        setCurrent(r)
    }

    fun toggleSign() {
        if (hasError) return
        val cur = entry
        if (cur != null) {
            entry = if (cur.startsWith("-")) cur.substring(1) else "-$cur"
        } else if (pendingOperator != null) {
            // Chưa gõ số thứ hai: iOS hiện "-0" để gõ tiếp số âm.
            entry = "-0"
        } else {
            shown = shown.negate()
        }
    }

    /** "C": xoá số đang gõ; nếu không có thì xoá hết ("AC"). */
    fun clear() {
        if (entry != null && !hasError) {
            entry = null
            shown = BigDecimal.ZERO
            // Giữ biểu thức + toán tử: gõ lại số thứ hai.
            if (operands.isEmpty()) justEvaluated = false
        } else {
            allClear()
        }
    }

    fun allClear() {
        operands.clear()
        ops.clear()
        entry = null
        shown = BigDecimal.ZERO
        lastOp = null
        lastOperand = null
        justEvaluated = false
        hasError = false
    }

    /** Xoá 1 ký tự số đang gõ (vuốt trên màn hiển thị như iOS). */
    fun backspace() {
        if (hasError || justEvaluated) return
        val cur = entry ?: return
        val next = cur.dropLast(1)
        entry = if (next.isEmpty() || next == "-") null else next
        if (entry == null) shown = BigDecimal.ZERO
    }

    /** Nạp lại kết quả từ lịch sử làm số đang nhập. */
    fun loadValue(value: String) {
        val v = value.replace(",", "").toBigDecimalOrNull() ?: return
        if (hasError || justEvaluated) resetExpression()
        entry = plain(v)
    }

    /** Chuỗi hiển thị lớn (kết quả / số đang gõ). */
    fun display(): String {
        if (hasError) return ERROR
        val cur = entry
        return if (cur != null) formatEntry(cur) else format(shown)
    }

    /** Dòng biểu thức nhỏ phía trên (vd "30×4"). */
    fun expressionLine(): String {
        if (hasError) return ""
        if (justEvaluated) return lastExpression
        if (operands.isEmpty() && ops.isEmpty()) return ""
        val sb = StringBuilder()
        for (i in operands.indices) {
            sb.append(format(operands[i]))
            if (i < ops.size) sb.append(ops[i].symbol)
        }
        entry?.let { sb.append(formatEntry(it)) }
        return sb.toString()
    }

    private fun buildExpression(): String {
        val sb = StringBuilder()
        for (i in operands.indices) {
            sb.append(format(operands[i]))
            if (i < ops.size) sb.append(ops[i].symbol)
        }
        return sb.toString()
    }

    // Gộp từ cuối: highOnly=true chỉ gộp × ÷ (trước khi thêm × ÷), false gộp hết.
    private fun collapse(highOnly: Boolean) {
        if (!highOnly) {
            // Gộp × ÷ trước rồi + − (ưu tiên toán tử).
            var i = 0
            while (i < ops.size) {
                if (ops[i].highPriority) {
                    val r = apply(operands[i], ops[i], operands[i + 1]) ?: run { error(); return }
                    operands[i] = r
                    operands.removeAt(i + 1)
                    ops.removeAt(i)
                } else i++
            }
            while (ops.isNotEmpty()) {
                val r = apply(operands[0], ops[0], operands[1]) ?: run { error(); return }
                operands[0] = r
                operands.removeAt(1)
                ops.removeAt(0)
            }
        } else {
            while (ops.isNotEmpty() && ops.last().highPriority && operands.size == ops.size + 1) {
                val n = ops.size - 1
                val r = apply(operands[n], ops[n], operands[n + 1]) ?: run { error(); return }
                operands[n] = r
                operands.removeAt(n + 1)
                ops.removeAt(n)
            }
        }
    }

    private fun currentValue(): BigDecimal = entry?.toBigDecimal() ?: shown

    private fun setCurrent(v: BigDecimal) {
        if (justEvaluated) {
            shown = v
            return
        }
        entry = plain(v)
    }

    private fun resetExpression() {
        resetExpressionKeepShown()
        shown = BigDecimal.ZERO
    }

    private fun resetExpressionKeepShown() {
        operands.clear()
        ops.clear()
        lastOp = null
        lastOperand = null
        justEvaluated = false
        lastExpression = ""
    }

    private fun error(): Result? {
        hasError = true
        operands.clear()
        ops.clear()
        entry = null
        return null
    }

    private fun apply(a: BigDecimal, op: Op, b: BigDecimal): BigDecimal? = when (op) {
        Op.ADD -> a.add(b, MC)
        Op.SUBTRACT -> a.subtract(b, MC)
        Op.MULTIPLY -> a.multiply(b, MC)
        Op.DIVIDE -> if (b.signum() == 0) null else a.divide(b, MC)
    }

    private fun digitCount(s: String) = s.count { it.isDigit() }

    companion object {
        const val MAX_DIGITS = 9
        const val ERROR = "Error"
        private val MC = MathContext(16, RoundingMode.HALF_UP)
        private val HUNDRED = BigDecimal(100)
        private val MAX_PLAIN = BigDecimal("1e9")
        private val MIN_PLAIN = BigDecimal("1e-8")

        /** Số -> chuỗi hiển thị: nhóm nghìn bằng ",", tối đa 9 chữ số, quá lớn/nhỏ thì dạng 1.2e10. */
        fun format(v: BigDecimal): String {
            if (v.signum() == 0) return "0"
            val abs = v.abs()
            if (abs >= MAX_PLAIN || abs < MIN_PLAIN) return scientific(v)
            // Số < 1: số 0 đứng đầu không tính vào giới hạn 9 chữ số (1/3 -> 0.333333333).
            val intDigits = if (abs < BigDecimal.ONE) 0
            else abs.setScale(0, RoundingMode.DOWN).toPlainString().length
            val scale = (MAX_DIGITS - intDigits).coerceAtLeast(0)
            val rounded = v.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros()
            if (rounded.signum() == 0) return "0"
            return group(plain(rounded))
        }

        private fun scientific(v: BigDecimal): String {
            val s = String.format(java.util.Locale.US, "%.5e", v)
            val (mant, exp) = s.split("e")
            val m = mant.trimEnd('0').trimEnd('.')
            return m + "e" + exp.toInt()
        }

        private fun plain(v: BigDecimal): String = v.stripTrailingZeros().toPlainString()

        // Giữ nguyên phần thập phân user đang gõ (kể cả "0." / "1.50"), chỉ nhóm phần nguyên.
        private fun formatEntry(s: String): String = group(s)

        private fun group(s: String): String {
            val neg = s.startsWith("-")
            val body = if (neg) s.substring(1) else s
            val dot = body.indexOf('.')
            val intPart = if (dot >= 0) body.substring(0, dot) else body
            val frac = if (dot >= 0) body.substring(dot) else ""
            val sb = StringBuilder()
            for (i in intPart.indices) {
                if (i > 0 && (intPart.length - i) % 3 == 0) sb.append(',')
                sb.append(intPart[i])
            }
            return (if (neg) "-" else "") + sb + frac
        }
    }
}
