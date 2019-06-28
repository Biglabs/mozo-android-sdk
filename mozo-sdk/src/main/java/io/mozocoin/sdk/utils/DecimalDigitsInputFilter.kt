package io.mozocoin.sdk.utils

import android.text.InputFilter
import android.text.Spanned
import java.text.DecimalFormatSymbols

class DecimalDigitsInputFilter(private val digitsBeforeZero: Int, private val digitsAfterZero: Int) : InputFilter {

    private val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator

    override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence? {
        if (decimalSeparator.toString().equals(source?.toString(), true) && dest?.contains(decimalSeparator, true) == true) {
            return ""
        }

        dest?.let {
            val s = it.split(decimalSeparator)
            return when {
                s.size == 1 && s[0].length > digitsBeforeZero - 1 && decimalSeparator.toString() != source -> ""
                s.size > 1 && s[1].length > digitsAfterZero - 1 -> ""
                else -> source
            }
        }

        return source
    }
}