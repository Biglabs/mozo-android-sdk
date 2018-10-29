package com.biglabs.mozo.sdk.contact

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.support.v7.widget.RecyclerView

internal class ContactSectionDecoration(
        private var letterBarWidth: Int,
        private var letterBarMarginTop: Int,
        private var lettersBarTextSize: Int,
        private var lettersBarTextColor: Int,
        private var lettersBarTextLineHeight: Int
) : RecyclerView.ItemDecoration() {

    private val alphabets = charArrayOf('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '#')

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(canvas, parent, state)

            val paint = Paint()
            paint.color = lettersBarTextColor
            paint.style = Paint.Style.FILL
            paint.textSize = lettersBarTextSize.toFloat()
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            val x = canvas.width.toFloat() - letterBarWidth / 2
            var startY = (letterBarMarginTop + lettersBarTextSize).toFloat()

            alphabets.map {
                canvas.drawText(it.toString(), x, startY, paint)
                startY += lettersBarTextLineHeight
            }
    }

    fun getLetter(y: Float): String {
        val yPosition = (y - letterBarMarginTop).toInt()
        val index = Math.min(Math.max(yPosition / lettersBarTextLineHeight, 0), alphabets.size - 1)
        return alphabets[index].toString()
    }
}