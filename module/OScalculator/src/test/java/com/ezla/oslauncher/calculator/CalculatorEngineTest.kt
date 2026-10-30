package com.ezla.oslauncher.calculator

import com.ezla.oslauncher.calculator.CalculatorEngine.Op
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

    private fun CalculatorEngine.type(s: String) = s.forEach {
        when (it) {
            in '0'..'9' -> inputDigit(it)
            '.' -> inputDot()
            '+' -> inputOperator(Op.ADD)
            '-' -> inputOperator(Op.SUBTRACT)
            'x' -> inputOperator(Op.MULTIPLY)
            '/' -> inputOperator(Op.DIVIDE)
            '=' -> equal()
            '%' -> percent()
            'n' -> toggleSign()
        }
    }

    private fun calc(s: String) = CalculatorEngine().apply { type(s) }.display()

    @Test fun figmaExample() {
        val e = CalculatorEngine()
        e.type("30x4")
        assertEquals("30×4", e.expressionLine())
        val r = e.equal()
        assertEquals("120", e.display())
        assertEquals("30×4", r!!.expression)
        assertEquals("120", r.result)
    }

    @Test fun precedence() = assertEquals("14", calc("2+3x4="))
    @Test fun precedenceDivide() = assertEquals("7", calc("1+12/2="))
    @Test fun decimals() = assertEquals("0.3", calc("0.1+0.2="))
    @Test fun divideByZero() = assertEquals(CalculatorEngine.ERROR, calc("5/0="))
    @Test fun repeatEquals() = assertEquals("11", calc("5+2==="))
    @Test fun repeatEqualsMultiply() = assertEquals("20", calc("5x2=="))

    @Test fun expressionStaysAfterEquals() {
        val e = CalculatorEngine()
        e.type("30x4=")
        assertEquals("30×4", e.expressionLine())
        e.type("5")
        assertEquals("", e.expressionLine())
    }
    @Test fun operatorReplace() = assertEquals("3", calc("5+-2="))
    @Test fun selfOperand() = assertEquals("10", calc("5+="))
    @Test fun percentPlain() = assertEquals("0.5", calc("50%"))
    @Test fun percentOfBase() = assertEquals("55", calc("50+10%="))
    @Test fun percentMultiply() = assertEquals("5", calc("50x10%="))
    @Test fun negate() = assertEquals("-12", calc("12n"))
    @Test fun negateSecondOperand() = assertEquals("2", calc("5+n3="))
    @Test fun grouping() = assertEquals("1,234,567", calc("1234567"))
    @Test fun maxDigits() = assertEquals("123,456,789", calc("1234567891"))
    @Test fun leadingZero() = assertEquals("7", calc("007"))
    @Test fun singleDot() = assertEquals("1.5", calc("1..5"))
    @Test fun startWithDot() = assertEquals("0.5", calc(".5"))
    @Test fun trailingDotShown() = assertEquals("3.", calc("3."))
    @Test fun thirdRoundsToNine() = assertEquals("0.333333333", calc("1/3="))
    @Test fun largeUsesScientific() = assertTrue(calc("999999999x999999999=").contains("e"))
    @Test fun negativeResult() = assertEquals("-5", calc("3-8="))
    @Test fun newNumberAfterEquals() = assertEquals("9", calc("2+2=9"))
    @Test fun chainAfterEquals() = assertEquals("12", calc("2+2=x3="))

    @Test fun clearEntryKeepsExpression() {
        val e = CalculatorEngine()
        e.type("8+5")
        assertTrue(e.showsClearEntry)
        e.clear()
        e.type("2=")
        assertEquals("10", e.display())
    }

    @Test fun allClear() {
        val e = CalculatorEngine()
        e.type("8+5")
        e.clear(); e.clear()
        assertEquals("0", e.display())
        assertEquals("", e.expressionLine())
    }

    @Test fun errorThenDigitResets() {
        val e = CalculatorEngine()
        e.type("1/0=")
        assertTrue(e.hasError)
        e.type("4")
        assertEquals("4", e.display())
    }

    @Test fun pendingOperatorHighlight() {
        val e = CalculatorEngine()
        e.type("3x")
        assertEquals(Op.MULTIPLY, e.pendingOperator)
        e.type("4")
        assertNull(e.pendingOperator)
    }

    @Test fun backspace() {
        val e = CalculatorEngine()
        e.type("123")
        e.backspace()
        assertEquals("12", e.display())
    }

    @Test fun loadFromHistory() {
        val e = CalculatorEngine()
        e.loadValue("1,200")
        e.type("+1=")
        assertEquals("1,201", e.display())
    }

    @Test fun equalsOnlyNumberNoHistory() {
        val e = CalculatorEngine()
        e.type("7")
        assertNull(e.equal())
        assertEquals("7", e.display())
    }
}
