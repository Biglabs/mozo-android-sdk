/**
 * Copyright 2016 Ali Muzaffar
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.biglabs.mozo.sdk.ui.widget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v4.text.TextUtilsCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.AppCompatEditText
import android.text.InputFilter
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import com.biglabs.mozo.sdk.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.util.*

internal class PinEntryEditText : AppCompatEditText {
    private var onBackPress: (suspend () -> Unit)? = null

    private var mMask: String? = null
    private var mMaskChars: StringBuilder? = null
    private var mSingleCharHint: String? = null
    private var mAnimatedType = 0
    private var mSpace = 10f //24 dp by default, space between the lines
    private var mCharSize = 0f
    private var mNumChars = 4f
    private var mTextBottomPadding = 9f //8dp by default, height of the text from our lines
    private var mMaxLength = 4
    private var mLineCoords: Array<RectF>? = null
    private lateinit var mCharBottom: FloatArray
    private lateinit var mCharPaint: Paint
    private lateinit var mLastCharPaint: Paint
    private lateinit var mSingleCharPaint: Paint
    private var mPinBackground: Drawable? = null
    private var mTextHeight = Rect()
    private var mIsDigitSquare = false

    private var mClickListener: View.OnClickListener? = null
    private var mOnPinEnteredListener: OnPinEnteredListener? = null

    private var mLineStroke = 1f //1dp by default
    private var mLineStrokeSelected = 2f //2dp by default
    private lateinit var mLinesPaint: Paint
    private var mAnimate = false
    var isError = false
    private var mOriginalTextColors: ColorStateList? = null
    private var mStates = arrayOf(intArrayOf(android.R.attr.state_selected), // selected
            intArrayOf(android.R.attr.state_active), // error
            intArrayOf(android.R.attr.state_focused), // focused
            intArrayOf(-android.R.attr.state_focused))// unfocused

    private var mColors = intArrayOf(Color.GREEN, Color.RED, Color.BLACK, Color.GRAY)

    private var mColorStates = ColorStateList(mStates, mColors)

    private val fullText: CharSequence
        get() = if (mMask == null) {
            text
        } else {
            maskChars
        }

    private val maskChars: StringBuilder
        get() {
            if (mMaskChars == null) {
                mMaskChars = StringBuilder()
            }
            val textLength = text.length
            while (mMaskChars!!.length != textLength) {
                if (mMaskChars!!.length < textLength) {
                    mMaskChars!!.append(mMask)
                } else {
                    mMaskChars!!.deleteCharAt(mMaskChars!!.length - 1)
                }
            }
            return mMaskChars!!
        }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    fun setMaxLength(maxLength: Int) {
        mMaxLength = maxLength
        mNumChars = maxLength.toFloat()

        filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))

        text = null
        invalidate()
    }

    fun setMask(mask: String?) {
        mMask = mask
        mMaskChars = null
        invalidate()
    }

    fun setSingleCharHint(hint: String) {
        mSingleCharHint = hint
        invalidate()
    }

    private fun init(context: Context, attrs: AttributeSet) {
        val multi = context.resources.displayMetrics.density
        mLineStroke *= multi
        mLineStrokeSelected *= multi
        mSpace *= multi //convert to pixels for our density
        mTextBottomPadding *= multi //convert to pixels for our density

        val ta = context.obtainStyledAttributes(attrs, R.styleable.PinEntryEditText, 0, 0)
        try {
            val outValue = TypedValue()
            ta.getValue(R.styleable.PinEntryEditText_pinAnimationType, outValue)
            mAnimatedType = outValue.data
            mMask = ta.getString(R.styleable.PinEntryEditText_pinCharacterMask)
            mSingleCharHint = ta.getString(R.styleable.PinEntryEditText_pinRepeatedHint)
            mLineStroke = ta.getDimension(R.styleable.PinEntryEditText_pinLineStroke, mLineStroke)
            mLineStrokeSelected = ta.getDimension(R.styleable.PinEntryEditText_pinLineStrokeSelected, mLineStrokeSelected)
            mSpace = ta.getDimension(R.styleable.PinEntryEditText_pinCharacterSpacing, mSpace)
            mTextBottomPadding = ta.getDimension(R.styleable.PinEntryEditText_pinTextBottomPadding, mTextBottomPadding)
            mIsDigitSquare = ta.getBoolean(R.styleable.PinEntryEditText_pinBackgroundIsSquare, mIsDigitSquare)
            mPinBackground = ta.getDrawable(R.styleable.PinEntryEditText_pinBackgroundDrawable)
            val colors = ta.getColorStateList(R.styleable.PinEntryEditText_pinLineColors)
            if (colors != null) {
                mColorStates = colors
            }
        } finally {
            ta.recycle()
        }

        mCharPaint = Paint(paint)
        mLastCharPaint = Paint(paint)
        mSingleCharPaint = Paint(paint)
        mLinesPaint = Paint(paint)
        mLinesPaint.strokeWidth = mLineStroke

        val outValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorControlActivated,
                outValue, true)
        val colorSelected = outValue.data
        mColors[0] = colorSelected

        val colorFocused = if (isInEditMode) Color.GRAY else ContextCompat.getColor(context, R.color.mozo_color_pin)
        mColors[1] = colorFocused

        val colorUnfocused = if (isInEditMode) Color.GRAY else ContextCompat.getColor(context, R.color.mozo_color_pin)
        mColors[2] = colorUnfocused

        setBackgroundResource(0)

        mMaxLength = attrs.getAttributeIntValue(XML_NAMESPACE_ANDROID, "maxLength", 4)
        mNumChars = mMaxLength.toFloat()

        //Disable copy paste
        super.setCustomSelectionActionModeCallback(object : ActionMode.Callback {
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {}

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return false
            }
        })
        // When tapped, move cursor to end of text.
        super.setOnClickListener { v ->
            setSelection(text.length)
            if (mClickListener != null) {
                mClickListener!!.onClick(v)
            }
        }

        super.setOnLongClickListener {
            setSelection(text.length)
            true
        }

        //If input type is password and no mask is set, use a default mask
        if (inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD == InputType.TYPE_TEXT_VARIATION_PASSWORD && TextUtils.isEmpty(mMask)) {
            mMask = DEFAULT_MASK
        } else if (inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD == InputType.TYPE_NUMBER_VARIATION_PASSWORD && TextUtils.isEmpty(mMask)) {
            mMask = DEFAULT_MASK
        }

        if (!TextUtils.isEmpty(mMask)) {
            mMaskChars = maskChars
        }

        //Height of the characters, used if there is a background drawable
        paint.getTextBounds("|", 0, 1, mTextHeight)

        mAnimate = mAnimatedType > -1

        isCursorVisible = false
        setTextIsSelectable(false)
    }

    override fun setInputType(type: Int) {
        super.setInputType(type)

        if (type and InputType.TYPE_TEXT_VARIATION_PASSWORD == InputType.TYPE_TEXT_VARIATION_PASSWORD || type and InputType.TYPE_NUMBER_VARIATION_PASSWORD == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
            // If input type is password and no mask is set, use a default mask
            if (TextUtils.isEmpty(mMask)) {
                setMask(DEFAULT_MASK)
            }
        } else {
            // If input type is not password, remove mask
            setMask(null)
        }

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mOriginalTextColors = textColors
        if (mOriginalTextColors != null) {
            mLastCharPaint.color = mOriginalTextColors!!.defaultColor
            mCharPaint.color = mOriginalTextColors!!.defaultColor
            mSingleCharPaint.color = currentHintTextColor
        }
        val availableWidth = width - ViewCompat.getPaddingEnd(this) - ViewCompat.getPaddingStart(this)
        if (mSpace < 0) {
            mCharSize = availableWidth / (mNumChars * 2 - 1)
        } else {
            mCharSize = (availableWidth - mSpace * (mNumChars - 1)) / mNumChars
        }
        mLineCoords = Array(mNumChars.toInt()) { RectF() }
        mCharBottom = FloatArray(mNumChars.toInt())
        var startX: Int
        val bottom = height - paddingBottom
        val rtlFlag: Int
        val isLayoutRtl = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL
        if (isLayoutRtl) {
            rtlFlag = -1
            startX = (width.toFloat() - ViewCompat.getPaddingStart(this).toFloat() - mCharSize).toInt()
        } else {
            rtlFlag = 1
            startX = ViewCompat.getPaddingStart(this)
        }
        var i = 0
        while (i < mNumChars) {
            mLineCoords!![i] = RectF(startX.toFloat(), bottom.toFloat(), startX + mCharSize, bottom.toFloat())
            if (mPinBackground != null) {
                if (mIsDigitSquare) {
                    mLineCoords!![i].top = paddingTop.toFloat()
                    mLineCoords!![i].right = startX + mLineCoords!![i].height()
                } else {
                    mLineCoords!![i].top -= mTextHeight.height() + mTextBottomPadding * 2
                }
            }

            if (mSpace < 0) {
                startX += (rtlFlag.toFloat() * mCharSize * 2f).toInt()
            } else {
                startX += (rtlFlag * (mCharSize + mSpace)).toInt()
            }
            mCharBottom[i] = mLineCoords!![i].bottom - mTextBottomPadding
            i++
        }
    }

    override fun setOnClickListener(l: View.OnClickListener?) {
        mClickListener = l
    }

    override fun setCustomSelectionActionModeCallback(actionModeCallback: ActionMode.Callback) {
        throw RuntimeException("setCustomSelectionActionModeCallback() not supported.")
    }

    override fun onDraw(canvas: Canvas) {
        //super.onDraw(canvas);
        val text = fullText
        val textLength = text.length
        val textWidths = FloatArray(textLength)
        paint.getTextWidths(text, 0, textLength, textWidths)

        var hintWidth = 0f
        if (mSingleCharHint != null) {
            val hintWidths = FloatArray(mSingleCharHint!!.length)
            paint.getTextWidths(mSingleCharHint, hintWidths)
            for (i in hintWidths) {
                hintWidth += i
            }
        }
        var i = 0
        while (i < mNumChars) {
            //If a background for the pin characters is specified, it should be behind the characters.
            if (mPinBackground != null) {
                updateDrawableState(i < textLength, i == textLength)
                mPinBackground!!.setBounds(mLineCoords!![i].left.toInt(), mLineCoords!![i].top.toInt(), mLineCoords!![i].right.toInt(), mLineCoords!![i].bottom.toInt())
                mPinBackground!!.draw(canvas)
            }
            val middle = mLineCoords!![i].left + mCharSize / 2
            if (textLength > i) {
                if (!mAnimate || i != textLength - 1) {
                    canvas.drawText(text, i, i + 1, middle - textWidths[i] / 2, mCharBottom[i], mCharPaint)
                } else {
                    canvas.drawText(text, i, i + 1, middle - textWidths[i] / 2, mCharBottom[i], mLastCharPaint)
                }
            } else if (mSingleCharHint != null) {
                canvas.drawText(mSingleCharHint!!, middle - hintWidth / 2, mCharBottom[i], mSingleCharPaint)
            }
            //The lines should be in front of the text (because that's how I want it).
            if (mPinBackground == null) {
                updateColorForLines(i <= textLength)
                canvas.drawLine(mLineCoords!![i].left, mLineCoords!![i].top, mLineCoords!![i].right, mLineCoords!![i].bottom, mLinesPaint)
            }
            i++
        }
    }


    private fun getColorForState(vararg states: Int): Int {
        return mColorStates.getColorForState(states, Color.GRAY)
    }

    /**
     * @param hasTextOrIsNext Is the color for a character that has been typed or is
     * the next character to be typed?
     */
    protected fun updateColorForLines(hasTextOrIsNext: Boolean) {
        if (isError) {
            mLinesPaint.color = getColorForState(android.R.attr.state_active)
        } else if (isFocused) {
            mLinesPaint.strokeWidth = mLineStrokeSelected
            mLinesPaint.color = getColorForState(android.R.attr.state_focused)
            if (hasTextOrIsNext) {
                mLinesPaint.color = getColorForState(android.R.attr.state_selected)
            }
        } else {
            mLinesPaint.strokeWidth = mLineStroke
            mLinesPaint.color = getColorForState(-android.R.attr.state_focused)
        }
    }

    protected fun updateDrawableState(hasText: Boolean, isNext: Boolean) {
        if (isError) {
            mPinBackground!!.state = intArrayOf(android.R.attr.state_active)
        } else if (isFocused) {
            mPinBackground!!.state = intArrayOf(android.R.attr.state_focused)
            if (isNext) {
                mPinBackground!!.state = intArrayOf(android.R.attr.state_focused, android.R.attr.state_selected)
            } else if (hasText) {
                mPinBackground!!.state = intArrayOf(android.R.attr.state_focused, android.R.attr.state_checked)
            }
        } else {
            mPinBackground!!.state = intArrayOf(-android.R.attr.state_focused)
        }
    }

    /**
     * Request focus on this PinEntryEditText
     */
    fun focus() {
        requestFocus()

        // Show keyboard
        val inputMethodManager = context
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(this, 0)
    }

    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        isError = false
        if (mLineCoords == null || !mAnimate) {
            if (mOnPinEnteredListener != null && text.length == mMaxLength) {
                mOnPinEnteredListener!!.onPinEntered(text)
            }
            return
        }

        if (mAnimatedType == -1) {
            invalidate()
            return
        }

        if (lengthAfter > lengthBefore) {
            if (mAnimatedType == 0) {
                animatePopIn()
            } else {
                animateBottomUp(text, start)
            }
        }
    }

    private fun animatePopIn() {
        val va = ValueAnimator.ofFloat(1f, paint.textSize)
        va.duration = 200
        va.interpolator = OvershootInterpolator()
        va.addUpdateListener { animation ->
            mLastCharPaint.textSize = animation.animatedValue as Float
            this@PinEntryEditText.invalidate()
        }
        if (text.length == mMaxLength && mOnPinEnteredListener != null) {
            va.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    mOnPinEnteredListener!!.onPinEntered(text)
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
        va.start()
    }

    private fun animateBottomUp(text: CharSequence, start: Int) {
        mCharBottom[start] = mLineCoords!![start].bottom - mTextBottomPadding
        val animUp = ValueAnimator.ofFloat(mCharBottom[start] + paint.textSize, mCharBottom[start])
        animUp.duration = 300
        animUp.interpolator = OvershootInterpolator()
        animUp.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            mCharBottom[start] = value
            this@PinEntryEditText.invalidate()
        }

        mLastCharPaint.alpha = 255
        val animAlpha = ValueAnimator.ofInt(0, 255)
        animAlpha.duration = 300
        animAlpha.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            mLastCharPaint.alpha = value
        }

        val set = AnimatorSet()
        if (text.length == mMaxLength && mOnPinEnteredListener != null) {
            set.addListener(object : Animator.AnimatorListener {

                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    mOnPinEnteredListener!!.onPinEntered(getText())
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {

                }
            })
        }
        set.playTogether(animUp, animAlpha)
        set.start()
    }

    fun setAnimateText(animate: Boolean) {
        mAnimate = animate
    }

    fun setOnPinEnteredListener(l: OnPinEnteredListener) {
        mOnPinEnteredListener = l
    }

    interface OnPinEnteredListener {
        fun onPinEntered(str: CharSequence)
    }

    fun setOnBackPress(action: suspend () -> Unit) {
        this.onBackPress = action
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPress?.let {
                launch(UI) {
                    it()
                }
            }
            return true
        }
        return false
    }

    companion object {
        private const val XML_NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android"

        const val DEFAULT_MASK = "\u25CF"
    }
}

internal fun PinEntryEditText.onBackPress(action: suspend () -> Unit) = setOnBackPress(action)