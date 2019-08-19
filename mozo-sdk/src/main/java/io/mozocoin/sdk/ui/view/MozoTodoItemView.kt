package io.mozocoin.sdk.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import io.mozocoin.sdk.R

class MozoTodoItemView : LinearLayout {

    private val paintBorder = Paint()
    private val paintFill = Paint()
    private var cornerRadius: Int = 0
    private var borderWidth: Int = 0
    private var startEdgeWidth: Int = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.MozoTodoItemView)
        try {
            borderWidth = typeArray.getDimensionPixelSize(R.styleable.MozoTodoItemView_todo_borderWidth, borderWidth)
            cornerRadius = typeArray.getDimensionPixelSize(R.styleable.MozoTodoItemView_todo_cornerRadius, cornerRadius)
            startEdgeWidth = typeArray.getDimensionPixelSize(R.styleable.MozoTodoItemView_todo_startEdgeWidth, startEdgeWidth)
        } finally {
            typeArray.recycle()
        }

        paintBorder.style = Paint.Style.FILL
        paintBorder.color = Color.DKGRAY
        paintFill.style = Paint.Style.FILL
        paintFill.color = Color.WHITE
    }

    override fun dispatchDraw(canvas: Canvas?) {
        val r = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas?.drawRoundRect(r, cornerRadius.toFloat(), cornerRadius.toFloat(), paintBorder)

        val rectFill = RectF(
                startEdgeWidth.toFloat() + borderWidth.toFloat(),
                borderWidth.toFloat(),
                width.toFloat() - borderWidth,
                height.toFloat() - borderWidth
        )
        canvas?.drawRoundRect(rectFill, cornerRadius.toFloat(), cornerRadius.toFloat(), paintFill)
        super.dispatchDraw(canvas)
    }

    fun setBorderColor(@ColorInt color: Int) {
        paintBorder.color = color
        invalidate()
    }
}