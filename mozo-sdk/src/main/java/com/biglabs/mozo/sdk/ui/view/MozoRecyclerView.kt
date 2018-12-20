package com.biglabs.mozo.sdk.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.contact.ContactSectionDecoration

internal class MozoRecyclerView : RecyclerView {

    private var letterBarWidth = 0
    private lateinit var sectionDecoration: ContactSectionDecoration

    private var lastTappedLetter = ""
    var onLetterScrollListener: ((letter: String) -> Unit)? = null

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {

        letterBarWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, resources.displayMetrics).toInt()
        val defaultTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics).toInt()
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MozoRecyclerView, 0, defStyle)
        try {
            if (typedArray.getBoolean(R.styleable.MozoRecyclerView_lettersBarEnable, true)) {

                letterBarWidth = typedArray.getDimensionPixelSize(R.styleable.MozoRecyclerView_lettersBarWidth, letterBarWidth)
                val lettersBarMarginTop = typedArray.getDimensionPixelSize(R.styleable.MozoRecyclerView_lettersBarMarginTop, 0)
                val lettersBarTextSize = typedArray.getDimensionPixelSize(R.styleable.MozoRecyclerView_lettersBarTextSize, defaultTextSize)
                val lettersBarTextColor = typedArray.getColor(R.styleable.MozoRecyclerView_lettersBarTextColor, Color.BLACK)
                val lettersBarTextLineHeight = typedArray.getDimensionPixelSize(R.styleable.MozoRecyclerView_lettersBarTextLineHeight, (lettersBarTextSize * 1.4).toInt())

                setPaddingRelative(paddingStart, paddingTop, paddingEnd + letterBarWidth, paddingBottom)

                sectionDecoration = ContactSectionDecoration(letterBarWidth, lettersBarMarginTop, lettersBarTextSize, lettersBarTextColor, lettersBarTextLineHeight)
                addItemDecoration(sectionDecoration)
            }
        } catch (e: Exception) {
            typedArray?.recycle()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        e?.run {
            if (x >= width - letterBarWidth) {
                val letter: String = sectionDecoration.getLetter(y)

                if (!lastTappedLetter.equals(letter, ignoreCase = true)) {
                    lastTappedLetter = letter
                    onLetterScrollListener?.invoke(letter)
                }

                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }
        }
        return super.onTouchEvent(e)
    }
}