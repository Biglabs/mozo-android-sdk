package io.mozocoin.sdk.ui.view

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import androidx.core.content.ContextCompat
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.color

class MozoEmptyView : MozoIconTextView {

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.ITALIC)
        setTextColor(context.color(R.color.mozo_color_line))

        var hasIcon = false
        compoundDrawablesRelative.map {
            hasIcon = it != null
        }

        if (!hasIcon) {
            setCompoundDrawablesRelative(null, ContextCompat.getDrawable(context, R.drawable.im_empty_box), null, null)
        }
    }
}