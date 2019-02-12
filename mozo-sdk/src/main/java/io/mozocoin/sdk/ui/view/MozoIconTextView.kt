package io.mozocoin.sdk.ui.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.mozocoin.sdk.R

open class MozoIconTextView : AppCompatTextView {

    private var drawableSize = 0
    private var drawableRect: Rect? = null

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.MozoIconTextView, defStyle, 0)
        try {
            drawableSize = typeArray.getDimensionPixelSize(R.styleable.MozoIconTextView_drawableSize, drawableSize)
            if (drawableSize > 0) {
                drawableRect = Rect(0, 0, drawableSize, drawableSize)
            }
        } finally {
            typeArray.recycle()
        }
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