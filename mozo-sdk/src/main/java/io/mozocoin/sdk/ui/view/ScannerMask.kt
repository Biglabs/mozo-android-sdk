package io.mozocoin.sdk.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import io.mozocoin.sdk.utils.dp2Px

class ScannerMask : View {

    private val mCutPaint = Paint()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val maskColor = Color.parseColor("#80000000")
    private var squareHoleSize = 0f
    private var lineW = 0f
    private var lineH = 0f
    private var lineOffset = 0f

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mCutPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        paint.color = Color.WHITE
        squareHoleSize = resources.dp2Px(260f)
        lineW = resources.dp2Px(40f)
        lineH = resources.dp2Px(5f)
        lineOffset = lineH
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return

        canvas.drawColor(maskColor)
        val x = width /2 - squareHoleSize /2
        val y = height /2 - squareHoleSize * 0.7f
        canvas.drawRect(
            x,
            y,
            x + squareHoleSize,
            y + squareHoleSize,
            mCutPaint
        )

        var lineX = x - lineOffset - lineH
        var lineY = y - lineOffset - lineH
        canvas.drawRect(
            lineX,
            lineY,
            lineX + lineW,
            lineY + lineH,
            paint
        )
        canvas.drawRect(
            lineX,
            lineY,
            lineX + lineH,
            lineY + lineW,
            paint
        )

        lineX = x + squareHoleSize + lineOffset
        canvas.drawRect(
            lineX,
            lineY,
            lineX + lineH,
            lineY + lineW,
            paint
        )

        lineX -= lineW - lineH
        canvas.drawRect(
            lineX,
            lineY,
            lineX + lineW,
            lineY + lineH,
            paint
        )

        lineY += squareHoleSize + lineOffset * 2 + lineH
        canvas.drawRect(
            lineX,
            lineY,
            lineX + lineW,
            lineY + lineH,
            paint
        )

        lineX += lineW - lineH
        lineY -= lineW - lineH
        canvas.drawRect(
            lineX,
            lineY,
            lineX + lineH,
            lineY + lineW,
            paint
        )

        lineX -= squareHoleSize + lineOffset * 2 + lineH
        canvas.drawRect(
            lineX,
            lineY,
            lineX + lineH,
            lineY + lineW,
            paint
        )

        lineY += lineW - lineH
        canvas.drawRect(
            lineX,
            lineY,
            lineX + lineW,
            lineY + lineH,
            paint
        )
    }
}