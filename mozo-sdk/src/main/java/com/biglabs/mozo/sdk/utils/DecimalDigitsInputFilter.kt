package com.biglabs.mozo.sdk.utils

import android.text.InputFilter
import android.text.Spanned

class DecimalDigitsInputFilter(private val digitsBeforeZero: Int, private val digitsAfterZero: Int) : InputFilter {

    override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence? {
        dest?.let {
            val s = it.split(".")
            return when {
                s.size == 1 && s[0].length > digitsBeforeZero - 1 && "." != source -> ""
                s.size > 1 && s[1].length > digitsAfterZero - 1 -> ""
                else -> source
            }
        }

        return source
    }
}