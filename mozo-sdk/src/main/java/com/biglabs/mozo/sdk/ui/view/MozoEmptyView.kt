package com.biglabs.mozo.sdk.ui.view

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.TextView
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.utils.color

class MozoEmptyView : TextView {

    private var drawableSize = 0
    private var drawableRect: Rect? = null

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.MozoEmptyView, defStyle, 0)
        try {
            drawableSize = typeArray.getDimensionPixelSize(R.styleable.MozoEmptyView_drawableSize, drawableSize)
            if (drawableSize > 0) {
                drawableRect = Rect(0, 0, drawableSize, drawableSize)
            }
        } finally {
            typeArray.recycle()
        }

        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.ITALIC)
        setTextColor(context.color(R.color.mozo_color_line))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        drawableRect?.let { rect ->
            val drawables = compoundDrawablesRelative.map {
                it?.apply { bounds = rect }
            }
            setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3])
        }
    }
}