package io.mozocoin.sdk.ui.view

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import androidx.core.content.ContextCompat
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.color
import io.mozocoin.sdk.utils.dimen
import io.mozocoin.sdk.utils.dp2Px

class MozoEmptyView : MozoIconTextView {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.ITALIC)
        setTextColor(context.color(R.color.mozo_color_line))

        var hasIcon = false
        compoundDrawablesRelative.map {
            hasIcon = it != null
        }

        if (!hasIcon) {
            setCompoundDrawablesRelative(null, ContextCompat.getDrawable(context, R.drawable.im_empty_box), null, null)

            compoundDrawablePadding = resources.dp2Px(20f).toInt()
            drawableSize = context.dimen(R.dimen.mozo_view_empty_height)
            drawableRect = Rect(0, 0, drawableSize, drawableSize)
        }
    }
}