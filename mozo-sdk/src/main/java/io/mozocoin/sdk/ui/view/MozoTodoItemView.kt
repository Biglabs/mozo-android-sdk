package io.mozocoin.sdk.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import io.mozocoin.sdk.utils.dp2Px

class MozoTodoItemView : LinearLayout {

    private val paintBorder = Paint()
    private val paintFill = Paint()
    private val cornerRadius: Float
    private val borderWidth: Float

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        paintBorder.style = Paint.Style.FILL
        paintBorder.color = Color.DKGRAY
        paintFill.style = Paint.Style.FILL
        paintFill.color = Color.WHITE

        cornerRadius = context.resources.dp2Px(8f)
        borderWidth = context.resources.dp2Px(0.5f)
    }

    override fun dispatchDraw(canvas: Canvas?) {
        val r = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas?.drawRoundRect(r, cornerRadius, cornerRadius, paintBorder)

        val rectFill = RectF(borderWidth * 8, borderWidth, width.toFloat() - borderWidth, height.toFloat() - borderWidth)
        canvas?.drawRoundRect(rectFill, cornerRadius, cornerRadius, paintFill)
        super.dispatchDraw(canvas)
    }

    fun setBorderColor(@ColorInt color: Int) {
        paintBorder.color = color
        invalidate()
    }
}